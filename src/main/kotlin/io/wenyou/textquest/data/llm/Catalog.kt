package io.wenyou.textquest.data.llm

import io.wenyou.textquest.data.model.ProviderKind

/**
 * 内置「各品牌」接入预设。绝大多数服务都提供 OpenAI 兼容端点，
 * 因此同一套 chat/completions 逻辑即可覆盖国产大模型；Anthropic 与
 * Gemini 使用各自原生流式协议（见 [ChatClient]）。
 */
data class ProviderPreset(
    val key: String,
    val label: String,
    val kind: ProviderKind,
    val baseUrl: String = "",
    val models: List<String> = emptyList(),
    val needsKey: Boolean = true,
    val note: String = "",
    /** Where users create an API key; empty when the service needs none or has no stable page. */
    val keyUrl: String = ""
)

object ProviderCatalog {

    fun presets(): List<ProviderPreset> = listOf(
        ProviderPreset("deepseek", "DeepSeek 深度求索", ProviderKind.OPENAI_COMPAT, "https://api.deepseek.com/v1",
            listOf("deepseek-flash", "deepseek-v4-pro"), note = "官方现行模型；留空单价可估算峰谷费用，base 已含 /v1", keyUrl = "https://platform.deepseek.com/api_keys"),
        ProviderPreset("openai", "OpenAI", ProviderKind.OPENAI_COMPAT, "https://api.openai.com/v1",
            listOf("gpt-4o", "gpt-4o-mini", "gpt-4.1"), note = "官方 OpenAI，需海外网络", keyUrl = "https://platform.openai.com/api-keys"),
        ProviderPreset("moonshot", "Moonshot Kimi", ProviderKind.OPENAI_COMPAT, "https://api.moonshot.cn/v1",
            listOf("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"), note = "长上下文友好", keyUrl = "https://platform.moonshot.cn/console/api-keys"),
        ProviderPreset("zhipu", "智谱 GLM", ProviderKind.OPENAI_COMPAT, "https://open.bigmodel.cn/api/paas/v4",
            listOf("glm-4-flash", "glm-4-air", "glm-4-plus"), note = "glm-4-flash 免费档", keyUrl = "https://open.bigmodel.cn/usercenter/apikeys"),
        ProviderPreset("qwen", "阿里云百炼 Qwen", ProviderKind.OPENAI_COMPAT, "https://dashscope.aliyuncs.com/compatible-mode/v1",
            listOf("qwen-plus", "qwen-turbo", "qwen-max", "qwen-long"), note = "DashScope 兼容模式", keyUrl = "https://bailian.console.aliyun.com/"),
        ProviderPreset("volcengine", "火山方舟 豆包", ProviderKind.OPENAI_COMPAT, "https://ark.cn-beijing.volces.com/api/v3",
            listOf("doubao-pro-32k", "doubao-lite-32k"), note = "填推理接入点 ID（ep-…）", keyUrl = "https://console.volcengine.com/ark"),
        ProviderPreset("openrouter", "OpenRouter", ProviderKind.OPENAI_COMPAT, "https://openrouter.ai/api/v1",
            listOf("openai/gpt-4o", "anthropic/claude-3.5-sonnet", "google/gemini-2.0-flash-001"), note = "聚合多家模型", keyUrl = "https://openrouter.ai/keys"),
        ProviderPreset("siliconflow", "硅基流动 SiliconFlow", ProviderKind.OPENAI_COMPAT, "https://api.siliconflow.cn/v1",
            listOf("deepseek-ai/DeepSeek-V3", "Qwen/Qwen2.5-72B-Instruct"), note = "中文模型多", keyUrl = "https://cloud.siliconflow.cn/account/ak"),
        ProviderPreset("anthropic", "Anthropic Claude", ProviderKind.ANTHROPIC, "https://api.anthropic.com",
            listOf("claude-3-5-sonnet-latest", "claude-3-5-haiku-latest"), note = "原生 Messages API", keyUrl = "https://console.anthropic.com/settings/keys"),
        ProviderPreset("gemini", "Google Gemini", ProviderKind.GEMINI, "https://generativelanguage.googleapis.com/v1beta",
            listOf("gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash"), note = "官方生成式语言 API", keyUrl = "https://aistudio.google.com/apikey"),
        ProviderPreset("ollama", "Ollama（本地免费）", ProviderKind.OPENAI_COMPAT, "http://127.0.0.1:11434/v1",
            listOf("qwen2.5:7b", "llama3.1:8b", "gemma2:9b"), needsKey = false, note = "完全离线，无需 Key"),
        ProviderPreset("mimo", "小米 MiMo（小米官方）", ProviderKind.OPENAI_COMPAT, "https://api.xiaomimimo.com/v1",
            listOf("mimo-v2-flash", "mimo-v2-pro", "mimo-v2.5-pro", "mimo-v2-omni"),
            needsKey = true,
            note = "OpenAI 兼容；Key 以 sk- 开头（若用 Token Plan，地址改成 token-plan-cn.xiaomimimo.com/v1，Key 以 tp- 开头）"),
        ProviderPreset("custom", "自定义（OpenAI 兼容）", ProviderKind.OPENAI_COMPAT, "",
            emptyList(), note = "任意中转/私有端点填入 baseUrl")
    )

    fun find(key: String): ProviderPreset? = presets().firstOrNull { it.key == key }
}
