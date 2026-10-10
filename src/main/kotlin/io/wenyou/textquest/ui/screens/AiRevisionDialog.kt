package io.wenyou.textquest.ui.screens

import io.wenyou.textquest.ui.common.AppTextButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import io.wenyou.textquest.ui.common.AppIcon as Icon
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.ai.AiCreator
import io.wenyou.textquest.data.ai.AiDirector
import io.wenyou.textquest.data.model.AppBundle
import io.wenyou.textquest.data.model.CharacterMetrics
import io.wenyou.textquest.ui.common.AppField
import io.wenyou.textquest.ui.common.TonalCard
import io.wenyou.textquest.ui.common.UsagePanel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun AiRevisionDialog(container: WenYouApp.AppContainer, original: AppBundle,
    onApply: (AppBundle) -> Unit, onDismiss: () -> Unit) {
    var instruction by rememberSaveable { mutableStateOf("") }
    var preview by remember { mutableStateOf<AppBundle?>(null) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val close = { job?.cancel(); onDismiss() }
    AlertDialog(onDismissRequest = close, title = { Text("一句话修改") }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("修改当前未保存的内容；先查看预览，再应用到表单，最后按保存。")
            AppField(instruction, { if (!busy) { instruction = it.take(2000); preview = null } }, "修改要求", modifier = Modifier.testTag("revision-instruction"), minLines = 2, maxLines = 4)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error.isNotBlank()) io.wenyou.textquest.ui.common.AppText(error, color = MaterialTheme.colorScheme.error)
            preview?.let { CreationPreview(it) }
            UsagePanel(container.chatClient.usage)
        }
    }, confirmButton = {
        Button(modifier = Modifier.testTag("revision-confirm"), enabled = !busy && instruction.isNotBlank(), onClick = {
            preview?.let { onApply(it); close(); return@Button }
            val prefs = container.settings.state.value
            val profiles = container.library.providers.value
            val profile = profiles.firstOrNull { it.id == prefs.defaultProviderId } ?: profiles.firstOrNull()
            if (profile == null) { error = "请先配置 AI 服务"; return@Button }
            busy = true; error = ""
            job = scope.launch {
                try { preview = AiCreator(container.chatClient).revise(profile, instruction, original, prefs.adultContent) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = AiDirector.errorMessage(e) }
                finally { busy = false }
            }
        }) { io.wenyou.textquest.ui.common.AppText(if (preview == null) "生成修改预览" else "应用到表单") }
    }, dismissButton = { AppTextButton(onClick = close) { io.wenyou.textquest.ui.common.AppText(if (busy) "取消生成" else "关闭") } })
}

@Composable
internal fun CreationPreview(bundle: AppBundle) {
    bundle.stories.forEach { s -> TonalCard {
        Text("${s.coverEmoji} ${s.title}", style = MaterialTheme.typography.titleMedium)
        io.wenyou.textquest.ui.common.RawText(s.subtitle); Text("题材：${s.genre} · ${s.mode.label}")
        Text("世界观：${s.ai.worldSummary}")
        Text("叙事风格：${s.ai.tone}"); Text("导演要求：${s.ai.directorExtra}")
        Text("初始变量：${s.initialVariables.entries.joinToString { "${it.key}=${it.value}" }}")
        Text("初始标记：${s.initialFlags.joinToString()}")
        Text("内容：${if (s.lgbt) "LGBT " else ""}${if (s.adult) "18+" else "全年龄"}")
        s.nodes.forEach { (id, node) ->
            Text("${if (id == s.startNodeId) "开场 · " else ""}${node.title.ifBlank { "场景" }}", style = MaterialTheme.typography.labelLarge)
            io.wenyou.textquest.ui.common.RawText(node.text)
            if (node.prompt.isNotBlank()) Text("生成要求：${node.prompt}")
            node.choices.forEach { Text("· ${it.text}${if (it.hint.isBlank()) "" else "（${it.hint}）"}") }
        }
    } }
    bundle.characters.forEach { c -> TonalCard {
        Text("${c.emoji} ${c.name}", style = MaterialTheme.typography.titleMedium)
        io.wenyou.textquest.ui.common.RawText(c.tagline); Text("性取向：${c.orientation.label}")
        Text("性格：${c.personality}"); Text("背景：${c.background}")
        Text("说话方式：${c.speechStyle}"); Text("台词：${c.exampleDialogue}"); Text("招呼：${c.greeting}")
        Text("附加人设：${c.extraPrompt}")
        Text("初始外观：${c.initial.description}")
        io.wenyou.textquest.ui.common.RawText(c.initial.metrics.entries.joinToString(" · ") { "${CharacterMetrics.label(it.key)} ${it.value}" })
        Text("初始标记：${c.initial.flags.joinToString()}")
        Text("内容：${if (c.lgbt) "LGBT " else ""}${if (c.adult) "18+" else "全年龄"}")
    } }
}
