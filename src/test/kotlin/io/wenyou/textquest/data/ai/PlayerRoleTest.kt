package io.wenyou.textquest.data.ai

import io.wenyou.textquest.data.llm.ChatClient
import io.wenyou.textquest.data.model.ApiProfile
import io.wenyou.textquest.data.model.AppJson
import io.wenyou.textquest.data.model.CharacterData
import io.wenyou.textquest.data.model.SessionState
import io.wenyou.textquest.data.model.Story
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRoleTest {
    private val cast = listOf(CharacterData("gu", "顾言深"), CharacterData("lu", "陆晚"))
    private val story = Story("s", "雾都", characterIds = listOf("gu", "lu"))
    private val profile = ApiProfile("p", "p", baseUrl = "https://example.com/v1", model = "m")
    private val asLu = SessionState("s", playerCharacterId = "lu", playerCharacterName = "陆晚")

    @Test fun linesWrittenForThePlayersCharacterAreDropped() {
        val scene = AiScene(entries = listOf(AiEntry("", "雨还在下。"), AiEntry("gu", "你来了。"), AiEntry("lu", "我替玩家说的话"), AiEntry("陆晚", "按名字写的也算"), AiEntry("", "她点头。", speaker = "陆晚")))
        val logs = scene.logEntries(cast, playerId = "lu")
        assertEquals(listOf("雨还在下。", "你来了。"), logs.map { it.text })
        // A free identity keeps every line.
        assertEquals(5, scene.logEntries(cast).size)
    }

    @Test fun everyPromptSaysWhoThePlayerIs() = runBlocking {
        val sent = mutableListOf<String>()
        val ok = OkHttpClient.Builder().addInterceptor { chain ->
            val body = AppJson.parseToJsonElement(Buffer().also { chain.request().body!!.writeTo(it) }.readUtf8()).jsonObject
            sent += body.getValue("messages").jsonArray.joinToString("\n") { it.jsonObject.getValue("content").jsonPrimitive.content }
            val reply = buildJsonObject { putJsonArray("choices") { addJsonObject { putJsonObject("message") {
                put("content", """{"entries":[{"speakerId":"gu","text":"你来了。"}],"choices":[{"text":"走近"}],"reply":"好"}""") } } } }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(reply.toString().toResponseBody()).build()
        }.build()
        val director = AiDirector(ChatClient(ok))
        director.directorTurn(profile, story, cast, asLu, "我推门进去")
        director.directorChat(profile, story, cast, asLu, emptyList(), "接下来怎么安排？")
        for (prompt in sent) {
            assertTrue(prompt.contains("玩家扮演：陆晚"))
            assertTrue(prompt.contains("【由玩家扮演，只用于了解其身份与关系；不要替其说话、行动或做决定】\n· 角色名：陆晚"))
            assertFalse(prompt.contains("【由玩家扮演，只用于了解其身份与关系；不要替其说话、行动或做决定】\n· 角色名：顾言深"))
        }
        // The turn restates it last, after the story so far.
        assertTrue(sent[0].substringAfter("我推门进去").contains("【玩家身份】本轮由玩家扮演「陆晚」"))
    }
}
