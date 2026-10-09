package io.wenyou.textquest.data.engine

import io.wenyou.textquest.data.model.CharacterData
import io.wenyou.textquest.data.model.CharacterMetrics
import io.wenyou.textquest.data.model.CharacterState
import io.wenyou.textquest.data.model.CompareOp
import io.wenyou.textquest.data.model.Cond
import io.wenyou.textquest.data.model.CondType
import io.wenyou.textquest.data.model.Effect
import io.wenyou.textquest.data.model.EffectType
import io.wenyou.textquest.data.model.SessionState
import io.wenyou.textquest.data.model.Story
import kotlin.random.Random

/** 节点进入后的产物：新状态 + 需要写进日志的说明（如掷骰结果）。 */
data class Arrival(
    val state: SessionState,
    val notes: List<String> = emptyList()
)

/**
 * 纯逻辑的分支剧情引擎。不触碰 IO / 时间，便于单测。
 *
 * 状态模型：`flags` 是互斥场景标记的集合，`variables` 是数值变量；
 * 节点进入时可触发 [Effect]，选项有 [Cond] 门控与 [Effect]，支持掷骰
 * （dN → 变量）。
 */
object GameEngine {

    // ---------------- 模板插值：${变量名} ----------------

    fun renderTemplate(template: String, variables: Map<String, Double>): String {
        if (template.isEmpty()) return template
        val out = StringBuilder(template.length + 16)
        var i = 0
        while (i < template.length) {
            val c = template[i]
            if (c == '$' && i + 1 < template.length && template[i + 1] == '{') {
                val close = template.indexOf('}', i + 2)
                if (close > 0) {
                    val key = template.substring(i + 2, close).trim()
                    val value = variables[key]
                    out.append(if (value != null) formatNumber(value) else "")
                    i = close + 1
                    continue
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.US, "%.1f", value)

    // ---------------- 条件 ----------------

    fun evaluate(state: SessionState, conds: List<Cond>): Boolean {
        if (conds.isEmpty()) return true
        return conds.all { cond ->
            val charId = cond.charId.trim()
            val charState = if (charId.isNotEmpty()) state.characterStates[charId] ?: CharacterState() else null
            when (cond.type) {
                CondType.FLAG_TRUE ->
                    if (charState != null) cond.name in charState.flags else cond.name in state.flags
                CondType.FLAG_FALSE ->
                    if (charState != null) cond.name !in charState.flags else cond.name !in state.flags
                CondType.VAR -> {
                    val actual = if (charState != null) charState.metrics[cond.name] ?: 0.0
                    else state.variables[cond.name] ?: 0.0
                    val expect = cond.value
                    when (cond.op) {
                        CompareOp.EQ -> actual == expect
                        CompareOp.NE -> actual != expect
                        CompareOp.GT -> actual > expect
                        CompareOp.GTE -> actual >= expect
                        CompareOp.LT -> actual < expect
                        CompareOp.LTE -> actual <= expect
                    }
                }
            }
        }
    }

    fun visibleChoices(state: SessionState, story: Story, nodeId: String): List<io.wenyou.textquest.data.model.ChoiceData> {
        val node = story.nodes[nodeId] ?: return emptyList()
        return node.choices.filter { evaluate(state, it.conditions) }
    }

    // ---------------- 效果 ----------------

    data class EffectsOutcome(val state: SessionState, val notes: List<String>)

    fun applyEffects(state: SessionState, effects: List<Effect>): EffectsOutcome {
        var flags = state.flags
        var variables = state.variables
        var charStates = state.characterStates
        val notes = mutableListOf<String>()
        for (effect in effects) {
            val name = effect.name.trim()
            val charId = effect.charId.trim()
            if (charId.isNotEmpty()) {
                charStates = applyCharEffect(charStates, charId, effect, name, notes)
                continue
            }
            when (effect.type) {
                EffectType.SET_FLAG -> if (name.isNotEmpty()) flags = flags + name
                EffectType.CLEAR_FLAG -> if (name.isNotEmpty()) flags = flags - name
                EffectType.SET_VAR -> if (name.isNotEmpty()) variables = variables + (name to effect.value)
                EffectType.ADD_VAR -> if (name.isNotEmpty())
                    variables = variables + (name to ((variables[name] ?: 0.0) + effect.value))
                EffectType.RANDOM_VAR -> if (name.isNotEmpty()) {
                    val lo = minOf(effect.from, effect.to)
                    val hi = maxOf(effect.from, effect.to)
                    val rolled = if (hi > lo) Random.nextDouble(lo, hi) else lo
                    variables = variables + (name to rollPrecision(rolled))
                }
                EffectType.ROLL -> if (name.isNotEmpty()) {
                    val faces = effect.to.toInt().coerceAtLeast(2)
                    val rolled = Random.nextInt(faces) + 1
                    variables = variables + (name to rolled.toDouble())
                    notes += "🎲 掷 d$faces → $rolled" + if (name.isNotEmpty()) "（记录到「$name」）" else ""
                }
            }
        }
        val normalized = variables.mapValues { (_, v) -> rollPrecision(v) }
        return EffectsOutcome(state.copy(flags = flags, variables = normalized, characterStates = charStates), notes)
    }

    /** 对单个角色状态施加效果（数值 0..100，标记按需增删）。 */
    private fun applyCharEffect(
        cs: Map<String, CharacterState>,
        charId: String,
        effect: Effect,
        name: String,
        notes: MutableList<String>
    ): Map<String, CharacterState> {
        if (name.isEmpty()) return cs
        val cur = cs[charId] ?: CharacterState()
        var metrics = cur.metrics
        var flags = cur.flags
        when (effect.type) {
            EffectType.SET_FLAG -> flags = flags + name
            EffectType.CLEAR_FLAG -> flags = flags - name
            EffectType.SET_VAR -> metrics = setMetric(metrics, name, effect.value)
            EffectType.ADD_VAR -> metrics = setMetric(metrics, name, (metrics[name] ?: 0.0) + effect.value)
            EffectType.RANDOM_VAR -> {
                val lo = minOf(effect.from, effect.to)
                val hi = maxOf(effect.from, effect.to)
                val rolled = if (hi > lo) Random.nextDouble(lo, hi) else lo
                metrics = setMetric(metrics, name, rolled)
            }
            EffectType.ROLL -> {
                val faces = effect.to.toInt().coerceAtLeast(2)
                val rolled = Random.nextInt(faces) + 1
                metrics = setMetric(metrics, name, rolled.toDouble())
                notes += "🎲 掷 d$faces → $rolled（${CharacterMetrics.label(name)} 更新）"
            }
        }
        return cs + (charId to cur.copy(metrics = metrics, flags = flags))
    }

    private fun setMetric(metrics: Map<String, Double>, key: String, value: Double): Map<String, Double> =
        metrics + (key to CharacterMetrics.clamp(rollPrecision(value)))

    private fun rollPrecision(v: Double): Double = Math.round(v * 10.0) / 10.0

    // ---------------- 会话与流转 ----------------

    fun newSession(
        story: Story,
        characters: List<CharacterData> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): SessionState {
        val charStates = characters.associate { it.id to it.initial }
        return SessionState(
            storyId = story.id,
            currentNodeId = story.startNodeId,
            flags = story.initialFlags,
            variables = story.initialVariables,
            history = emptyList(),
            aiEndless = story.mode == io.wenyou.textquest.data.model.StoryMode.AI_DIRECTOR,
            updatedAt = now,
            characterStates = charStates
        )
    }

    /** 到达某节点：应用 onEnter 效果并记录说明。 */
    fun arriveAt(story: Story, state: SessionState, nodeId: String): Arrival {
        val node = story.nodes[nodeId]
        val afterEnter = if (node == null) EffectsOutcome(state, emptyList()) else applyEffects(state, node.onEnter)
        val base = afterEnter.state.copy(currentNodeId = nodeId, updatedAt = System.currentTimeMillis())
        return Arrival(base, afterEnter.notes)
    }


}
