package io.wenyou.textquest.ui.screens

import io.wenyou.textquest.data.model.Story

/** Display only stored story content; never director instructions or private character prompts. */
internal fun Story.cardIntroduction(): String {
    val text = ai.worldSummary.takeIf { it.isNotBlank() }
        ?: subtitle.takeIf { it.isNotBlank() }
        ?: nodes[startNodeId]?.text.orEmpty()
    return text.trim().replace(Regex("\\s+"), " ").take(300)
}
