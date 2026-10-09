package io.wenyou.textquest.data.llm

import io.wenyou.textquest.data.model.ApiProfile
import io.wenyou.textquest.data.model.AppJson
import io.wenyou.textquest.data.model.ProviderKind
import io.wenyou.textquest.data.repo.Baseline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatOptions(val temperature: Double = 0.85, val maxTokens: Int = 1024, val thinking: Boolean? = null)

/** 调用失败（网络 / HTTP / 解析）时向用户展示的可读错误。 */
class LlmException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Names the likely cause and the next step; the provider's own message is kept short for diagnosis. */
internal fun httpFailure(code: Int, body: String): LlmException {
    val detail = runCatching {
        when (val error = AppJson.parseToJsonElement(body).jsonObject["error"]) {
            is JsonObject -> error["message"]?.jsonPrimitive?.contentOrNull
            is JsonPrimitive -> error.contentOrNull
            else -> null
        }
    }.getOrNull() ?: body.trim().takeUnless { it.startsWith("<") }.orEmpty()
    val reason = when (code) {
        400 -> "请求没有被接受：模型名或参数可能不正确，请核对模型名"
        401, 403 -> "API Key 无效或没有权限：请检查 Key 是否填写正确、是否已过期"
        402 -> "账户余额不足：请到服务商平台充值后重试"
        404 -> "找不到接口或模型：请核对接口地址和模型名"
        408, 504 -> "服务响应超时：请稍后重试"
        413 -> "内容过长：请缩短描述后重试"
        429 -> "请求太频繁或额度已用完：请稍等片刻再试，或检查账户额度"
        in 500..599 -> "服务商暂时不可用：通常是服务繁忙，请稍后重试"
        else -> "服务返回了错误"
    }
    return LlmException("$reason（HTTP $code）" + if (detail.isBlank()) "" else "\n服务商信息：${detail.take(120)}")
}

internal fun networkFailure(e: IOException): LlmException = LlmException(when (e) {
    is java.net.UnknownHostException -> "无法连接到服务器：请检查网络，或核对接口地址是否正确"
    is java.net.SocketTimeoutException -> "连接超时：网络较慢或服务繁忙，请稍后重试"
    is javax.net.ssl.SSLException -> "安全连接失败：请检查接口地址或网络代理设置"
    is java.net.ConnectException -> "连接被拒绝：请核对接口地址和端口；本地服务请确认已经启动"
    else -> "网络错误：请检查网络后重试"
}, e)

/** 一次流式/非流式调用的结果：正文 + 思考过程。 */
data class ChatResult(val content: String, val reasoning: String)

/** 一个响应帧可以同时包含正文和思考。 */
private data class Delta(val content: String = "", val reasoning: String = "")

/** 多品牌流式聊天客户端。OpenAI 兼容、Anthropic、Gemini 三种协议收敛到 [streamText]。 */
class ChatClient(ok: OkHttpClient = defaultClient(), val usage: UsageTracker = UsageTracker()) {
    /**
     * The player's baseline (底层基调). Every request is built in [streamText], which always wraps it around the
     * prompt, so no feature can send AI text without it.
     */
    @Volatile var baseline: () -> String = { Baseline.DEFAULT }

    private val client = ok

    suspend fun streamText(
        profile: ApiProfile,
        system: String,
        user: String,
        options: ChatOptions = ChatOptions(profile.temperature, profile.maxTokens),
        onDelta: (String) -> Unit = {},
        onReasoning: (String) -> Unit = {}
    ): ChatResult = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val started = System.nanoTime()
        val id = java.util.UUID.randomUUID().toString()
        usage.start(GenerationProgress(id, profile.name, profile.model, started))
        var tokens = TokenUsage()
        var status = "失败"
        val full = StringBuilder()
        val reasoningFull = StringBuilder()
        var call: Call? = null
        // A whole reply may stream for minutes (long branching scripts); a stalled connection is caught by the client's
        // read timeout, so this cap only bounds the total. Reasoning models think longer still.
        val timeoutMs = if (profile.model.contains("reasoner", ignoreCase = true)) 360_000L else 300_000L
        try {
            val (guardedSystem, guardedUser) = Baseline.guard(baseline(), system, user)
            call = buildCall(profile, guardedSystem, guardedUser, options)
            val requestCall = call
            val result = withTimeout(timeoutMs) {
                suspendCancellableCoroutine<ChatResult> { cont ->
                    cont.invokeOnCancellation { requestCall.cancel() }
                    requestCall.enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            if (cont.isCancelled) return
                            cont.resumeWith(Result.failure(networkFailure(e)))
                        }

                        override fun onResponse(call: Call, response: Response) {
                            try {
                                if (!response.isSuccessful) {
                                    val body = response.body?.string()?.take(2000) ?: ""
                                    cont.resumeWith(Result.failure(httpFailure(response.code, body)))
                                    return
                                }
                                val src = response.body?.source() ?: run {
                                    cont.resumeWith(Result.failure(LlmException("空响应")))
                                    return
                                }
                                fun emit(d: Delta?) {
                                    if (d == null || !cont.isActive) return
                                    if (d.reasoning.isNotEmpty()) { reasoningFull.append(d.reasoning); usage.progress(id, "正在思考", full.length + reasoningFull.length); onReasoning(d.reasoning) }
                                    if (d.content.isNotEmpty()) { full.append(d.content); usage.progress(id, "正在生成内容", full.length + reasoningFull.length); onDelta(d.content) }
                                }
                                var sawData = false
                                val raw = StringBuilder()
                                while (true) {
                                    val line = src.readUtf8Line() ?: break
                                    if (line.isBlank()) continue
                                    if (line.startsWith("data:")) {
                                        sawData = true
                                        val payload = line.removePrefix("data:").trim()
                                        if (payload == "[DONE]") break
                                        if (payload.isEmpty()) continue
                                        emit(try { val element = AppJson.parseToJsonElement(payload); tokens = tokens.read(profile.kind, element); extractDelta(profile.kind, element) } catch (_: Throwable) { null })
                                    } else if (!sawData) {
                                        raw.append(line).append('\n')
                                    }
                                }
                                if (!sawData && raw.isNotBlank()) {
                                    emit(try { val element = AppJson.parseToJsonElement(raw.toString()); tokens = tokens.read(profile.kind, element); extractWhole(profile.kind, element) } catch (_: Throwable) { null })
                                }
                                // 流已结束（[DONE] 或响应流结束）但正文仍为空：视为失败，避免用户看到无提示的空白
                                if (full.isBlank() && reasoningFull.isBlank()) {
                                    if (cont.isActive) cont.resumeWith(Result.failure(LlmException("AI 未返回任何文本（可能被内容安全拦截或模型静默），请重试或换条提示。")))
                                } else if (cont.isActive) cont.resumeWith(Result.success(ChatResult(full.toString(), reasoningFull.toString())))
                            } catch (e: CancellationException) {
                                cont.resumeWith(Result.failure(e))
                            } catch (t: Throwable) {
                                if (cont.isActive) cont.resumeWith(Result.failure(LlmException(if (t is IOException) "网络连接中断，内容没有接收完整，请重试" else "读取 AI 回复失败，请重试", t)))
                            } finally {
                                try { response.close() } catch (_: Throwable) {}
                            }
                        }
                    })
                }
            }
            status = "完成"
            result
        } catch (e: TimeoutCancellationException) {
            val secs = timeoutMs / 1000
            throw LlmException("AI 响应超时（${secs} 秒未返回内容）。请检查模型配置、Key 与网络，或切换模型重试。")
        } catch (e: CancellationException) {
            status = "已取消"
            throw e
        } finally {
            call?.cancel()
            val finishedAt = System.currentTimeMillis()
            val estimate = tokens.estimate(profile, startedAt, finishedAt)
            usage.finish(id, UsageRecord(profile.name, profile.model, finishedAt,
                (System.nanoTime() - started) / 1_000_000, status, tokens, estimate.lower, profile.priceCurrency, estimatedCostUpper = estimate.upper, pricingNote = estimate.note))
        }
    }
    // ---------------- 读取可用模型列表 ----------------

    /**
     * 拉取该服务支持的模型 id 列表：
     * OpenAI 兼容 → GET {base}/models（data[].id）；Anthropic → GET /v1/models；
     * Gemini → GET /models?pageSize=1000（models[].name 去掉 models/ 前缀）。
     */
    suspend fun listModels(profile: ApiProfile): List<String> = withContext(Dispatchers.IO) {
        val call = listModelsCall(profile)
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(networkFailure(e)))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching { response.use { parseModels(profile.kind, it) } }
                    if (continuation.isActive) continuation.resumeWith(result)
                }
            })
        }
    }

    private fun parseModels(kind: ProviderKind, response: Response): List<String> {
        if (!response.isSuccessful) {
            throw httpFailure(response.code, response.body?.string()?.take(2000).orEmpty())
        }
        val text = response.body?.string().orEmpty()
        val element = try {
            if (text.isBlank()) null else AppJson.parseToJsonElement(text)
        } catch (_: Exception) {
            null
        } ?: return emptyList()
        return when (kind) {
            ProviderKind.OPENAI_COMPAT, ProviderKind.ANTHROPIC -> {
                element.jsonObject["data"]?.jsonArray?.mapNotNull { item ->
                    (item.jsonObject["id"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                } ?: emptyList()
            }
            ProviderKind.GEMINI -> {
                element.jsonObject["models"]?.jsonArray?.mapNotNull { item ->
                    (item.jsonObject["name"] as? JsonPrimitive)?.content
                        ?.removePrefix("models/")?.takeIf { it.isNotBlank() }
                } ?: emptyList()
            }
        }
    }

    private fun listModelsCall(profile: ApiProfile): Call {
        if (profile.baseUrl.isBlank()) throw LlmException("先填好接口地址（baseUrl）")
        val base = profile.baseUrl.trim().trimEnd('/')
        val builder = Request.Builder()
        when (profile.kind) {
            ProviderKind.OPENAI_COMPAT -> {
                val path = when {
                    base.endsWith("/chat/completions") -> base.removeSuffix("/chat/completions")
                    else -> base
                }
                val url = if (path.endsWith("/models")) path else "$path/models"
                builder.url(url)
                if (profile.apiKey.isNotBlank()) builder.header("Authorization", "Bearer ${profile.apiKey}")
            }
            ProviderKind.ANTHROPIC -> {
                val url = if (base.endsWith("/v1")) "$base/models" else "$base/v1/models"
                builder.url(url)
                builder.header("anthropic-version", "2023-06-01")
                if (profile.apiKey.isNotBlank()) builder.header("x-api-key", profile.apiKey)
            }
            ProviderKind.GEMINI -> {
                builder.url("$base/models?pageSize=1000")
                if (profile.apiKey.isNotBlank()) builder.header("x-goog-api-key", profile.apiKey)
            }
        }
        return client.newCall(builder.build())
    }

    // ---------------- 请求构建 ----------------

    private fun buildCall(profile: ApiProfile, system: String, user: String, options: ChatOptions): Call {
        if (profile.model.isBlank()) throw LlmException("尚未填写模型名（model）")
        val base = profile.baseUrl.trim().trimEnd('/')
        if (base.isEmpty()) throw LlmException("尚未填写接口地址（baseUrl）")
        return when (profile.kind) {
            ProviderKind.OPENAI_COMPAT -> openAiCall(profile, base, system, user, options)
            ProviderKind.ANTHROPIC -> anthropicCall(profile, base, system, user, options)
            ProviderKind.GEMINI -> geminiCall(profile, base, system, user, options)
        }
    }

    private fun openAiCall(profile: ApiProfile, base: String, system: String, user: String, options: ChatOptions): Call {
        val url = (if (base.endsWith("/chat/completions")) base else "$base/chat/completions")
        // deepseek-reasoner 为推理模型：temperature 固定不可调（传了通常被忽略），
        // 思考需要更充裕的 max_tokens，且更慢；这里归一化处理以贴合 DeepSeek 行为。
        val isReasoner = profile.model.contains("reasoner", ignoreCase = true)
        val body = buildJsonObject {
            put("model", profile.model)
            put("stream", true)
            options.thinking?.let { enabled -> putJsonObject("thinking") { put("type", if (enabled) "enabled" else "disabled") } }
            putJsonObject("stream_options") { put("include_usage", true) }
            if (!isReasoner) put("temperature", options.temperature)
            put("max_tokens", if (isReasoner) maxOf(options.maxTokens, 2048) else options.maxTokens)
            putJsonArray("messages") {
                if (system.isNotBlank()) addJsonObject {
                    put("role", "system"); put("content", system)
                }
                addJsonObject { put("role", "user"); put("content", user) }
            }
        }.toString()
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .apply {
                if (profile.apiKey.isNotBlank()) header("Authorization", "Bearer ${profile.apiKey}")
            }
            .post(body.toRequestBody(JSON))
            .build()
        return client.newCall(request)
    }

    private fun anthropicCall(profile: ApiProfile, base: String, system: String, user: String, options: ChatOptions): Call {
        val url = when {
            base.endsWith("/messages") -> base
            base.endsWith("/v1") -> "$base/messages"
            else -> "$base/v1/messages"
        }
        val body = buildJsonObject {
            put("model", profile.model)
            put("stream", true)
            put("temperature", options.temperature.coerceIn(0.0, 1.0))
            put("max_tokens", options.maxTokens)
            if (system.isNotBlank()) put("system", system)
            putJsonArray("messages") {
                addJsonObject { put("role", "user"); put("content", user) }
            }
        }.toString()
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("anthropic-version", "2023-06-01")
            .apply {
                if (profile.apiKey.isNotBlank()) header("x-api-key", profile.apiKey)
            }
            .post(body.toRequestBody(JSON))
            .build()
        return client.newCall(request)
    }

    private fun geminiCall(profile: ApiProfile, base: String, system: String, user: String, options: ChatOptions): Call {
        val url = "$base/models/${profile.model}:streamGenerateContent?alt=sse"
        val body = buildJsonObject {
            if (system.isNotBlank()) putJsonObject("system_instruction") {
                putJsonArray("parts") { addJsonObject { put("text", system) } }
            }
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") { addJsonObject { put("text", user) } }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", options.temperature)
                put("maxOutputTokens", options.maxTokens)
            }
        }.toString()
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .apply {
                if (profile.apiKey.isNotBlank()) header("x-goog-api-key", profile.apiKey)
            }
            .post(body.toRequestBody(JSON))
            .build()
        return client.newCall(request)
    }

    private fun extractDelta(kind: ProviderKind, element: kotlinx.serialization.json.JsonElement): Delta? {
        val root = element.jsonObject
        return when (kind) {
            ProviderKind.OPENAI_COMPAT -> {
                val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
                val delta = choice["delta"]?.jsonObject
                val reason = delta?.get("reasoning_content")?.takeUnless { it is JsonNull } ?: delta?.get("reasoning")
                val content = delta?.get("content") ?: choice["message"]?.jsonObject?.get("content")
                Delta(content = primText(content ?: JsonNull), reasoning = primText(reason ?: JsonNull))
            }
            ProviderKind.ANTHROPIC -> {
                if (root["type"]?.jsonPrimitive?.content != "content_block_delta") return null
                val t = root["delta"]?.jsonObject?.get("text")
                if (t == null || t is JsonNull) return null
                Delta(content = primText(t))
            }
            ProviderKind.GEMINI -> geminiText(root)
        }
    }

    private fun geminiText(root: JsonObject): Delta? {
        val parts = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray ?: return null
        val text = parts.mapNotNull { it as? JsonObject }
            .filterNot { it["thought"]?.jsonPrimitive?.contentOrNull == "true" }
            .joinToString("") { primText(it["text"] ?: JsonNull) }
        return if (text.isEmpty()) null else Delta(content = text)
    }

    private fun primText(p: kotlinx.serialization.json.JsonElement): String = when (p) {
        is JsonPrimitive -> p.content.takeUnless { it == "null" } ?: ""
        is JsonArray -> p.joinToString("") { (it as? JsonPrimitive)?.content.orEmpty() }
        else -> ""
    }

    /** 非流式：从一次性 JSON 响应里提取文本（正文/推理）。 */
    private fun extractWhole(kind: ProviderKind, element: kotlinx.serialization.json.JsonElement): Delta? {
        val root = element.jsonObject
        return when (kind) {
            ProviderKind.OPENAI_COMPAT -> {
                val msg = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
                val reason = msg?.get("reasoning_content")?.takeUnless { it is JsonNull } ?: msg?.get("reasoning")
                val content = msg?.get("content")
                Delta(content = primText(content ?: JsonNull), reasoning = primText(reason ?: JsonNull))
            }
            ProviderKind.ANTHROPIC -> {
                val text = root["content"]?.jsonArray.orEmpty().mapNotNull { it as? JsonObject }
                    .filter { (it["type"]?.jsonPrimitive?.contentOrNull ?: "text") == "text" }
                    .joinToString("") { primText(it["text"] ?: JsonNull) }
                if (text.isEmpty()) null else Delta(content = text)
            }
            ProviderKind.GEMINI -> geminiText(root)
        }
    }
    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
