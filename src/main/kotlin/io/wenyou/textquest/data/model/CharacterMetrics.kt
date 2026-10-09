package io.wenyou.textquest.data.model

/**
 * 角色状态指标注册表（参照养成/文字游戏常用设计）。
 * 数值统一 0..100；[atmosphere] 为“氛围值”（仅影响 AI 语气与暧昧尺度，内容保持非露骨、角色均成年）。
 */
object CharacterMetrics {

    data class Def(
        val key: String,
        val label: String,
        val icon: String,
        val atmosphere: Boolean = false,
        val desc: String = ""
    )

    val defs: List<Def> = listOf(
        Def("affection", "好感度", "❤️", atmosphere = true, desc = "对这个角色的好感，越高越亲昵。"),
        Def("trust", "信任", "🤝", desc = "她/他愿意对你放下防备的程度。"),
        Def("mood", "心情", "🎭", atmosphere = true, desc = "当前情绪好坏，影响语气与反应。"),
        Def("energy", "精力", "⚡", desc = "精神与体力，过低会疲惫、易怒。"),
        Def("health", "身体状况", "🩹", desc = "是否健康/带伤，影响行动与描写。"),
        Def("fatigue", "疲劳", "💤", desc = "越累越需要休息，也越容易心软。"),
        Def("arousal", "性欲(氛围值)", "🔥", atmosphere = true, desc = "仅作为暧昧/亲密氛围数值，不写露骨内容。")
    )

    fun byKey(key: String): Def? = defs.firstOrNull { it.key == key }
    fun label(key: String): String = byKey(key)?.label ?: key
    fun icon(key: String): String = byKey(key)?.icon ?: "📌"

    /** 归一化到 0..100。 */
    fun clamp(v: Double): Double = v.coerceIn(0.0, 100.0)
    fun clamp(v: Int): Int = v.coerceIn(0, 100)
}
