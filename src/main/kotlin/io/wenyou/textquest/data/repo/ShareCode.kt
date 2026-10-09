package io.wenyou.textquest.data.repo

import io.wenyou.textquest.BuildConfig
import io.wenyou.textquest.data.model.AppBundle
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater

/** 分享码：把一部剧情/角色编码成可复制、可扫码的文本。
 *
 *  负载先 deflate 压缩再 base64，显著缩小体积；编码用 [shareJson]（关闭 encodeDefaults）
 *  以去掉默认字段，进一步压小，从而能放进单张二维码。前缀 WY2: 表示压缩负载，兼容旧 WY1:。 */
object ShareCode {
    private const val PREFIX_COMPRESSED = "WY2:"
    private const val PREFIX_LEGACY = "WY1:"
    private const val MAX_BYTES = 8 * 1024 * 1024

    // 分享专用：忽略未知键、不写默认值，让分享码尽可能小
    private val shareJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    fun encode(bundle: AppBundle): String {
        val json = shareJson.encodeToString(AppBundle.serializer(), bundle.copy(origin = BuildConfig.SHARE_ORIGIN))
        val bytes = json.toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_BYTES) return ""
        val compressed = deflate(bytes)
        val b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)
        return PREFIX_COMPRESSED + b64
    }

    fun decode(code: String): AppBundle? = decodeJson(code)?.let {
        runCatching { shareJson.decodeFromString(AppBundle.serializer(), it) }.getOrNull()
    }

    /**
     * Content written by the hx app may only be imported there; every other origin imports anywhere.
     * Older content has no origin, so fields only the hx app writes still identify it.
     */
    fun foreign(code: String): Boolean = decodeJson(code)?.let(::foreignJson) ?: false

    fun foreignJson(json: String): Boolean {
        if (BuildConfig.SHARE_ORIGIN == "hx") return false
        val root = runCatching { shareJson.parseToJsonElement(json).jsonObject }.getOrNull() ?: return false
        if (root["origin"]?.jsonPrimitive?.contentOrNull == "hx") return true
        val items = listOf("stories", "characters").flatMap { key -> runCatching { root[key]?.jsonArray }.getOrNull().orEmpty() }
        return items.any { it is JsonObject && (it["lgbt"]?.jsonPrimitive?.booleanOrNull == true || "orientation" in it) }
    }

    private fun decodeJson(code: String): String? {
        if (code.length > MAX_BYTES * 2) return null
        val cleaned = code.trim()
        val pre = when {
            cleaned.startsWith(PREFIX_COMPRESSED) -> PREFIX_COMPRESSED
            cleaned.startsWith(PREFIX_LEGACY) -> PREFIX_LEGACY
            else -> null
        }
        val payload = cleaned.removePrefix(PREFIX_COMPRESSED).removePrefix(PREFIX_LEGACY)
            .filterNot { it.isWhitespace() }
        if (payload.isBlank()) return null
        return try {
            val bytes = Base64.getUrlDecoder().decode(payload)
            val jsonBytes = if (pre == PREFIX_COMPRESSED) inflate(bytes) else bytes
            if (jsonBytes.size > MAX_BYTES) return null
            String(jsonBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun deflate(bytes: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(bytes)
        deflater.finish()
        val out = ByteArrayOutputStream(bytes.size / 2)
        val buf = ByteArray(4096)
        while (!deflater.finished()) {
            out.write(buf, 0, deflater.deflate(buf))
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun inflate(bytes: ByteArray): ByteArray {
        val inflater = Inflater(true)
        try {
            inflater.setInput(bytes)
            val out = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                require(n > 0 || inflater.finished()) { "分享码压缩数据不完整" }
                require(out.size() + n <= MAX_BYTES) { "分享内容超过 8 MiB" }
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        } finally {
            inflater.end()
        }
    }

    // ---- 二维码分片（QR Book）：单张放不下时才用 ----

    const val QR_CHUNK_PREFIX = "wyq:"
    private const val CHUNK_CHARS = 420

    data class QrChunk(val index: Int, val total: Int, val data: String, val bookId: String? = null)

    /** 把整段分享码拆成若干分片；bookId 防止同页数的两套二维码被误拼。 */
    fun qrChunks(code: String): List<String> {
        if (code.isBlank()) return emptyList()
        val n = (code.length + CHUNK_CHARS - 1) / CHUNK_CHARS
        // ponytail: CRC32 prevents accidental book mixing; use a longer digest only if adversarial collisions matter.
        val bookId = CRC32().apply { update(code.toByteArray(Charsets.UTF_8)) }.value.toString(16).padStart(8, '0')
        return (0 until n).map { i ->
            val start = i * CHUNK_CHARS
            val end = minOf(start + CHUNK_CHARS, code.length)
            "$QR_CHUNK_PREFIX$bookId:${i + 1}/$n|" + code.substring(start, end)
        }
    }

    /** 解析一个分片（新版 WQ: 页或旧版 wyq: 分片）；不是分片返回 null。 */
    fun parseChunk(text: String): QrChunk? {
        // Base45 data may legitimately end with a space, so new pages are never trimmed at the end.
        if (text.trimStart().startsWith(PAGE_PREFIX)) return parsePage(text.trimStart())
        val cleaned = text.trim()
        if (!cleaned.startsWith(QR_CHUNK_PREFIX)) return null
        val body = cleaned.removePrefix(QR_CHUNK_PREFIX)
        val sep = body.indexOf('|')
        if (sep < 0) return null
        val header = body.substring(0, sep)
        val colon = header.indexOf(':')
        val bookId = if (colon >= 0) header.substring(0, colon).takeIf { it.matches(Regex("[0-9a-f]{8}")) }
            ?: return null else null
        val parts = header.substring(colon + 1).split('/')
        if (parts.size != 2) return null
        val idx = parts[0].toIntOrNull() ?: return null
        val total = parts[1].toIntOrNull() ?: return null
        val data = body.substring(sep + 1)
        if (idx < 1 || total < 1 || idx > total || data.isBlank()) return null
        return QrChunk(idx, total, data, bookId)
    }

    /** 按序号拼接所有分片，得到完整分享码；未收齐返回 null。 */
    fun assembleChunks(chunks: Map<Int, String>, total: Int): String? {
        if (chunks.size < total) return null
        val sb = StringBuilder()
        for (i in 1..total) {
            val d = chunks[i] ?: return null
            sb.append(d)
        }
        return sb.toString()
    }

    /** 从相册识别出的若干二维码文本中恢复一份完整分享码。 */
    fun assembleQrTexts(texts: Iterable<String>): String? {
        val cleaned = texts.map(String::trimStart).filter(String::isNotBlank)
        val complete = cleaned.filterTo(mutableSetOf()) { decode(it) != null }

        val books = cleaned.mapNotNull(::parseChunk).groupBy { it.bookId to it.total }
        for ((key, chunks) in books) {
            val byIndex = mutableMapOf<Int, String>()
            if (chunks.any { byIndex.put(it.index, it.data)?.let { old -> old != it.data } == true }) continue
            val assembled = assembleChunks(byIndex, key.second) ?: continue
            if (decode(assembled) != null) complete += assembled
        }
        return complete.singleOrNull()
    }

    // ---- 新版二维码分页：Base45 走二维码的字母数字模式，同样密度比 base64 文本多装约一半 ----

    private const val PAGE_PREFIX = "WQ:"
    /** 每页的压缩字节数：取 6 的倍数，使每页的 Base45 与 base64 片段都能独立换算、直接拼接。 */
    private const val PAGE_BYTES = 426

    /** 把分享码拆成新版二维码页；内容只含二维码字母数字字符，单页也带「1/1」页头。 */
    fun qrPages(code: String): List<String> {
        val bytes = code.trim().takeIf { it.startsWith(PREFIX_COMPRESSED) }
            ?.let { runCatching { Base64.getUrlDecoder().decode(it.removePrefix(PREFIX_COMPRESSED)) }.getOrNull() }
            ?: return emptyList()
        val bookId = CRC32().apply { update(bytes) }.value.toString(16).padStart(8, '0').uppercase()
        val n = (bytes.size + PAGE_BYTES - 1) / PAGE_BYTES
        return (0 until n).map { i ->
            val slice = bytes.copyOfRange(i * PAGE_BYTES, minOf((i + 1) * PAGE_BYTES, bytes.size))
            "$PAGE_PREFIX$bookId:${i + 1}/$n:" + Base45.encode(slice)
        }
    }

    private fun parsePage(text: String): QrChunk? {
        val parts = text.removePrefix(PAGE_PREFIX).split(':', limit = 3)
        if (parts.size != 3 || !parts[0].matches(Regex("[0-9A-F]{8}"))) return null
        val numbers = parts[1].split('/').mapNotNull(String::toIntOrNull)
        if (numbers.size != 2) return null
        val (idx, total) = numbers
        if (idx < 1 || total < 1 || idx > total) return null
        val bytes = Base45.decode(parts[2])?.takeIf { it.isNotEmpty() } ?: return null
        // Every page but the last holds a multiple of 3 bytes, so the base64 pieces join into one WY2 payload.
        val piece = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return QrChunk(idx, total, if (idx == 1) PREFIX_COMPRESSED + piece else piece, parts[0].lowercase())
    }

    private val CODE_PATTERN = Regex("WY[12]:[A-Za-z0-9_-]{8,}")

    /** 从任意文字（分享消息、链接、文件内容）中找出第一段有效分享码。 */
    fun extract(text: String): String? =
        CODE_PATTERN.findAll(text.take(MAX_BYTES * 2)).map { it.value }.firstOrNull { decode(it) != null }
}

/** RFC 9285 Base45：只用二维码字母数字模式的 45 个字符。 */
internal object Base45 {
    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"

    fun encode(bytes: ByteArray): String = buildString {
        var i = 0
        while (i + 1 < bytes.size) {
            val n = (bytes[i].toInt() and 0xFF) * 256 + (bytes[i + 1].toInt() and 0xFF)
            append(ALPHABET[n % 45]).append(ALPHABET[n / 45 % 45]).append(ALPHABET[n / 2025])
            i += 2
        }
        if (i < bytes.size) {
            val n = bytes[i].toInt() and 0xFF
            append(ALPHABET[n % 45]).append(ALPHABET[n / 45])
        }
    }

    fun decode(text: String): ByteArray? {
        if (text.length % 3 == 1) return null
        val values = IntArray(text.length)
        for (i in text.indices) values[i] = ALPHABET.indexOf(text[i]).takeIf { it >= 0 } ?: return null
        val out = ByteArrayOutputStream(text.length * 2 / 3)
        var i = 0
        while (i + 2 < values.size) {
            val n = values[i] + values[i + 1] * 45 + values[i + 2] * 2025
            if (n > 0xFFFF) return null
            out.write(n shr 8); out.write(n and 0xFF)
            i += 3
        }
        if (i < values.size) {
            val n = values[i] + values[i + 1] * 45
            if (n > 0xFF) return null
            out.write(n)
        }
        return out.toByteArray()
    }
}
