package io.wenyou.textquest

import io.wenyou.textquest.data.ai.*
import io.wenyou.textquest.data.llm.*
import io.wenyou.textquest.data.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class AiAuthoringTest {
    private val creator = AiCreator(ChatClient())
    private val full = """{"story":{"title":"雨城","subtitle":"失物调查","coverEmoji":"🌧️","genre":"悬疑","worldSummary":"雨城的旧物保留记忆","opening":"雨落窗边","tone":"简洁","directorExtra":"尊重玩家选择","colorIndex":11,"mode":"script","initialVariables":{"clues":0},"initialFlags":["rain"],"startNodeId":"start","nodes":{"start":{"title":"初遇","speakerId":"阿雨","text":"你来找谁？","choices":[{"text":"进门","next":"end","effects":[{"charId":"阿雨","type":"add_var","name":"trust","value":1}]}]},"end":{"kind":"ending","title":"告别","text":"天晴了"}}},"characters":[{"name":"阿雨","emoji":"☂️","colorIndex":10,"tagline":"记忆侦探","personality":"守约","speechStyle":"话少","background":"旧城居民","exampleDialogue":"雨会记得","greeting":"请进","extraPrompt":"坚持既有身份","bottomPrompt":"不伤害无辜","bottomRules":[{"name":"守约","content":"信守承诺"}],"initial":{"metrics":{"affection":20,"trust":30,"mood":50,"energy":80,"health":90,"fatigue":10,"arousal":0},"flags":["umbrella"],"description":"灰色风衣"}}]}"""


    @Test fun booleanFlagMapsAndLegacyVariableEffectsKeepTheirMeaning() {
        val raw = full.replace("\"initialFlags\":[\"rain\"]", "\"initialFlags\":{\"rain\":true,\"hidden\":false}")
            .replace("\"flags\":[\"umbrella\"]", "\"flags\":{\"umbrella\":true,\"revealed\":false}")
            .replace("\"type\":\"add_var\",\"name\":\"trust\"", "\"type\":\"variable\",\"target\":\"trust\"")
        val bundle = creator.parse(raw, CreationKind.STORY)
        assertEquals(setOf("rain"), bundle.stories.single().initialFlags)
        assertEquals(setOf("umbrella"), bundle.characters.single().initial.flags)
        val effect = bundle.stories.single().nodes.getValue("start").choices.single().effects.single()
        assertEquals(EffectType.ADD_VAR, effect.type)
        assertEquals("trust", effect.name)
        assertEquals(1.0, effect.value, 0.0)
        assertEquals(bundle.characters.single().id, effect.charId)
    }

    @Test fun initialVariableListsPreserveGlobalAndCharacterScope() {
        val raw = full.replace("\"initialVariables\":{\"clues\":0}",
            "\"initialVariables\":[{\"name\":\"clues\",\"value\":2},{\"name\":\"trust\",\"value\":45,\"charId\":\"阿雨\"}]")
        val bundle = creator.parse(raw, CreationKind.STORY)
        assertEquals(mapOf("clues" to 2.0), bundle.stories.single().initialVariables)
        assertEquals(45.0, bundle.characters.single().initial.metrics.getValue("trust"), 0.0)
        assertEquals(90.0, bundle.characters.single().initial.metrics.getValue("health"), 0.0)
        try { creator.parse(raw.replace("\"charId\":\"阿雨\"", "\"charId\":\"不存在\""), CreationKind.STORY); fail("Unknown initial actor must fail") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("创作格式")) }
    }

    @Test fun flagEffectsAndConditionsIgnoreBooleanOrNullValues() {
        // Seen from DeepSeek: {"type":"set_flag","name":"metInCafe","value":true} rejected whole creations.
        val raw = full.replace("\"effects\":[{\"charId\":\"阿雨\",\"type\":\"add_var\",\"name\":\"trust\",\"value\":1}]",
            "\"conditions\":[{\"type\":\"flag_true\",\"name\":\"rain\",\"value\":null}],\"effects\":[{\"type\":\"set_flag\",\"name\":\"met\",\"value\":true,\"charId\":\"\"}]")
        val choice = creator.parse(raw, CreationKind.STORY).stories.single().nodes.getValue("start").choices.single()
        assertEquals(EffectType.SET_FLAG, choice.effects.single().type)
        assertEquals("met", choice.effects.single().name)
        assertEquals("rain", choice.conditions.single().name)
        // A variable action still needs a real number.
        try { creator.parse(full.replace("\"value\":1}", "\"value\":true}"), CreationKind.STORY); fail("Non-numeric variable value must fail") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("创作格式")) }
    }

    @Test fun malformedFlagsAndUnknownActionTypesCannotSilentlyChangeTheStory() {
        for (raw in listOf(
            full.replace("\"initialFlags\":[\"rain\"]", "\"initialFlags\":{\"rain\":\"maybe\"}"),
            full.replace("\"type\":\"add_var\"", "\"type\":\"invented_action\"")
        )) {
            try { creator.parse(raw, CreationKind.STORY); fail("Invalid authoring format must fail") }
            catch (e: IllegalStateException) { assertTrue(e.message!!.contains("创作格式")) }
        }
    }

    @Test fun allEditorFieldsAndPlayableReferencesAreGenerated() {
        val draft = creator.parse(full, CreationKind.STORY)
        val s = draft.stories.single(); val c = draft.characters.single()
        assertEquals(StoryMode.SCRIPT, s.mode)
        assertEquals(11, s.colorIndex); assertEquals(10, c.colorIndex)
        assertEquals("尊重玩家选择", s.ai.directorExtra)
        assertEquals(0.0, s.initialVariables.getValue("clues"), 0.0)
        assertEquals(setOf("rain"), s.initialFlags)
        assertEquals(c.id, s.nodes.getValue("start").speakerId)
        assertEquals(c.id, s.nodes.getValue("start").choices.single().effects.single().charId)
        assertEquals("end", s.nodes.getValue("start").choices.single().next)
        assertEquals("灰色风衣", c.initial.description); assertEquals(7, c.initial.metrics.size)
        assertEquals("坚持既有身份", c.extraPrompt)
    }

    @Test fun revisionPreservesIdsAndUnmentionedFields() {
        val original = creator.parse(full, CreationKind.STORY)
        val c = original.characters.single()
        val patch = """{"story":{"title":"晴城","nodes":{"start":{"text":"请进"}}},"characters":[{"id":"${c.id}","initial":{"metrics":{"trust":60}}}]}"""
        val revised = creator.applyRevision(patch, original)
        assertEquals("雨城", original.stories.single().title)
        assertEquals("晴城", revised.stories.single().title)
        assertEquals(original.stories.single().id, revised.stories.single().id)
        assertEquals("end", revised.stories.single().nodes.getValue("start").choices.single().next)
        assertEquals("请进", revised.stories.single().nodes.getValue("start").text)
        assertEquals(c.copy(initial = c.initial.copy(metrics = c.initial.metrics + ("trust" to 60.0))), revised.characters.single())
    }

    @Test fun badPatchesCannotChangeIdentityOrBreakLinksOrContentPreferences() {
        val draft = creator.parse(full, CreationKind.STORY)
        for (patch in listOf("""{"story":{"id":"other"}}""", """{"story":{"startNodeId":"missing"}}""",
            """{"story":{"adult":true}}""", """{"characters":[{"id":"missing","name":"覆盖"}]}""",
            """{"providers":[]}""")) {
            assertThrows(IllegalArgumentException::class.java) { creator.applyRevision(patch, draft, false) }
        }
    }

    @Test fun authoringRecoversReasoningOnlyAndTruncatedDeepseekJson() = runBlocking {
        val original = creator.parse(full, CreationKind.STORY)
        for (streaming in listOf(false, true)) for (revision in listOf(false, true)) {
            var requests = 0
            val ok = OkHttpClient.Builder().addInterceptor { chain ->
                val buffer = okio.Buffer(); chain.request().body!!.writeTo(buffer)
                val body = AppJson.parseToJsonElement(buffer.readUtf8()).jsonObject
                requests++
                assertEquals(8192, body.getValue("max_tokens").jsonPrimitive.int)
                assertEquals("disabled", body.getValue("thinking").jsonObject.getValue("type").jsonPrimitive.content)
                val message = buildJsonObject {
                    if (requests == 1) {
                        put("reasoning_content", "构思耗尽输出额度")
                        put("content", if (revision) "{\"story\":{\"title\":\"晴" else "")
                    } else put("content", if (revision) """{"story":{"title":"晴城"}}""" else full)
                }
                val response = buildJsonObject { putJsonArray("choices") { addJsonObject {
                    put(if (streaming) "delta" else "message", message)
                    put("finish_reason", if (requests == 1) "length" else "stop")
                } } }.toString()
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body((if (streaming) "data: $response\n\ndata: [DONE]\n\n" else response).toResponseBody()).build()
            }.build()
            try {
                val author = AiCreator(ChatClient(ok))
                val profile = ApiProfile("p", "test", baseUrl = "https://api.deepseek.com", model = "deepseek-flash")
                val result = if (revision) author.revise(profile, "修改标题", original, false)
                    else author.generate(profile, "雨城侦探", CreationKind.STORY, false)
                assertEquals(2, requests)
                if (revision) assertEquals(original.copy(stories = original.stories.map { it.copy(title = "晴城") }), result)
                else assertEquals("雨城", result.stories.single().title)
                assertEquals("雨城", original.stories.single().title)
            } finally { ok.dispatcher.executorService.shutdownNow(); ok.connectionPool.evictAll() }
        }
    }

    @Test fun authoringRetryIsBoundedAndDoesNotApplyThinkingOrPartialJson() = runBlocking {
        val original = creator.parse(full, CreationKind.STORY)
        for (base in listOf("https://api.deepseek.com", "https://example.com")) {
            var requests = 0
            val ok = OkHttpClient.Builder().addInterceptor { chain ->
                requests++
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body("""{"choices":[{"message":{"reasoning_content":"只有构思","content":""}}]}""".toResponseBody()).build()
            }.build()
            try {
                try {
                    AiCreator(ChatClient(ok)).revise(ApiProfile("p", "test", baseUrl = base, model = "deepseek-flash"), "修改标题", original, false)
                    fail("Thinking must not become a revision")
                } catch (e: LlmException) { assertTrue(e.message!!.contains("只返回了思考")) }
                assertEquals(if (base == "https://api.deepseek.com") 2 else 1, requests)
                assertEquals("雨城", original.stories.single().title)
                assertThrows(IllegalArgumentException::class.java) {
                    creator.applyRevision("{\"story\":{\"title\":\"晴城\"}", original)
                }
            } finally { ok.dispatcher.executorService.shutdownNow(); ok.connectionPool.evictAll() }
        }
    }

    @Test fun completeAuthoringJsonNeedsNoRetry() = runBlocking {
        val original = creator.parse(full, CreationKind.STORY)
        var requests = 0
        val ok = OkHttpClient.Builder().addInterceptor { chain ->
            requests++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""{"choices":[{"message":{"reasoning_content":"构思","content":"{\"story\":{\"title\":\"晴城\"}}"}}]}""".toResponseBody()).build()
        }.build()
        try {
            val result = AiCreator(ChatClient(ok)).revise(ApiProfile("p", "test", baseUrl = "https://api.deepseek.com", model = "deepseek-flash"), "修改标题", original, false)
            assertEquals(1, requests)
            assertEquals("晴城", result.stories.single().title)
        } finally { ok.dispatcher.executorService.shutdownNow(); ok.connectionPool.evictAll() }
    }

    @Test fun reasoningOnlyDeepseekGetsOneNonThinkingRetryAndSeparateDialogue() = runBlocking {
        var requests = 0
        val ok = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = okio.Buffer(); chain.request().body!!.writeTo(buffer)
            val body = AppJson.parseToJsonElement(buffer.readUtf8()).jsonObject
            requests++
            val message = if (requests == 1) buildJsonObject { put("reasoning_content", "构思雨声与人物回应") } else {
                assertEquals("disabled", body.getValue("thinking").jsonObject.getValue("type").jsonPrimitive.content)
                assertTrue(body.getValue("max_tokens").jsonPrimitive.int >= 2048)
                buildJsonObject { put("content", """{"entries":[{"text":"窗外下雨"},{"speakerId":"c","text":"请进"}],"choices":[{"text":"走进去"}]}""") }
            }
            val response = buildJsonObject { putJsonArray("choices") { addJsonObject { put("message", message) } } }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(response.toString().toResponseBody()).build()
        }.build()
        try {
            val logs = AiDirector(ChatClient(ok)).directorTurn(ApiProfile("p", "test", baseUrl = "https://api.deepseek.com", model = "deepseek-flash"),
                Story("s", "test", ai = AiStorySettings(maxTokens = 900)), listOf(CharacterData("c", "阿雨")), SessionState("s"), "请回应").logEntries(listOf(CharacterData("c", "阿雨")))
            assertEquals(2, requests); assertEquals(EntryKind.NARRATION, logs[0].kind); assertEquals(EntryKind.CHARACTER, logs[1].kind)
            assertEquals("请进", logs[1].text); assertEquals("阿雨", logs[1].speaker)
            assertEquals("构思雨声与人物回应", logs[0].reasoning); assertTrue(logs[1].reasoning.isEmpty())
        } finally { ok.dispatcher.executorService.shutdownNow(); ok.connectionPool.evictAll() }
    }

    @Test fun otherReasoningOnlyRepliesNeverBecomeCompletedDialogue() = runBlocking {
        val ok = OkHttpClient.Builder().addInterceptor { chain -> Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
            .code(200).message("OK").body("""{"choices":[{"message":{"reasoning_content":"只有构思"}}]}""".toResponseBody()).build() }.build()
        try {
            try {
                AiDirector(ChatClient(ok)).directorTurn(ApiProfile("p", "test", baseUrl = "https://example.com", model = "test"),
                    Story("s", "test"), emptyList(), SessionState("s"), "继续")
                fail("Reasoning without prose must fail")
            } catch (e: LlmException) { assertTrue(e.message!!.contains("未返回剧情正文")) }
        } finally { ok.dispatcher.executorService.shutdownNow(); ok.connectionPool.evictAll() }
    }
}
