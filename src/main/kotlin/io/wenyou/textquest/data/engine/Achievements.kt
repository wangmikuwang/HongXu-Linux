package io.wenyou.textquest.data.engine

import io.wenyou.textquest.data.model.*

enum class Achievement(val title: String, val description: String, val target: Int) {
    FIRST_JOURNEY("初次启程", "开始一部剧情", 1),
    EXPLORER("世界探索者", "尝试 3 部不同剧情", 3),
    DECISIONS("命运抉择", "单局作出 10 次选择或自由输入", 10),
    FIRST_AI("灵感火花", "成功完成 1 轮 AI 场景或导演续写", 1),
    AI_STORYTELLER("共创故事", "单局成功完成 10 轮 AI 续写", 10),
    FIRST_ENDING("旅途终章", "抵达一个作者设定的结局", 1),
    ENDING_COLLECTOR("结局收藏家", "抵达 3 个不同的剧情结局", 3)
}

object Achievements {
    /** Milestone sets stop growing at their target; replaying a save cannot farm progress. */
    fun observe(previous: List<AchievementRecord>, story: Story, state: SessionState, now: Long): List<AchievementRecord> {
        if (state.storyId != story.id || (story.mode == StoryMode.SCRIPT && state.currentNodeId !in story.nodes)) return previous
        val ending = story.mode == StoryMode.SCRIPT && story.nodes[state.currentNodeId]?.kind == NodeKind.ENDING
        return Achievement.entries.map { definition ->
            val old = previous.firstOrNull { it.id == definition.name } ?: AchievementRecord(definition.name)
            val milestone = when (definition) {
                Achievement.EXPLORER -> story.id
                Achievement.ENDING_COLLECTOR -> if (ending) "${story.id.length}:${story.id}${state.currentNodeId}" else null
                else -> null
            }
            val milestones = if (milestone != null && old.milestones.size < definition.target) old.milestones + milestone else old.milestones
            val observed = when (definition) {
                Achievement.FIRST_JOURNEY -> 1
                Achievement.EXPLORER, Achievement.ENDING_COLLECTOR -> milestones.size
                Achievement.DECISIONS -> maxOf(state.choicesTaken, state.history.count { it.kind == EntryKind.CHOICE })
                Achievement.FIRST_AI, Achievement.AI_STORYTELLER -> state.aiTurns
                Achievement.FIRST_ENDING -> if (ending) 1 else 0
            }
            val progress = maxOf(old.progress, observed).coerceIn(0, definition.target)
            old.copy(progress = progress, milestones = milestones,
                unlockedAt = if (progress < definition.target) 0L else old.unlockedAt.takeIf { it > 0L } ?: now)
        }
    }

    /** Backups merge local accomplishments; an old backup must not revoke them. */
    fun merge(local: List<AchievementRecord>, incoming: List<AchievementRecord>, now: Long): List<AchievementRecord> =
        Achievement.entries.map { definition ->
            val records = (local + incoming).filter { it.id == definition.name }
            val milestones = records.flatMap { it.milestones }.distinct().take(definition.target).toSet()
            val progress = maxOf(records.maxOfOrNull { it.progress } ?: 0, milestones.size).coerceIn(0, definition.target)
            AchievementRecord(definition.name, progress,
                if (progress < definition.target) 0L else records.map { it.unlockedAt }.filter { it > 0L }.minOrNull() ?: now,
                milestones)
        }
}
