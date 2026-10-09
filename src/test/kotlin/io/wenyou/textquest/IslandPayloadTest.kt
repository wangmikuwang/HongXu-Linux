package io.wenyou.textquest

import io.wenyou.textquest.data.llm.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class IslandPayloadTest {
    @Test fun payloadIsBoundedAndCompletionMatchesTheRequest() {
        for (phase in listOf("等待服务响应", "正在思考", "正在生成内容", "private\"\n".repeat(5000))) {
            val text = islandPayload(phase, Long.MAX_VALUE, Int.MAX_VALUE)
            assertTrue(text.toByteArray(Charsets.UTF_8).size <= 3072)
            assertFalse(text.contains("private"))
            val payload = Json.parseToJsonElement(text).jsonObject.getValue("param_v2").jsonObject
            assertFalse(payload.getValue("filterWhenNoPermission").jsonPrimitive.boolean)
            val island = payload.getValue("param_island").jsonObject
            assertTrue(island.getValue("bigIslandArea").jsonObject.getValue("imageTextInfoLeft").jsonObject
                .getValue("textInfo").jsonObject.getValue("title").jsonPrimitive.content.length <= 4)
            assertEquals("99:59", island.getValue("bigIslandArea").jsonObject.getValue("sameWidthDigitInfo").jsonObject.getValue("digit").jsonPrimitive.content)
        }
        val usage = UsageTracker()
        usage.start(GenerationProgress("new", "Service", "Model", System.nanoTime()))
        usage.finish("new", UsageRecord("Service", "Model", 0, 0, "完成", TokenUsage()))
        assertTrue(usage.active.value.isEmpty())
        assertEquals("new", usage.records.value.last().requestId)
    }
}
