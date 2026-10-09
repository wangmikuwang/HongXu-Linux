package io.wenyou.textquest

import io.wenyou.textquest.data.ai.AiDirector
import io.wenyou.textquest.data.llm.*
import io.wenyou.textquest.data.model.*
import io.wenyou.textquest.data.repo.LocalLibrary
import io.wenyou.textquest.ui.vm.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationTest {
    @get:Rule val temp = TemporaryFolder()
    private val profile = ApiProfile("p", "mock", baseUrl = "http://localhost/v1", model = "mock")
    private val good = """{"entries":[{"text":"灯亮了。"},{"speakerId":"b","text":"请坐。"}],"choices":[{"text":"在窗边坐下"},{"text":"询问来意"}]}"""
    private fun client(reply: (String) -> String) = OkHttpClient.Builder().addInterceptor { chain ->
        val buffer = okio.Buffer(); chain.request().body!!.writeTo(buffer)
        val content = reply(buffer.readUtf8())
        val envelope = """{"choices":[{"message":{"reasoning_content":"独立思考","content":${AppJson.encodeToString(kotlinx.serialization.serializer<String>(), content)}}}]}"""
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(envelope.toResponseBody()).build()
    }.build()
    private fun OkHttpClient.close() { dispatcher.executorService.shutdownNow(); connectionPool.evictAll() }

    @Test fun malformedOptionalFieldsDoNotDiscardDialogueOrSuggestions() {
        val scene = AiDirector(ChatClient()).parseScene("""{"entries":[{"text":"门开了。"},{"speakerId":"a","text":"欢迎。"}],"choices":["进去",{"text":"稍等[to:hall]"},null],"state":[{"delta":"bad"},{"char":"a","metric":"trust","delta":2}],"memory":null,"relationships":{}}""")
        assertEquals(listOf("门开了。", "欢迎。"), scene.entries.map { it.text })
        assertEquals(listOf("进去", "稍等"), scene.choices.map { it.text })
        assertEquals("hall", scene.choices.last().next)
        assertEquals(1, scene.stateEffects.size)
        assertEquals(EntryKind.CHARACTER, scene.logEntries(listOf(CharacterData("a", "阿雨"))).last().kind)
    }

    @Test fun firstPersonActionsAndDialogueAreNotMistakenForModelPlanning() {
        val scene = AiDirector(ChatClient()).parseScene("""{"entries":[{"text":"我先构思场景"},{"speakerId":"a","text":"让我来告诉你。"}],"choices":[{"text":"我先问问他的来意"},{"text":"让我来开门"}]}""")
        assertEquals(listOf("让我来告诉你。"), scene.entries.map { it.text })
        assertEquals(listOf("我先问问他的来意", "让我来开门"), scene.choices.map { it.text })
    }

    @Test fun truncatedEnvelopeDoesNotCommitTheFirstNarration() {
        val scene = AiDirector(ChatClient()).parseScene("""{"entries":[{"text":"半截剧情"},{"speakerId":"a","text":"没结束"}],"choices":[""")
        assertTrue(scene.entries.isEmpty()); assertTrue(scene.text.isEmpty())
    }

    @Test fun missingSuggestionsRetryOnceWithOriginalPlayerContext() = runBlocking {
        val requests = mutableListOf<String>()
        val ok = client { request -> requests += request; if (requests.size == 1) """{"text":"门开了。"}""" else good }
        try {
            val scene = AiDirector(ChatClient(ok)).directorTurn(profile, Story("s", "雨城"), emptyList(), SessionState("s"), "在窗边坐下")
            assertEquals(2, requests.size); assertEquals(2, scene.choices.size); assertEquals(2, scene.entries.size)
            assertTrue(requests.all { it.contains("在窗边坐下") })
            assertFalse(requests.last().contains("独立思考"))
            assertFalse(requests.last().contains("\"thinking\""))
            assertEquals("独立思考\n独立思考", scene.reasoning)
        } finally { ok.close() }
    }

    @Test fun explicitEndingDoesNotRequestMoreSuggestions() = runBlocking {
        val calls = AtomicInteger()
        val ok = client { calls.incrementAndGet(); """{"text":"故事结束。","choices":[],"ended":true}""" }
        try {
            val scene = AiDirector(ChatClient(ok)).directorTurn(profile, Story("s", "雨城"), emptyList(), SessionState("s"), "结束故事")
            assertTrue(scene.ended); assertEquals(1, calls.get())
        } finally { ok.close() }
    }

    @Test fun failedRecommendationRestoresChoicesAndRetryRecordsPlayerOnlyOnce() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val calls = AtomicInteger()
        val requests = java.util.Collections.synchronizedList(mutableListOf<String>())
        val ok = client { request -> requests += request; if (calls.incrementAndGet() <= 2) """{"entries":[{"text":"坏的半截"}""" else good }
        try {
            val library = LocalLibrary(temp.newFolder())
            library.upsertProvider(profile)
            library.upsertCharacter(CharacterData("a", "阿雨"))
            library.upsertCharacter(CharacterData("b", "小晴"))
            // The player plays 阿雨; the AI voices everyone else.
            library.upsertStory(Story("s", "雨城", mode = StoryMode.AI_DIRECTOR, characterIds = listOf("a", "b")))
            val state = SessionState("s", playerCharacterId = "a", playerCharacterName = "阿雨",
                pendingAiChoices = listOf(ChoiceData("在窗边坐下")), aiAwaitingChoice = true,
                history = listOf(LogEntry(text = "雨还在下。")))
            library.upsertSave(SaveSlot("save", "save", 1, 1, state))
            val vm = PlayViewModel("s", "save", library, AiDirector(ChatClient(ok))) { "p" }
            vm.dmSend("在窗边坐下"); vm.dmSend("在窗边坐下")
            withContext(Dispatchers.Default) { withTimeout(5000) { while (vm.ui.value.stage == PlayStage.AI_WORKING) delay(10) } }
            assertEquals(2, calls.get())
            assertEquals(listOf("在窗边坐下"), vm.ui.value.pendingAiChoices.map { it.text })
            assertEquals(state.pendingAiChoices, vm.ui.value.session!!.pendingAiChoices)
            assertEquals(0, vm.ui.value.session!!.history.count { it.kind == EntryKind.CHOICE })
            vm.dmSend("在窗边坐下")
            withContext(Dispatchers.Default) { withTimeout(5000) { while (vm.ui.value.stage == PlayStage.AI_WORKING) delay(10) } }
            assertEquals(3, calls.get()); assertEquals(2, vm.ui.value.pendingAiChoices.size)
            assertEquals(1, vm.ui.value.session!!.history.count { it.kind == EntryKind.CHOICE })
            assertEquals(listOf("雨还在下。", "在窗边坐下", "灯亮了。", "请坐。"), vm.ui.value.session!!.history.filter { it.kind != EntryKind.ERROR }.map { it.text })
            val user = AppJson.parseToJsonElement(requests.last()).toString()
            assertTrue(user.contains("在窗边坐下")); assertFalse(user.contains("坏的半截"))
        } finally { ok.close(); Dispatchers.resetMain() }
    }
}
