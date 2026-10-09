package io.wenyou.textquest

import io.wenyou.textquest.data.ai.AiDirector
import io.wenyou.textquest.data.llm.ChatClient
import io.wenyou.textquest.data.model.*
import io.wenyou.textquest.data.repo.LocalLibrary
import io.wenyou.textquest.ui.vm.PlayStage
import io.wenyou.textquest.ui.vm.PlayViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class StopAiTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun stoppingADirectorTurnPutsTheTurnBackAsItWas() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val reached = CountDownLatch(1)
        // A provider that never answers until the call is cancelled.
        val ok = OkHttpClient.Builder().addInterceptor { chain ->
            reached.countDown()
            try { Thread.sleep(30_000) } catch (_: InterruptedException) { }
            if (chain.call().isCanceled()) throw IOException("Canceled")
            error("the call should have been cancelled")
        }.build()
        try {
            val library = LocalLibrary(temp.newFolder())
            library.upsertProvider(ApiProfile("p", "mock", baseUrl = "http://localhost/v1", model = "mock"))
            library.upsertCharacter(CharacterData("b", "小晴"))
            library.upsertStory(Story("s", "雨城", mode = StoryMode.AI_DIRECTOR, characterIds = listOf("b")))
            val state = SessionState("s", pendingAiChoices = listOf(ChoiceData("在窗边坐下")), aiAwaitingChoice = true,
                history = listOf(LogEntry(text = "雨还在下。")))
            library.upsertSave(SaveSlot("save", "save", 1, 1, state))
            val vm = PlayViewModel("s", "save", library, AiDirector(ChatClient(ok))) { "p" }
            withContext(Dispatchers.Default) { withTimeout(5000) { while (vm.ui.value.stage != PlayStage.DM_INPUT) delay(10) } }
            vm.dmSend("我推门进去")
            assertEquals(PlayStage.AI_WORKING, vm.ui.value.stage)
            withContext(Dispatchers.IO) { reached.await(5, TimeUnit.SECONDS) }
            vm.stopAi()
            assertEquals(PlayStage.DM_INPUT, vm.ui.value.stage)
            assertEquals(listOf("雨还在下。"), vm.ui.value.session!!.history.map { it.text })
            assertEquals(listOf("在窗边坐下"), vm.ui.value.pendingAiChoices.map { it.text })
        } finally { ok.dispatcher.executorService.shutdownNow(); Dispatchers.resetMain() }
    }
}
