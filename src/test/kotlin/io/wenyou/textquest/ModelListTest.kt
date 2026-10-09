package io.wenyou.textquest

import io.wenyou.textquest.data.llm.ChatClient
import io.wenyou.textquest.data.model.ApiProfile
import io.wenyou.textquest.data.model.ProviderKind
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ModelListTest {
    @Test fun allProtocolsKeepModelIdsAndHttpFailuresAreReported() = runBlocking {
        var status = 200
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val json = if (chain.request().url.encodedPath.startsWith("/gemini/"))
                """{"models":[{"name":"models/gemini-test"},{"name":""}]}"""
            else """{"data":[{"id":"chat-test"},{"id":""}]}"""
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(status).message("test").body(json.toResponseBody()).build()
        }.build()
        try {
            val chat = ChatClient(client)
            for (kind in ProviderKind.entries) {
                val profile = ApiProfile(id = "test", name = "test", kind = kind,
                    baseUrl = "https://example.test/${if (kind == ProviderKind.GEMINI) "gemini" else "v1"}", model = "test")
                assertEquals(listOf(if (kind == ProviderKind.GEMINI) "gemini-test" else "chat-test"), chat.listModels(profile))
                status = 403
                try { chat.listModels(profile); fail("HTTP errors must be visible") }
                catch (e: Exception) { assertTrue(e.message.orEmpty().contains("HTTP 403")) }
                status = 200
            }
        } finally { client.dispatcher.executorService.shutdownNow(); client.connectionPool.evictAll() }
    }

    @Test fun leavingTheEditorCancelsAnUnansweredModelRequest() = runBlocking {
        val server = ServerSocket(0)
        val accepted = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val release = CountDownLatch(1)
        val serving = launch(Dispatchers.IO) {
            server.accept().use { accepted.countDown(); release.await(5, TimeUnit.SECONDS) }
        }
        val client = OkHttpClient.Builder().eventListener(object : EventListener() {
            override fun canceled(call: Call) { cancelled.countDown() }
        }).build()
        try {
            val profile = ApiProfile(id = "test", name = "test", baseUrl = "http://127.0.0.1:${server.localPort}/v1", model = "test")
            val request = launch { ChatClient(client).listModels(profile) }
            assertTrue(withContext(Dispatchers.IO) { accepted.await(3, TimeUnit.SECONDS) })
            withTimeout(1_000) { request.cancelAndJoin() }
            assertTrue("Network call must also stop", cancelled.await(1, TimeUnit.SECONDS))
            assertTrue(request.isCancelled)
        } finally {
            release.countDown(); serving.join(); server.close()
            client.dispatcher.executorService.shutdownNow(); client.connectionPool.evictAll()
        }
    }
}
