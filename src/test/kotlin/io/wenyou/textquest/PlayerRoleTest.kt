package io.wenyou.textquest

import io.wenyou.textquest.data.ai.AiDirector
import io.wenyou.textquest.data.llm.ChatClient
import io.wenyou.textquest.data.model.*
import io.wenyou.textquest.data.repo.LocalLibrary
import io.wenyou.textquest.ui.vm.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerRoleTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun selectionGatesStartAndIdentitySurvivesSaveLoadAndRestart() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val library = LocalLibrary(temp.newFolder())
            library.upsertCharacter(CharacterData("a", "阿雨"))
            library.upsertCharacter(CharacterData("other", "局外人"))
            library.upsertStory(Story("s", "雨城", characterIds = listOf("a"), nodes = mapOf(
                "start" to StoryNode("start", text = "门响了", onEnter = listOf(Effect(EffectType.ADD_VAR, "entered", 1.0)),
                    choices = listOf(ChoiceData("推门", "@self")))
            )))
            val director = AiDirector(ChatClient())
            val vm = PlayViewModel("s", "new", library, director) { null }
            assertEquals(PlayStage.ROLE_SELECT, vm.ui.value.stage)
            assertEquals(listOf("a"), vm.ui.value.characters.map { it.id })
            assertTrue(vm.ui.value.session!!.history.isEmpty())
            assertNull(vm.ui.value.session!!.variables["entered"])
            vm.saveNow(); vm.chooseAuthored(0); vm.dmSend("提前行动")
            assertTrue(library.saves.value.isEmpty())
            vm.selectPlayerCharacter("other")
            assertEquals(PlayStage.ROLE_SELECT, vm.ui.value.stage)
            vm.selectPlayerCharacter("a")
            vm.selectPlayerCharacter("") // Duplicate confirmation cannot reset or replay the opening.
            assertEquals(1.0, vm.ui.value.session!!.variables["entered"]!!, 0.0)
            assertEquals("a", vm.ui.value.session!!.playerCharacterId)
            assertEquals(listOf("门响了"), vm.ui.value.session!!.history.map { it.text })
            vm.chooseAuthored(0)
            val choice = vm.ui.value.session!!.history.last()
            assertEquals("a", choice.speakerId); assertEquals("阿雨", choice.speaker)
            vm.saveNow()
            val saved = withContext(Dispatchers.Default) { withTimeout(5_000) { library.saves.first { it.isNotEmpty() }.single() } }
            // Verify serialisation separately, then use the persisted save in the original library.
            val restored = AppJson.decodeFromString(SessionState.serializer(), AppJson.encodeToString(SessionState.serializer(), saved.state))
            assertEquals(saved.state, restored)
            val loaded = PlayViewModel("s", saved.id, library, director) { null }
            assertEquals(PlayStage.AUTHORED, loaded.ui.value.stage)
            assertEquals(saved.state, loaded.ui.value.session)
            loaded.restart()
            assertEquals(PlayStage.ROLE_SELECT, loaded.ui.value.stage)
            assertTrue(loaded.ui.value.session!!.history.isEmpty())
            loaded.selectPlayerCharacter("")
            assertEquals("", loaded.ui.value.session!!.playerCharacterId)
            loaded.chooseAuthored(0)
            assertEquals("你", loaded.ui.value.session!!.history.last().speaker)
            assertEquals("", AppJson.decodeFromString(SessionState.serializer(), """{"storyId":"s"}""").playerCharacterId)
        } finally { Dispatchers.resetMain() }
    }
}
