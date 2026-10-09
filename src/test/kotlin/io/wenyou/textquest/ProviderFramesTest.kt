package io.wenyou.textquest

import io.wenyou.textquest.data.llm.ChatClient
import io.wenyou.textquest.data.llm.ChatOptions
import io.wenyou.textquest.data.model.ApiProfile
import io.wenyou.textquest.data.model.ProviderKind
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every text part reaches the story; thought parts and thinking blocks never do. */
class ProviderFramesTest {
    private fun ask(kind: ProviderKind, reply: String, temperature: Double = 0.8): Pair<String, String> = runBlocking {
        var sent = ""
        val ok = OkHttpClient.Builder().addInterceptor { chain ->
            sent = Buffer().also { chain.request().body!!.writeTo(it) }.readUtf8()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(reply.toResponseBody()).build()
        }.build()
        try {
            val profile = ApiProfile("p", "test", kind = kind, baseUrl = "http://localhost/v1", model = "m")
            ChatClient(ok).streamText(profile, "s", "u", ChatOptions(temperature, 64)).content to sent
        } finally { ok.dispatcher.executorService.shutdownNow() }
    }

    @Test fun geminiJoinsTextPartsAndSkipsThoughts() {
        val frame = """{"candidates":[{"content":{"parts":[{"text":"plan","thought":true},{"text":"雨"},{"text":"停了"}]}}]}"""
        assertEquals("雨停了", ask(ProviderKind.GEMINI, "data: $frame\n\n").first)
    }

    @Test fun claudeWholeReplySkipsThinkingBlocksAndClampsTemperature() {
        val reply = """{"content":[{"type":"thinking","thinking":"plan"},{"type":"text","text":"门开了"}]}"""
        val (text, sent) = ask(ProviderKind.ANTHROPIC, reply, temperature = 1.5)
        assertEquals("门开了", text)
        assertTrue(sent, sent.contains("\"temperature\":1.0"))
    }
}
