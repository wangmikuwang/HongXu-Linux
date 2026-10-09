package io.wenyou.textquest

import io.wenyou.textquest.data.engine.Transcript
import io.wenyou.textquest.data.model.*
import io.wenyou.textquest.data.repo.LocalLibrary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExplorationTest {
    @get:Rule val temp = TemporaryFolder()
    private val story = Story("s", "雨夜", nodes = mapOf(
        "start" to StoryNode("start", text = "开场", choices = listOf(ChoiceData("离开", "end"))),
        "end" to StoryNode("end", NodeKind.ENDING, text = "结束")
    ))

    @Test fun reachedNodesPersistOnlyForScriptsAndSurviveOlderBackups() = runBlocking {
        val dir = temp.newFolder()
        val library = LocalLibrary(dir)
        library.recordAchievements(story, SessionState("s", "start"))
        library.recordAchievements(story, SessionState("s", "end"))
        library.recordAchievements(story, SessionState("s", "missing"))
        library.recordAchievements(story.copy(id = "ai", mode = StoryMode.AI_DIRECTOR), SessionState("ai", "start"))
        assertEquals(listOf(StoryProgress("s", setOf("start", "end"))), library.progress.value)
        assertEquals(library.progress.value, LocalLibrary(dir).progress.value)

        // Restoring a backup merges exploration instead of forgetting it; old backups have no progress field.
        library.importBundle(library.bundle().copy(progress = listOf(StoryProgress("s", setOf("other")), StoryProgress("t", setOf("start")))))
        assertEquals(setOf("start", "end", "other"), library.progress.value.first { it.storyId == "s" }.visitedNodes)
        library.importBundle(AppJson.decodeFromString(AppBundle.serializer(), """{"version":1}"""))
        assertEquals(2, library.progress.value.size)
    }

    @Test fun autoSaveNamesAreRecognisedAndCustomNamesKept() {
        assertTrue(isAutoSaveName(autoSaveName("雾城 · 忆城", 5)))
        assertTrue(isAutoSaveName(""))
        assertFalse(isAutoSaveName("钟楼前的决定"))
        assertFalse(isAutoSaveName("第 5 步"))
    }

    @Test fun transcriptKeepsStoryOrderAndDropsReasoningAndNotices() {
        val state = SessionState("s", playerCharacterName = "林晚秋", history = listOf(
            LogEntry(EntryKind.NARRATION, text = "雨敲着玻璃。", reasoning = "内部思考"),
            LogEntry(EntryKind.CHARACTER, speakerId = "c1", text = "好巧。"),
            LogEntry(EntryKind.CHOICE, speaker = "林晚秋", text = "坐下"),
            LogEntry(EntryKind.SYSTEM, text = "已存档"),
            LogEntry(EntryKind.DM, text = "  灯亮了。  "),
            LogEntry(EntryKind.NARRATION, text = " ")
        ))
        val text = Transcript.format("雨夜", state, listOf(CharacterData("c1", "阿蛮")), 0)
        val body = text.lines().drop(3).filter { it.isNotBlank() }
        assertEquals(listOf("雨敲着玻璃。", "阿蛮：好巧。", "▶ 林晚秋：坐下", "灯亮了。"), body)
        assertTrue(text.startsWith("雨夜\n导出于 "))
        assertTrue("扮演：林晚秋" in text)
        assertFalse("内部思考" in text || "已存档" in text)
        assertEquals("雾城_忆城_a_b.txt", Transcript.fileName("雾城 忆城/a:b"))
        assertEquals("story.txt", Transcript.fileName("///"))
    }
}
