package io.wenyou.textquest.data.engine

import io.wenyou.textquest.data.model.CharacterData
import io.wenyou.textquest.data.model.EntryKind
import io.wenyou.textquest.data.model.SessionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Plain-text transcript of a journey: narration and dialogue in order, without AI reasoning or system notices. */
object Transcript {
    fun format(title: String, state: SessionState, characters: List<CharacterData>, exportedAt: Long): String = buildString {
        appendLine(title)
        append("导出于 ").appendLine(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(exportedAt)))
        if (state.playerCharacterName.isNotBlank()) append("扮演：").appendLine(state.playerCharacterName)
        for (entry in state.history) {
            val text = entry.text.trim()
            if (text.isEmpty()) continue
            val line = when (entry.kind) {
                EntryKind.NARRATION, EntryKind.DM -> text
                EntryKind.CHARACTER -> "${entry.speaker.ifBlank { characters.firstOrNull { it.id == entry.speakerId }?.name ?: "角色" }}：$text"
                EntryKind.CHOICE -> "▶ ${entry.speaker.ifBlank { "你" }}：$text"
                EntryKind.SYSTEM, EntryKind.ERROR -> continue
            }
            appendLine().appendLine(line)
        }
    }

    /** Suggested document name; the system picker lets the player change it. */
    fun fileName(title: String): String =
        title.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").trim('_').take(60).ifBlank { "story" } + ".txt"
}
