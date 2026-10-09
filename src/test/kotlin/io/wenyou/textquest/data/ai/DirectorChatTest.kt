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
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectorChatTest {
    private val story = Story("s", "雨城", characterIds = listOf("c"))
    private val cast = listOf(CharacterData("c", "阿雨"))
    private val profile = ApiProfile("p", "p", baseUrl = "https://example.com/v1", model = "m")

    /** Replies with [content] and records each request's user message. */
    private fun client(content: String, sent: MutableList<String>) = ChatClient(OkHttpClient.Builder().addInterceptor { chain ->
        val body = AppJson.parseToJsonElement(Buffer().also { chain.request().body!!.writeTo(it) }.readUtf8()).jsonObject
        sent += body.getValue("messages").jsonArray.last().jsonObject.getValue("content").jsonPrimitive.content
        val reply = buildJsonObject { putJsonArray("choices") { addJsonObject { putJsonObject("message") { put("content", content) } } } }
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(reply.toString().toResponseBody()).build()
    }.build())

    @Test fun replyAndMemoAreParsedAndEarlierExchangesAreSent() = runBlocking {
        val sent = mutableListOf<String>()
        val earlier = listOf(DirectorMessage(true, "陆晚知道什么？"), DirectorMessage(false, "她比你想的多。"))
        val reply = AiDirector(client("""{"reply":"好，下一幕让她先找到糖纸。","note":"下一幕由陆晚先发现糖纸线索"}""", sent))
            .directorChat(profile, story, cast, SessionState("s"), earlier, "让她先发现线索")
        assertEquals(DirectorMessage(false, "好，下一幕让她先找到糖纸。", "下一幕由陆晚先发现糖纸线索"), reply)
        assertTrue(sent.single().contains("玩家：陆晚知道什么？") && sent.single().contains("导演：她比你想的多。"))
        assertTrue(sent.single().contains("【玩家现在对导演说】\n让她先发现线索"))
    }

    @Test fun plainTextRepliesStillWorkWithoutAMemo() = runBlocking {
        val reply = AiDirector(client("我打算让雨停在第三幕。", mutableListOf())).directorChat(profile, story, cast, SessionState("s"), emptyList(), "雨什么时候停？")
        assertEquals(DirectorMessage(false, "我打算让雨停在第三幕。", ""), reply)
    }

    @Test fun aRenamedReplyFieldIsStillShown() = runBlocking {
        val reply = AiDirector(client("""{"回复":"雨会停在第三幕。","note":""}""", mutableListOf())).directorChat(profile, story, cast, SessionState("s"), emptyList(), "雨什么时候停？")
        assertEquals(DirectorMessage(false, "雨会停在第三幕。", ""), reply)
    }

    @Test fun memosTravelWithEveryLaterTurn() = runBlocking {
        val sent = mutableListOf<String>()
        val scene = """{"entries":[{"speakerId":"","text":"雨还在下。"}],"choices":[{"text":"继续"}]}"""
        AiDirector(client(scene, sent)).directorTurn(profile, story, cast, SessionState("s", directorNotes = listOf("下一幕由陆晚先发现糖纸线索")), "继续")
        assertTrue(sent.single().contains("【导演备忘：玩家在场外与你约定的剧情方向，在底层基调范围内执行】\n- 下一幕由陆晚先发现糖纸线索"))
    }
}
