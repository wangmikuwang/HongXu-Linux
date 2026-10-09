package io.wenyou.textquest

import android.content.Context
import io.wenyou.textquest.data.ai.AiDirector
import io.wenyou.textquest.data.llm.ChatClient
import io.wenyou.textquest.data.llm.UsageTracker
import io.wenyou.textquest.data.model.AppBundle
import io.wenyou.textquest.data.model.AppJson
import io.wenyou.textquest.data.model.CharacterData
import io.wenyou.textquest.data.model.SexualOrientation
import io.wenyou.textquest.data.repo.DevMode
import io.wenyou.textquest.data.repo.LocalLibrary
import io.wenyou.textquest.data.repo.SettingsStore
import io.wenyou.textquest.data.repo.ShareInbox
import io.wenyou.textquest.data.sample.SampleData
import java.io.File
import java.io.InputStream

/** Same shape as the Android app's WenYouApp, so screens that use WenYouApp.AppContainer are shared unchanged. */
object WenYouApp {
/** 进程级手动依赖注入容器（避免引入 Hilt，保持工程轻量）。 */
class AppContainer(context: Context, suppliedClient: ChatClient? = null) {
    val library = LocalLibrary(context)
    val settings = SettingsStore(context)
    val chatClient = suppliedClient ?: ChatClient(usage = UsageTracker(File(context.filesDir, "usage.json")))
    val director = AiDirector(chatClient)

    init {
        // Every AI request built by this client carries the player's baseline.
        chatClient.baseline = library::currentBaseline
    }
    val shareInbox = ShareInbox(context)
    val devMode = DevMode(context)

    /** Built-in samples and preset packs, merged at startup; [openAsset] reads a file from the app's bundled assets. */
    suspend fun seedLibrary(openAsset: (String) -> InputStream) {
        seedSamplesIfNeeded()
        applyPresetAssets(openAsset, listOf(
            "presets/wenyou-bare-presets.json",
            "presets/wenyou-bare2-presets.json"
        ))
        if (settings.state.value.showLgbt) {
            applyPresetAssets(openAsset, listOf(
                "presets/wenyou-romance-presets.json",
                "presets/wenyou-extended-presets.json",
                "presets/wenyou-diverse-presets.json"
            ), markLgbt = true)
            // 非异性向的 18+ 内容：受「显示 LGBT」与「成人内容」两个开关共同约束。
            applyPresetAssets(openAsset,
                listOf("presets/wenyou-adult-diverse-presets.json"),
                markLgbt = true, markAdult = true
            )
        }
        applyPresetAssets(openAsset,
            listOf("presets/wenyou-adult-presets.json", "presets/wenyou-adult-straight-presets.json"),
            markAdult = true
        )
        enrichBuiltinInitials(openAsset)
    }

    /**
     * 一次性为「已存在且未配置」的内置角色补齐 [CharacterData.initial] 与 [CharacterData.orientation]。
     * 非破坏性：只填充初始状态为空、性取向未标注的角色，不覆盖用户自定，
     * 也不会把用户删除的内置内容重新写回（仅针对当前仍存在的 id）。
     */
    private suspend fun enrichBuiltinInitials(openAsset: (String) -> InputStream) {
        val doneInitial = settings.presetEnrichDone
        val doneOrient = settings.presetOrientDone
        if (doneInitial && doneOrient) return
        try {
            val names = listOf(
                "presets/wenyou-bare-presets.json",
                "presets/wenyou-bare2-presets.json",
                "presets/wenyou-romance-presets.json",
                "presets/wenyou-extended-presets.json",
                "presets/wenyou-diverse-presets.json",
                "presets/wenyou-adult-presets.json",
                "presets/wenyou-adult-straight-presets.json"
            )
            val existing = library.characters.value.associateBy { it.id }
            var changedInitial = false
            var changedOrient = false
            val updates = mutableListOf<CharacterData>()
            for (name in names) {
                val text = openAsset(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val bundle = AppJson.decodeFromString(AppBundle.serializer(), text)
                for (c in bundle.characters) {
                    val cur = existing[c.id] ?: continue
                    var next = cur
                    if (!doneInitial && cur.initial.metrics.isEmpty() && c.initial.metrics.isNotEmpty()) {
                        next = next.copy(initial = c.initial)
                        changedInitial = true
                    }
                    if (!doneOrient && cur.orientation == SexualOrientation.UNKNOWN && c.orientation != SexualOrientation.UNKNOWN) {
                        next = next.copy(orientation = c.orientation)
                        changedOrient = true
                    }
                    if (next !== cur) updates += next
                }
            }
            for (cc in updates) library.upsertCharacter(cc)
            if (changedInitial) settings.presetEnrichDone = true
            if (changedOrient) settings.presetOrientDone = true
        } catch (_: Throwable) {
            // 补齐失败不阻塞主流程，下次启动重试
        }
    }

    /** 首次启动植入内置示例剧情与角色（雨夜咖啡馆 / 忆城）。 */
    private suspend fun seedSamplesIfNeeded() {
        if (settings.seeded) return
        try {
            val (chars, stories) = SampleData.all()
            for (c in chars) library.upsertCharacter(c)
            for (s in stories) library.upsertStory(s)
            settings.seeded = true
        } catch (_: Throwable) {
            // 示例植入失败不阻塞主流程，下次启动可重试（seeded 仍未置位）
        }
    }

    /**
     * 把 assets/presets/ 下的题材预设包（BL/伪百合/男娘/第四爱/娱乐圈ABO 等）
     * 自动并入资料库：按 id 去重、只补不覆盖。升级安装也能补到新版本新增的预设。
     */
    /** 逐个资源去重合并（按 id 只补不覆盖）。markLgbt/markAdult 用于打标签。 */
    private suspend fun applyPresetAssets(openAsset: (String) -> InputStream, presetFiles: List<String>, markLgbt: Boolean = false, markAdult: Boolean = false) {
        // 每个资源文件只成功合并一次并记录状态；否则每次启动都会全量重扫，
        // 既重复解析，也会把用户已删除的内置内容重新写回
        val already = settings.appliedPresetFiles()
        for (name in presetFiles) {
            if (name in already) continue
            try {
                applyPresetAsset(openAsset, name, markLgbt, markAdult)
                settings.markPresetFileApplied(name)
            } catch (_: Throwable) {
                // 单个资源失败不影响其它资源与主流程，下次启动重试
            }
        }
    }

    private suspend fun applyPresetAsset(openAsset: (String) -> InputStream, name: String, markLgbt: Boolean, markAdult: Boolean) {
        val text = openAsset(name)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val bundle = AppJson.decodeFromString(AppBundle.serializer(), text)
        val charIds = library.characters.value.mapTo(mutableSetOf()) { it.id }
        for (c in bundle.characters) {
            if (charIds.add(c.id)) {
                val cc = if (markLgbt || markAdult) c.copy(lgbt = c.lgbt || markLgbt, adult = c.adult || markAdult) else c
                library.upsertCharacter(cc)
            }
        }
        val storyIds = library.stories.value.mapTo(mutableSetOf()) { it.id }
        for (s in bundle.stories) {
            if (storyIds.add(s.id)) {
                val ss = if (markLgbt || markAdult) s.copy(lgbt = s.lgbt || markLgbt, adult = s.adult || markAdult) else s
                library.upsertStory(ss)
            }
        }
    }
}
}
