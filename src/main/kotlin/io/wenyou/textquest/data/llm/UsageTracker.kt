package io.wenyou.textquest.data.llm

import io.wenyou.textquest.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Serializable
data class TokenUsage(val input: Long? = null, val output: Long? = null, val cached: Long = 0, val cacheWrite: Long = 0, val uncached: Long? = null, val reasoning: Long? = null) {
    fun cost(profile: ApiProfile): Double? {
        val i = input ?: return null; val o = output ?: return null
        val ip = profile.inputPrice ?: return null; val op = profile.outputPrice ?: return null
        val cp = if (cached > 0) profile.cachedPrice ?: return null else 0.0
        val wp = if (cacheWrite > 0) profile.cacheWritePrice ?: return null else 0.0
        if (listOf(ip, op, cp, wp).any { !it.isFinite() || it < 0 }) return null
        if (i < 0 || o < 0 || cached < 0 || cacheWrite < 0 || cached > i || cacheWrite > i - cached) return null
        if (uncached != null && uncached != i - cached - cacheWrite) return null
        if (reasoning != null && reasoning !in 0..o) return null
        return (((i - cached - cacheWrite).coerceAtLeast(0) * ip + o * op + cached * cp + cacheWrite * wp) / 1_000_000.0).takeIf { it.isFinite() }
    }
}

internal fun TokenUsage.read(kind: ProviderKind, element: JsonElement): TokenUsage {
    val root = element as? JsonObject ?: return this
    val u = (root["usage"] as? JsonObject) ?: (root["message"] as? JsonObject)?.get("usage") as? JsonObject
    val g = root["usageMetadata"] as? JsonObject
    fun JsonObject.n(key: String): Long? = (get(key) as? JsonPrimitive)?.longOrNull?.takeIf { it in 0..1_000_000_000L }
    return when (kind) {
        ProviderKind.OPENAI_COMPAT -> if (u == null) this else copy(input = u.n("prompt_tokens") ?: if (u.n("prompt_cache_hit_tokens") != null && u.n("prompt_cache_miss_tokens") != null) u.n("prompt_cache_hit_tokens")!! + u.n("prompt_cache_miss_tokens")!! else input,
            output = u.n("completion_tokens") ?: output,
            uncached = u.n("prompt_cache_miss_tokens") ?: uncached,
            reasoning = (u["completion_tokens_details"] as? JsonObject)?.n("reasoning_tokens") ?: reasoning, cached = u.n("prompt_cache_hit_tokens") ?: (u["prompt_tokens_details"] as? JsonObject)?.n("cached_tokens") ?: cached)
        ProviderKind.ANTHROPIC -> if (u == null) this else {
            val c = u.n("cache_read_input_tokens") ?: cached; val w = u.n("cache_creation_input_tokens") ?: cacheWrite
            copy(input = u.n("input_tokens")?.let { it + c + w } ?: input, output = u.n("output_tokens") ?: output, cached = c, cacheWrite = w)
        }
        ProviderKind.GEMINI -> if (g == null) this else copy(input = g.n("promptTokenCount") ?: input,
            output = g.n("candidatesTokenCount")?.let { it + (g.n("thoughtsTokenCount") ?: 0) } ?: output, cached = g.n("cachedContentTokenCount") ?: cached)
    }
}

data class GenerationProgress(val id: String, val service: String, val model: String, val startNanos: Long, val phase: String = "等待服务响应", val characters: Int = 0)
@Serializable
data class UsageRecord(val service: String, val model: String, val time: Long, val elapsedMs: Long, val status: String,
    val tokens: TokenUsage, val estimatedCost: Double? = null, val currency: String = "CNY", val requestId: String = "", val estimatedCostUpper: Double? = null, val pricingNote: String = "")

/** ponytail: retain the latest 100 requests; add archived totals only when lifetime accounting is needed. */
class UsageTracker(private val file: File? = null, private val nanoTime: () -> Long = System::nanoTime) {
    private val recordsState = MutableStateFlow(runCatching { file?.takeIf { it.exists() }?.let {
        AppJson.decodeFromString(ListSerializer(UsageRecord.serializer()), it.readText()).takeLast(100)
    } }.getOrNull().orEmpty())
    val records = recordsState.asStateFlow()
    private val activeState = MutableStateFlow<Map<String, GenerationProgress>>(emptyMap())
    val active = activeState.asStateFlow()
    private val persistenceState = MutableStateFlow("")
    val persistenceError = persistenceState.asStateFlow()
    private val lastProgress = mutableMapOf<String, Long>()
    @Synchronized fun start(p: GenerationProgress) { activeState.value = activeState.value + (p.id to p) }
    @Synchronized fun progress(id: String, phase: String, characters: Int) {
        val p = activeState.value[id] ?: return
        val now = nanoTime()
        if (phase == p.phase && now - (lastProgress[id] ?: Long.MIN_VALUE / 2) < 100_000_000L) return
        lastProgress[id] = now
        activeState.value = activeState.value + (id to p.copy(phase = phase, characters = characters))
    }
    @Synchronized fun finish(id: String, record: UsageRecord) {
        lastProgress.remove(id)
        recordsState.value = (recordsState.value + record.copy(requestId = id)).takeLast(100)
        activeState.value = activeState.value - id
        if (file != null) try {
            file.parentFile?.mkdirs()
            val temp = File(file.path + ".tmp")
            temp.writeText(AppJson.encodeToString(ListSerializer(UsageRecord.serializer()), recordsState.value))
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            persistenceState.value = ""
        } catch (_: Exception) { persistenceState.value = "用量记录未保存，关闭应用后可能丢失" }
    }
}
