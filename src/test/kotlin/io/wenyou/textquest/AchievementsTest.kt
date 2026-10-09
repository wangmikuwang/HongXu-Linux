package io.wenyou.textquest

import io.wenyou.textquest.data.engine.*
import io.wenyou.textquest.data.model.*
import io.wenyou.textquest.data.repo.LocalLibrary
import io.wenyou.textquest.ui.vm.PlayViewModel
import io.wenyou.textquest.data.ai.AiDirector
import io.wenyou.textquest.data.llm.ChatClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import okhttp3.ResponseBody.Companion.toResponseBody

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsTest {
    @get:Rule val temp = TemporaryFolder()
    private fun story(id: String = "s") = Story(id, "剧情", nodes = mapOf(
        "start" to StoryNode("start", text = "开场", choices = listOf(ChoiceData("留下", "@self"), ChoiceData("离开", "end"))),
        "end" to StoryNode("end", NodeKind.ENDING, text = "结束")
    ))

    @Test fun milestonesDeduplicateAndOnlyRealEndingsCount() {
        var records = emptyList<AchievementRecord>()
        for (id in listOf("s", "s", "t", "u")) {
            val story = story(id)
            records = Achievements.observe(records, story, SessionState(id, "end", choicesTaken = 10, aiTurns = 10), 100)
        }
        assertEquals(7, records.count { it.unlockedAt == 100L })
        assertEquals(3, records.first { it.id == "ENDING_COLLECTOR" }.milestones.size)
        assertEquals(records, Achievements.observe(records, story(), SessionState("s", "start"), 200))
        val director = story().copy(mode = StoryMode.AI_DIRECTOR)
        val result = Achievements.observe(emptyList(), director, SessionState("s", "end"), 1)
        assertEquals(0, result.first { it.id == "FIRST_ENDING" }.progress)
        assertEquals(emptyList<AchievementRecord>(), Achievements.observe(emptyList(), story(), SessionState("s", "missing"), 1))
        assertEquals(records, Achievements.merge(records, emptyList(), 300))
        val old = AppJson.decodeFromString(SessionState.serializer(), """{"storyId":"s","currentNodeId":"start"}""")
        assertEquals(0, old.choicesTaken)
        assertEquals(0, old.aiTurns)
    }

    @Test fun concurrentWritesReloadBackupAndFailureRemainConsistent() = runBlocking {
        val dir = temp.newFolder()
        val library = LocalLibrary(dir)
        val story = story()
        val state = SessionState("s", "end", choicesTaken = 10)
        val unlocked = coroutineScope { List(20) { async(Dispatchers.IO) { library.recordAchievements(story, state) } }.awaitAll().flatten() }
        assertEquals(3, unlocked.size)
        assertEquals(unlocked.size, unlocked.distinct().size)
        assertEquals(library.achievements.value, LocalLibrary(dir).achievements.value)
        val backup = library.bundle()
        val other = LocalLibrary(temp.newFolder())
        other.importBundle(backup)
        other.importBundle(AppBundle())
        assertEquals(library.achievements.value, other.achievements.value)
        val brokenDir = temp.newFolder()
        File(brokenDir, "achievements.json").mkdir()
        val broken = LocalLibrary(brokenDir)
        val before = broken.achievements.value
        try { broken.recordAchievements(story, state); fail("write should fail") } catch (_: IOException) { }
        assertEquals(before, broken.achievements.value)
        assertNotNull(broken.writeError.value)
    }

    @Test fun realChoicesSurviveSaveLoadAndRestartWithoutFarming() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val library = LocalLibrary(temp.newFolder())
            library.upsertStory(story())
            val vm = PlayViewModel("s", "new", library, AiDirector(ChatClient())) { null }
            vm.selectPlayerCharacter("")
            repeat(10) { vm.chooseAuthored(0) }
            assertEquals(10, vm.ui.value.session!!.choicesTaken)
            library.upsertSave(SaveSlot("save", "存档", 1, 1, vm.ui.value.session!!))
            val loaded = PlayViewModel("s", "save", library, AiDirector(ChatClient())) { null }
            loaded.chooseAuthored(1)
            withTimeout(5_000) { library.achievements.first { it.count { r -> r.unlockedAt > 0 } == 3 } }
            val before = library.achievements.value
            assertEquals(11, loaded.ui.value.session!!.choicesTaken)
            assertEquals(emptyList<String>(), library.recordAchievements(story(), loaded.ui.value.session!!))
            loaded.restart()
            loaded.selectPlayerCharacter("")
            assertEquals(0, loaded.ui.value.session!!.choicesTaken)
            library.recordAchievements(story(), loaded.ui.value.session!!)
            assertEquals(before, library.achievements.value)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun failedAiDoesNotEarnAnAiAchievement() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(500).message("test failure").body("failure".toResponseBody()).build()
        }.build()
        try {
            val library = LocalLibrary(temp.newFolder())
            val story = Story("ai", "AI", nodes = mapOf("start" to StoryNode("start", NodeKind.AI)))
            library.upsertStory(story)
            library.upsertProvider(ApiProfile("p", "test", baseUrl = "http://localhost/v1", model = "test"))
            val vm = PlayViewModel("ai", "new", library, AiDirector(ChatClient(client))) { "p" }
            vm.selectPlayerCharacter("")
            val failed = withTimeout(10_000) { vm.ui.first { it.stoppedTitle == "AI 生成失败" } }
            assertEquals(0, failed.session!!.aiTurns)
            library.recordAchievements(story, failed.session!!)
            assertEquals(0L, library.achievements.value.first { it.id == "FIRST_AI" }.unlockedAt)
        } finally {
            client.dispatcher.executorService.shutdownNow()
            client.connectionPool.evictAll()
            Dispatchers.resetMain()
        }
    }
}
