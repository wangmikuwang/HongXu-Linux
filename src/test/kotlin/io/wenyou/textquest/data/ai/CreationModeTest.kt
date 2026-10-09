package io.wenyou.textquest.data.ai

import io.wenyou.textquest.data.llm.ChatClient
import io.wenyou.textquest.data.model.StoryMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CreationModeTest {
    private val creator = AiCreator(ChatClient())
    private val cast = """"characters":[{"name":"阿雨","personality":"守约","background":"旧城居民"}]"""

    private fun script(nodes: String, mode: String = "script") =
        """{"story":{"title":"雨城","worldSummary":"雨城","opening":"雨落","mode":"$mode","startNodeId":"start","nodes":{$nodes}},$cast}"""

    private val playable = script(""""start":{"text":"门响了","choices":[{"text":"开门","next":"hall"},{"text":"不理","next":"quiet"}]},
        "hall":{"text":"走廊很暗","choices":[{"text":"往前","next":"end"}]},
        "quiet":{"kind":"ending","text":"雨停了"},"end":{"kind":"ending","text":"天亮了"}""")

    @Test fun thePickedModeWinsOverWhatTheModelWrote() {
        assertEquals(StoryMode.SCRIPT, creator.parse(playable.replace("\"mode\":\"script\"", "\"mode\":\"ai_dm\""), CreationKind.STORY, mode = StoryMode.SCRIPT).stories.single().mode)
        assertEquals(StoryMode.AI_DIRECTOR, creator.parse(playable, CreationKind.STORY, mode = StoryMode.AI_DIRECTOR).stories.single().mode)
    }

    @Test fun pickedScriptsMustPlayFromStartToAnEnding() {
        creator.parse(playable, CreationKind.STORY, mode = StoryMode.SCRIPT)
        val broken = mapOf(
            "dead end" to playable.replace(""""hall":{"text":"走廊很暗","choices":[{"text":"往前","next":"end"}]}""", """"hall":{"text":"走廊很暗"}"""),
            "no ending" to script(""""start":{"text":"a","choices":[{"text":"b","next":"b"}]},"b":{"text":"b","choices":[{"text":"a","next":"start"}]},"c":{"text":"c","choices":[{"text":"a","next":"start"}]}"""),
            "too short" to script(""""start":{"text":"a","choices":[{"text":"b","next":"end"}]},"end":{"kind":"ending","text":"b"}"""),
        )
        for ((name, raw) in broken) {
            try { creator.parse(raw, CreationKind.STORY, mode = StoryMode.SCRIPT); fail("$name must be rejected") }
            catch (e: IllegalArgumentException) { assertTrue(name, e.message!!.contains("分支剧本")) }
        }
        // AI director stories are not held to script structure.
        creator.parse(broken.getValue("too short"), CreationKind.STORY, mode = StoryMode.AI_DIRECTOR)
    }
}
