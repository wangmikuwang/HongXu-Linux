package io.wenyou.textquest.data.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class LlmErrorTest {
    @Test fun providerFailuresExplainTheCauseAndKeepTheProviderMessageShort() {
        val key = httpFailure(401, """{"error":{"message":"Authentication Fails, Your api key is invalid","type":"authentication_error"}}""").message!!
        assertTrue(key, key.startsWith("API Key 无效"))
        assertTrue(key, key.endsWith("服务商信息：Authentication Fails, Your api key is invalid"))
        assertTrue(httpFailure(402, """{"error":{"message":"Insufficient Balance"}}""").message!!.startsWith("账户余额不足"))
        assertTrue(httpFailure(429, "").message!!.startsWith("请求太频繁"))
        // HTML error pages from proxies are not echoed to the player.
        assertEquals("服务商暂时不可用：通常是服务繁忙，请稍后重试（HTTP 502）", httpFailure(502, "<html>Bad Gateway</html>").message)
        assertTrue(networkFailure(UnknownHostException("api.example.com")).message!!.startsWith("无法连接到服务器"))
    }
}
