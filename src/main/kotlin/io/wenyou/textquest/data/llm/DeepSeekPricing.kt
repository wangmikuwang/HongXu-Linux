package io.wenyou.textquest.data.llm

import io.wenyou.textquest.data.model.ApiProfile
import io.wenyou.textquest.data.model.ProviderKind
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.DayOfWeek

internal data class CostEstimate(val lower: Double?, val upper: Double? = null, val note: String = "手动单价")

/** Official snapshot: https://api-docs.deepseek.com/zh-cn/quick_start/pricing/ (2026-10-03). */
internal fun TokenUsage.estimate(profile: ApiProfile, started: Long, finished: Long): CostEstimate {
    if (listOf(profile.inputPrice, profile.outputPrice, profile.cachedPrice, profile.cacheWritePrice).any { it != null })
        return CostEstimate(cost(profile))
    val uri = runCatching { URI(profile.baseUrl) }.getOrNull()
    if (profile.kind != ProviderKind.OPENAI_COMPAT || uri?.scheme != "https" || uri.host != "api.deepseek.com" ||
        uri.port !in listOf(-1, 443) || uri.userInfo != null || cacheWrite != 0L || uncached == null)
        return CostEstimate(null, note = "缺少官方价格或缓存用量；可填写手动单价")
    val pro = when (profile.model) {
        "deepseek-flash", "deepseek-v4-flash", "deepseek-v4-flash-vision-exp" -> false
        "deepseek-v4-pro" -> true
        else -> return CostEstimate(null, note = "该模型未收录官方价格，请填写手动单价")
    }
    val prices = when (profile.priceCurrency) {
        "CNY" -> if (pro) listOf(4.5, 13.5, 0.15) else listOf(1.0, 4.0, 0.02)
        "USD" -> if (pro) listOf(0.66, 1.98, 0.022) else listOf(0.15, 0.6, 0.003)
        else -> return CostEstimate(null, note = "官方价格仅支持 CNY / USD")
    }
    val low = cost(profile.copy(inputPrice = prices[0], outputPrice = prices[1], cachedPrice = prices[2]))
        ?: return CostEstimate(null, note = "用量不完整或缓存用量不一致")
    // A request crossing a tariff boundary is a range; server billing time is not known.
    val periods = mutableSetOf<Boolean?>()
    if (finished < started || finished - started > 86_400_000L) periods.add(null)
    else {
        var t = started
        while (t < finished) { periods.add(deepSeekPeak(t)); t += 60_000 }
        periods.add(deepSeekPeak(finished))
    }
    val note = "DeepSeek 官方价（2026-10-03）"
    return when {
        periods == setOf(false) -> CostEstimate(low, note = "$note · 闲时")
        periods == setOf(true) -> CostEstimate(low * 2, note = "$note · 高峰")
        else -> CostEstimate(low, low * 2, "$note · 时段不确定，显示范围")
    }
}

internal fun deepSeekPeak(time: Long): Boolean? {
    val local = Instant.ofEpochMilli(time).atZone(ZoneId.of("Asia/Shanghai"))
    if (local.dayOfWeek == DayOfWeek.SATURDAY || local.dayOfWeek == DayOfWeek.SUNDAY) return false
    val minute = local.hour * 60 + local.minute
    if (minute !in 540 until 720 && minute !in 840 until 1080) return false
    if (local.year != 2026) return null // Future public holidays are not assumed.
    val day = local.monthValue * 100 + local.dayOfMonth
    val holiday = day in 101..103 || day in 215..223 || day in 404..406 || day in 501..505 ||
        day in 619..621 || day in 925..927 || day in 1001..1007
    return !holiday
}
