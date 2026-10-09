package io.wenyou.textquest.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.KSerializer

/** 全局 JSON 配置：容忍新增字段、未知字段，便于手工编辑与跨版本迁移。
 *
 *  `coerceInputValues` 使无法映射的枚举值回落为字段默认值（而非整段解析失败），
 *  因此手工编辑 providers.json 时 `kind` 写成 `OPENAI_COMPAT` 也能安全回落，不丢整段数据。 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    coerceInputValues = true
    prettyPrint = false
}

// ---------------------------------------------------------------------------
// 多品牌 API 档案
// ---------------------------------------------------------------------------

/** 协议类别：绝大多数国产/开源服务走 OpenAI 兼容协议。 */
@Serializable(with = ProviderKindSerializer::class)
enum class ProviderKind(val label: String) {
    @SerialName("openai_compat") OPENAI_COMPAT("OpenAI 兼容（DeepSeek/Kimi/GLM/Qwen/OpenRouter…）"),
    @SerialName("anthropic") ANTHROPIC("Anthropic Claude"),
    @SerialName("gemini") GEMINI("Google Gemini")
}

/** [ProviderKind] 的容错序列化：兼容 `@SerialName` 与常量名（`openai_compat`/`OPENAI_COMPAT` 皆可）。 */
@OptIn(kotlinx.serialization.InternalSerializationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
object ProviderKindSerializer : KSerializer<ProviderKind> {
    private val byName: Map<String, ProviderKind> = buildMap {
        for (c in ProviderKind.entries) {
            put(c.name.lowercase(), c)
            val sn = runCatching { ProviderKind::class.java.getField(c.name).getAnnotation(SerialName::class.java)?.value }
                .getOrNull()
            if (sn != null) put(sn.lowercase(), c)
        }
    }
    override val descriptor: SerialDescriptor = kotlinx.serialization.descriptors.buildSerialDescriptor(
        "ProviderKind", kotlinx.serialization.descriptors.SerialKind.ENUM
    )
    override fun serialize(encoder: Encoder, value: ProviderKind) =
        encoder.encodeString(value.name)
    override fun deserialize(decoder: Decoder): ProviderKind {
        val raw = decoder.decodeString().trim()
        if (raw.isEmpty()) return ProviderKind.OPENAI_COMPAT
        return byName[raw.lowercase()] ?: ProviderKind.OPENAI_COMPAT
    }
}

/** 用户配置的一条「服务接入」，key 仅保存在本机。 */
@Serializable
data class ApiProfile(
    val id: String,
    val name: String,
    val kind: ProviderKind = ProviderKind.OPENAI_COMPAT,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val temperature: Double = 0.85,
    val maxTokens: Int = 1024,
    val note: String = "",
    val inputPrice: Double? = null,
    val outputPrice: Double? = null,
    val cachedPrice: Double? = null,
    val cacheWritePrice: Double? = null,
    val priceCurrency: String = "CNY"
)

// ---------------------------------------------------------------------------
// 角色
// ---------------------------------------------------------------------------

/** 角色性取向（用于角色库分类与「性取向」检索；内置角色已按题材预设，可在编辑器修改）。 */
@Serializable
enum class SexualOrientation(val label: String) {
    @SerialName("straight") STRAIGHT("异性恋"),
    @SerialName("gay") GAY("男同性恋"),
    @SerialName("lesbian") LESBIAN("女同性恋"),
    @SerialName("bi") BI("双性恋"),
    @SerialName("pan") PAN("泛性恋"),
    @SerialName("asexual") ASEXUAL("无性恋"),
    @SerialName("unknown") UNKNOWN("未标注")
}

/** 剧情内容分类（全年龄 / LGBT / 成人18+）。 */
@Serializable
enum class ContentClass(val label: String) {
    @SerialName("all_age") ALL_AGE("全年龄"),
    @SerialName("lgbt") LGBT("LGBT"),
    @SerialName("adult") ADULT("18+")
}

/** 角色卡：性格/说话方式/背景会注入到 AI 人设与叙事系统提示中，
 * 在作者自编节点中则由 [speakerId] 决定气泡归属（纯离线也能用）。 */
@Serializable
data class CharacterData(
    val id: String,
    val name: String,
    val emoji: String = "🎭",
    val colorIndex: Int = 0,
    val tagline: String = "",
    val personality: String = "",
    val speechStyle: String = "",
    val background: String = "",
    val exampleDialogue: String = "",
    val greeting: String = "",
    /** 附加人设提示语（高优先级）：拼接系统提示时排在最前，用于强化身份/世界观/规则。 */
    val extraPrompt: String = "",
    /** 初始状态（开局新会话沿用；可在角色编辑器调整，对局中由 AI 导演实时更新）。 */
    val initial: CharacterState = CharacterState(),
    /** 性取向（角色库分类用）。 */
    val orientation: SexualOrientation = SexualOrientation.UNKNOWN,
    /** 是否为 LGBT 向内容（供「内容开关」过滤显示）。 */
    val lgbt: Boolean = false,
    /** 是否为成人向内容。 */
    val adult: Boolean = false
)

// ---------------------------------------------------------------------------
// 剧情（作者自编 + AI 增强）
// ---------------------------------------------------------------------------

@Serializable
enum class StoryMode(val label: String) {
    @SerialName("script") SCRIPT("分支剧本（可离线游玩）"),
    @SerialName("ai_dm") AI_DIRECTOR("AI 导演（自由对话推进）")
}

@Serializable
enum class NodeKind(val label: String) {
    @SerialName("narration") NARRATION("叙述"),
    @SerialName("ai") AI("AI 生成场景"),
    @SerialName("ending") ENDING("结局")
}

@Serializable
enum class CondType(val label: String) {
    @SerialName("flag_true") FLAG_TRUE("拥有标记"),
    @SerialName("flag_false") FLAG_FALSE("没有标记"),
    @SerialName("var") VAR("变量比较")
}

@Serializable
enum class CompareOp(val label: String) {
    @SerialName("eq") EQ("=="),
    @SerialName("ne") NE("!="),
    @SerialName("gt") GT(">"),
    @SerialName("gte") GTE(">="),
    @SerialName("lt") LT("<"),
    @SerialName("lte") LTE("<=")
}

@Serializable
enum class EffectType(val label: String) {
    @SerialName("set_flag") SET_FLAG("设置标记"),
    @SerialName("clear_flag") CLEAR_FLAG("清除标记"),
    @SerialName("set_var") SET_VAR("变量 = 值"),
    @SerialName("add_var") ADD_VAR("变量 += 值"),
    @SerialName("random_var") RANDOM_VAR("变量 = 区间随机"),
    @SerialName("roll") ROLL("掷骰：变量 = dN 结果")
}

/** 选项显示条件（全部满足才显示）。charId 非空时作用于该角色，否则作用于全局。 */
@Serializable
data class Cond(
    val type: CondType = CondType.VAR,
    val name: String = "",
    val op: CompareOp = CompareOp.GTE,
    val value: Double = 0.0,
    val charId: String = ""
)

/** 选择/进入节点时执行的效果（可多行，按顺序执行）。charId 非空时作用于该角色状态。 */
@Serializable
data class Effect(
    val type: EffectType = EffectType.SET_FLAG,
    val name: String = "",
    val value: Double = 0.0,
    val from: Double = 0.0,
    val to: Double = 100.0,
    val charId: String = ""
)

@Serializable
data class ChoiceData(
    val text: String,
    val next: String = "",
    val conditions: List<Cond> = emptyList(),
    val effects: List<Effect> = emptyList(),
    val hint: String = ""
)

@Serializable
data class StoryNode(
    val id: String = "",
    val kind: NodeKind = NodeKind.NARRATION,
    val title: String = "",
    val speakerId: String = "",
    val text: String = "",
    /** kind = AI 时：让模型据此生成这一场景（含变量上下文与最近剧情）。 */
    val prompt: String = "",
    val choices: List<ChoiceData> = emptyList(),
    val onEnter: List<Effect> = emptyList(),
    /** AI 场景 / 结局后的去向：为空表示故事结束或停留。 */
    val endTarget: String = ""
)

/** AI 相关剧本设置。 */
@Serializable
data class AiStorySettings(
    val worldSummary: String = "",
    val tone: String = "以细腻的中文文学性叙述为主，第三人称，节奏自然。",
    val directorExtra: String = "",
    val temperature: Double = 0.95,
    val maxTokens: Int = 900,
    val historyWindow: Int = 40
)

/** 一部可玩的剧情。AI_DIRECTOR 模式下仅用开场节点渲染序章后即进入自由对话。 */
@Serializable
data class Story(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val coverEmoji: String = "📖",
    val colorIndex: Int = 0,
    val genre: String = "",
    val mode: StoryMode = StoryMode.SCRIPT,
    val characterIds: List<String> = emptyList(),
    val startNodeId: String = "start",
    val nodes: Map<String, StoryNode> = emptyMap(),
    val initialVariables: Map<String, Double> = emptyMap(),
    val initialFlags: Set<String> = emptySet(),
    val ai: AiStorySettings = AiStorySettings(),
    /** 是否为 LGBT 向内容（供「内容开关」过滤显示）。 */
    val lgbt: Boolean = false,
    /** 是否为成人向内容。 */
    val adult: Boolean = false
)

// ---------------------------------------------------------------------------
// 对局与存档
// ---------------------------------------------------------------------------

@Serializable
enum class EntryKind(val label: String) {
    @SerialName("narration") NARRATION("旁白"),
    @SerialName("character") CHARACTER("角色"),
    @SerialName("choice") CHOICE("玩家选择"),
    @SerialName("dm") DM("AI 导演"),
    @SerialName("system") SYSTEM("系统"),
    @SerialName("error") ERROR("提示")
}

@Serializable
data class LogEntry(
    val kind: EntryKind = EntryKind.NARRATION,
    val speaker: String = "",
    val speakerId: String = "",
    val text: String,
    val reasoning: String = "",
    val ts: Long = 0L
)

/** 单个角色的当前状态（数值 0..100 + 标记 + 穿着/外观描述）。 */
@Serializable
data class CharacterState(
    val metrics: Map<String, Double> = emptyMap(),
    val flags: Set<String> = emptySet(),
    val description: String = "",
    val relationships: Map<String, String> = emptyMap(),
    val lastChangeReason: String = ""
) {
    fun metric(key: String, def: Double = 0.0): Double = metrics[key] ?: def
}

/** 一局游戏的完整状态（可序列化存档）。 */
@Serializable
data class SessionState(
    val storyId: String,
    val currentNodeId: String = "",
    val flags: Set<String> = emptySet(),
    val variables: Map<String, Double> = emptyMap(),
    val history: List<LogEntry> = emptyList(),
    val aiEndless: Boolean = false,
    val updatedAt: Long = 0L,
    /** 角色状态（key = 角色 id）。 */
    val characterStates: Map<String, CharacterState> = emptyMap(),
    /** AI 已生成、等待玩家选择的动态选项；放进存档以避免读档时重复请求模型。 */
    val pendingAiChoices: List<ChoiceData> = emptyList(),
    /** 即使模型没有返回选项，也记录本轮已完成，读档后展示“继续生成”而非自动重跑。 */
    val aiAwaitingChoice: Boolean = false,
    val memory: String = "",
    /** Lifetime counts within this journey, independent of the bounded history window. */
    val choicesTaken: Int = 0,
    val aiTurns: Int = 0,
    /** Player identity snapshot, retained by saves even if the character is later removed. */
    val playerCharacterId: String = "",
    val playerCharacterName: String = "",
    /** How quickly AI turns move the story: [ScenePace] name. */
    val pace: String = "NORMAL",
    /** Recap carried from the previous chapter; kept apart from [memory], which the AI rewrites every turn. */
    val recap: String = "",
    /** Memos from talking to the director outside the story (developer mode); later turns follow them. */
    val directorNotes: List<String> = emptyList()
)

enum class ScenePace(val label: String, val hint: String, val instruction: String) {
    SLOW("慢", "细腻描写，一次只推进一个片刻", "本轮放慢节奏：只推进一个很小的动作或片刻，着重细节、感官描写、人物神态与对话往来；不跳过时间，不引入新的重大事件。"),
    NORMAL("标准", "叙述与推进均衡", ""),
    FAST("快", "略过过渡，直奔下一个关键事件", "本轮加快节奏：明显推进剧情，可略过过渡与琐碎细节、适当跳过时间，直接进入下一个关键事件或转折；叙述简洁。");

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}

@Serializable
data class SaveSlot(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val state: SessionState
)

// ---------------------------------------------------------------------------
// 导入 / 导出
// ---------------------------------------------------------------------------

@Serializable
data class AppBundle(
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val providers: List<ApiProfile> = emptyList(),
    val characters: List<CharacterData> = emptyList(),
    val stories: List<Story> = emptyList(),
    val saves: List<SaveSlot> = emptyList(),
    /** The player's baseline (底层基调), carried only by their own full backups, never by shared content. */
    val baseline: String = "",
    val achievements: List<AchievementRecord> = emptyList(),
    /** Branch nodes reached per story; older backups simply omit it. */
    val progress: List<StoryProgress> = emptyList(),
    /** Which app wrote this share or backup; empty for content written before origins existed. */
    val origin: String = ""
)

@Serializable
data class AchievementRecord(
    val id: String,
    val progress: Int = 0,
    val unlockedAt: Long = 0L,
    val milestones: Set<String> = emptySet()
)

/** Nodes of a branching story the player has reached in any journey. */
@Serializable
data class StoryProgress(val storyId: String, val visitedNodes: Set<String> = emptySet())

/** Saves are named "<story> · N 步" until the player renames them. */
fun autoSaveName(storyTitle: String, steps: Int) = "$storyTitle · $steps 步"
fun isAutoSaveName(name: String) = name.isBlank() || AUTO_SAVE_NAME.matches(name)
private val AUTO_SAVE_NAME = Regex(".* · \\d+ 步")

