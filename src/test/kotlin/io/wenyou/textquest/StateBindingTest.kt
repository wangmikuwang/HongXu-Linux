package io.wenyou.textquest

import io.wenyou.textquest.data.ai.AiDirector
import io.wenyou.textquest.data.ai.metricMeaning
import io.wenyou.textquest.data.llm.ChatClient
import io.wenyou.textquest.data.model.*
import io.wenyou.textquest.data.repo.LocalLibrary
import io.wenyou.textquest.ui.vm.PlayStage
import io.wenyou.textquest.ui.vm.PlayViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Collections

/** Character states shape the next lines, and the lines move the states gradually. */
@OptIn(ExperimentalCoroutinesApi::class)
class StateBindingTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun valuesCarryTheirMeaning() {
        assertEquals("反感、冷淡", metricMeaning("affection", 10.0))
        assertEquals("友善、普通", metricMeaning("affection", 40.0))
        assertEquals("深深依恋", metricMeaning("affection", 100.0))
        assertEquals("重伤或重病，行动严重受限", metricMeaning("health", 5.0))
        assertEquals("", metricMeaning("unknown", 50.0))
    }

    @Test fun promptsBindStatesAndAiChangesAreGradual() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val prompts = Collections.synchronizedList(mutableListOf<String>())
        val reply = """{"entries":[{"speakerId":"a","text":"……谢谢你。"}],"choices":[{"text":"继续"}],"memory":"",""" +
            """"state":[{"char":"a","metric":"affection","delta":60,"reason":"被打动"}]}"""
        val ok = OkHttpClient.Builder().addInterceptor { chain ->
            val body = AppJson.parseToJsonElement(Buffer().also { chain.request().body!!.writeTo(it) }.readUtf8()).jsonObject
            prompts += body.getValue("messages").jsonArray.last().jsonObject.getValue("content").jsonPrimitive.content
            val answer = buildJsonObject { put("choices", buildJsonArray { add(buildJsonObject {
                put("message", buildJsonObject { put("content", reply) }) }) }) }.toString()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(answer.toResponseBody()).build()
        }.build()
        try {
            val library = LocalLibrary(temp.newFolder())
            library.upsertProvider(ApiProfile("p", "mock", baseUrl = "http://localhost/v1", model = "mock"))
            library.upsertCharacter(CharacterData("a", "阿雨"))
            library.upsertStory(Story("s", "雨城", mode = StoryMode.AI_DIRECTOR, characterIds = listOf("a")))
            val state = SessionState("s", history = listOf(LogEntry(text = "雨还在下。")),
                characterStates = mapOf("a" to CharacterState(metrics = mapOf("affection" to 10.0), flags = setOf("刚吵过架"),
                    lastChangeReason = AiDirector.MANUAL_STATE_REASON)))
            library.upsertSave(SaveSlot("save", "save", 1, 1, state))
            val vm = PlayViewModel("s", "save", library, AiDirector(ChatClient(ok))) { "p" }
            suspend fun turn(text: String) {
                vm.dmSend(text)
                withContext(Dispatchers.Default) { withTimeout(5_000) { while (vm.ui.value.stage != PlayStage.DM_INPUT) delay(10) } }
            }
            turn("我把伞递给她")
            val first = prompts.single()
            assertTrue(first, first.contains("❤️好感度10（反感、冷淡）"))
            assertTrue(first.contains("【角色状态约束】"))
            assertTrue(first.contains("单项每轮变化不超过 ±${AiDirector.MAX_STATE_STEP}"))
            assertTrue(first.contains("玩家手动设定了「阿雨」的状态"))
            // The model asked for +60; one turn moves the value by the largest allowed step.
            val after = vm.ui.value.session!!.characterStates.getValue("a")
            assertEquals(30.0, after.metrics.getValue("affection"), 0.0)
            assertEquals("被打动", after.lastChangeReason)
            turn("我们一起走吧")
            assertFalse("The manual note ends once the story has moved the state", prompts.last().contains("玩家手动设定"))
            assertTrue(prompts.last().contains("❤️好感度30（疏离、客气）"))
            assertEquals(50.0, vm.ui.value.session!!.characterStates.getValue("a").metrics.getValue("affection"), 0.0)
        } finally { ok.dispatcher.executorService.shutdownNow(); Dispatchers.resetMain() }
    }

    @Test fun consoleEditsAreMarkedAsManual() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val library = LocalLibrary(temp.newFolder())
            library.upsertCharacter(CharacterData("a", "阿雨"))
            library.upsertStory(Story("s", "雨城", mode = StoryMode.AI_DIRECTOR, characterIds = listOf("a")))
            library.upsertSave(SaveSlot("save", "save", 1, 1, SessionState("s")))
            val vm = PlayViewModel("s", "save", library, AiDirector(ChatClient())) { null }
            vm.setCharacterState("a", CharacterState(metrics = mapOf("trust" to 150.0, "bogus" to 3.0), flags = setOf(" 已和好 ", "")))
            val st = vm.ui.value.session!!.characterStates.getValue("a")
            assertEquals(mapOf("trust" to 100.0), st.metrics)
            assertEquals(setOf("已和好"), st.flags)
            assertEquals(AiDirector.MANUAL_STATE_REASON, st.lastChangeReason)
        } finally { Dispatchers.resetMain() }
    }
}
