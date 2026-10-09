package io.wenyou.textquest

import io.wenyou.textquest.data.model.*
import io.wenyou.textquest.ui.screens.cardIntroduction
import org.junit.Assert.*
import org.junit.Test

class StoryPresentationTest {
    @Test fun descriptionUsesStoredWorldThenSubtitleThenOnlyTheOpeningNode() {
        val story = Story("intro", "标题", subtitle = "短句", startNodeId = "opening",
            nodes = mapOf("opening" to StoryNode("opening", text = "开场"), "ending" to StoryNode("ending", text = "结局秘密")),
            ai = AiStorySettings(worldSummary = "城市\n  和雨夜", directorExtra = "私有导演规则"))
        assertEquals("城市 和雨夜", story.cardIntroduction())
        assertEquals("短句", story.copy(ai = AiStorySettings()).cardIntroduction())
        assertEquals("开场", story.copy(ai = AiStorySettings(), subtitle = " ").cardIntroduction())
        assertEquals("", story.copy(ai = AiStorySettings(), subtitle = "", startNodeId = "missing").cardIntroduction())
        assertEquals(300, story.copy(ai = AiStorySettings(worldSummary = "雨".repeat(1000))).cardIntroduction().length)
    }
}
