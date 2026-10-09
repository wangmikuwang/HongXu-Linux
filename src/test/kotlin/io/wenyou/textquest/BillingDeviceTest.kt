package io.wenyou.textquest

import io.wenyou.textquest.data.llm.*
import io.wenyou.textquest.data.model.*
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class BillingDeviceTest {
    private val profile = ApiProfile("test", "DeepSeek", baseUrl = "https://api.deepseek.com/v1", model = "deepseek-flash")
    private fun time(value: String) = ZonedDateTime.parse(value + "+08:00[Asia/Shanghai]").toInstant().toEpochMilli()
    private val tokens = TokenUsage(1000, 200, 400, uncached = 600, reasoning = 100)

    @Test fun officialPricingUsesCacheAndHolidayWithoutCountingReasoningTwice() {
        val holiday = time("2026-10-02T10:00:00")
        assertEquals(0.001408, tokens.estimate(profile, holiday, holiday).lower!!, 1e-12)
        val peak = time("2026-10-08T10:00:00")
        assertEquals(0.002816, tokens.estimate(profile, peak, peak).lower!!, 1e-12)
        val usd = tokens.estimate(profile.copy(priceCurrency = "USD"), holiday, holiday)
        assertEquals(0.0002112, usd.lower!!, 1e-12)
    }
    @Test fun tariffBoundaryAndUnknownHolidayProduceRanges() {
        val estimate = tokens.estimate(profile, time("2026-10-08T11:59:00"), time("2026-10-08T12:01:00"))
        assertEquals(0.001408, estimate.lower!!, 1e-12)
        assertEquals(0.002816, estimate.upper!!, 1e-12)
        assertNull(deepSeekPeak(time("2027-01-04T10:00:00")))
        assertEquals(false, deepSeekPeak(time("2026-10-08T18:00:00")))
    }
    @Test fun manualPricesOverrideAndUnknownProvidersAreNotGuessed() {
        val t = time("2026-10-08T10:00:00")
        assertEquals(0.0022, tokens.estimate(profile.copy(inputPrice = 2.0, outputPrice = 4.0, cachedPrice = 0.5), t, t).lower!!, 1e-12)
        assertNull(tokens.estimate(profile.copy(baseUrl = "https://api.deepseek.com.example.org/v1"), t, t).lower)
        assertNull(tokens.estimate(profile.copy(model = "deepseek-chat"), t, t).lower)
        assertNull(tokens.copy(uncached = 700).estimate(profile, t, t).lower)
        assertNull(tokens.copy(reasoning = 300).estimate(profile, t, t).lower)
        assertNull(tokens.copy(input = null).estimate(profile, t, t).lower)
    }
    @Test fun usageTotalsAreReconstructedAndRepeatedFramesAreNotAdded() {
        val json = AppJson.parseToJsonElement("""{"usage":{"prompt_cache_hit_tokens":400,"prompt_cache_miss_tokens":600,"completion_tokens":200,"completion_tokens_details":{"reasoning_tokens":100}}}""")
        val parsed = TokenUsage().read(ProviderKind.OPENAI_COMPAT, json)
        assertEquals(tokens, parsed)
        assertEquals(parsed, parsed.read(ProviderKind.OPENAI_COMPAT, json))
        assertEquals(parsed, parsed.read(ProviderKind.OPENAI_COMPAT, AppJson.parseToJsonElement("""{"usage":null}""")))
    }
    @Test fun progressUpdatesAreBoundedButPhaseChangesAreImmediate() {
        var now = 1_000_000_000L
        val tracker = UsageTracker(nanoTime = { now })
        tracker.start(GenerationProgress("a", "s", "m", now))
        tracker.progress("a", "生成中", 1)
        repeat(50) { now += 1_000_000; tracker.progress("a", "生成中", it + 2) }
        assertEquals(1, tracker.active.value["a"]!!.characters)
        tracker.progress("a", "保存中", 100)
        assertEquals(100, tracker.active.value["a"]!!.characters)
        now += 100_000_000
        tracker.progress("a", "保存中", 120)
        assertEquals(120, tracker.active.value["a"]!!.characters)
        tracker.finish("a", UsageRecord("s", "m", 0, 0, "完成", tokens))
        assertTrue(tracker.active.value.isEmpty())
    }
}
