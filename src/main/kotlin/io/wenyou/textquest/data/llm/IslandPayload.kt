package io.wenyou.textquest.data.llm

import kotlinx.serialization.json.*

/** Official OS2/OS3 text template; business must match the Xiaomi-approved scenario. */
internal fun islandPayload(phase: String, seconds: Long, count: Int): String = buildJsonObject {
    putJsonObject("param_v2") {
        put("protocol", 1)
        put("business", "ai_generation")
        put("updatable", true)
        put("reopen", "reopen")
        put("islandFirstFloat", false)
        put("enableFloat", false)
        put("filterWhenNoPermission", false)
        put("timeout", 3)
        val badge = when (phase) {
            "正在思考" -> "正在思考"
            "正在生成内容" -> "正在生成"
            else -> "等待响应"
        }
        val elapsed = "${seconds.coerceAtLeast(0)}秒"
        put("ticker", badge)
        put("aodTitle", "AI生成 · $badge")
        putJsonObject("baseInfo") {
            put("type", 2); put("title", badge)
            put("content", "${count.coerceAtLeast(1)}项AI请求 · 已用$elapsed")
        }
        putJsonObject("picInfo") { put("type", 1); put("pic", "miui.focus.pic_generation") }
        putJsonObject("param_island") {
            put("islandProperty", 1); put("islandTimeout", 180)
            putJsonObject("bigIslandArea") {
                putJsonObject("imageTextInfoLeft") {
                    put("type", 1)
                    putJsonObject("textInfo") { put("title", badge) }
                }
                putJsonObject("sameWidthDigitInfo") {
                    put("digit", "%d:%02d".format(java.util.Locale.ROOT, seconds.coerceIn(0, 5999) / 60, seconds.coerceIn(0, 5999) % 60))
                }
            }
            putJsonObject("smallIslandArea") {
                putJsonObject("picInfo") { put("type", 1); put("pic", "miui.focus.pic_generation") }
            }
        }
    }
}.toString()
