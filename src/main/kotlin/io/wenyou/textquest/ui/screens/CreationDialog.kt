package io.wenyou.textquest.ui.screens

import io.wenyou.textquest.ui.common.AppTextButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.ai.CreationKind
import io.wenyou.textquest.ui.R
import io.wenyou.textquest.ui.common.UsagePanel
import io.wenyou.textquest.ui.common.AppDropdown
import io.wenyou.textquest.ui.common.AppField
import io.wenyou.textquest.ui.common.TonalCard
import io.wenyou.textquest.ui.vm.CreationViewModel
import io.wenyou.textquest.ui.vm.Vms

@Composable
fun CreationDialog(container: WenYouApp.AppContainer, nav: NavHostController, onDismiss: () -> Unit) {
    val vm: CreationViewModel = viewModel(factory = Vms.factory { CreationViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    LaunchedEffect(Unit) { if (ui.idea.isBlank() && ui.draft == null) vm.setKind(CreationKind.STORY) }
    val profiles by container.library.providers.collectAsStateWithLifecycle()
    val prefs by container.settings.state.collectAsStateWithLifecycle()
    val profile = profiles.firstOrNull { it.id == prefs.defaultProviderId } ?: profiles.firstOrNull()
    LaunchedEffect(ui.saved) {
        if (ui.saved) {
            val draft = ui.draft ?: return@LaunchedEffect
            val route = draft.stories.firstOrNull()?.let { R.storyEdit(it.id) } ?: R.charEdit(draft.characters.first().id)
            vm.consumeSaved()
            onDismiss()
            nav.navigate(route)
        }
    }
    val close = { vm.cancel(); onDismiss() }
    AlertDialog(
        onDismissRequest = { if (!ui.busy) close() },
        title = { Text("AI 一句话创建") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppDropdown("创建内容", CreationKind.entries.map { it.label to it }, ui.kind, vm::setKind, enabled = !ui.busy)
                if (ui.kind == CreationKind.STORY) AppDropdown("剧情玩法", io.wenyou.textquest.data.model.StoryMode.entries.map { it.label to it },
                    ui.mode, vm::setMode, enabled = !ui.busy, modifier = Modifier.testTag("creation-mode"))
                if (!ui.busy) {
                    AppField(ui.idea, vm::setIdea, "描述你的创意", modifier = Modifier.testTag("creation-idea"), minLines = 2, maxLines = 4,
                        placeholder = "例如：一位失忆侦探与能听见旧物记忆的少女，在雨城寻找失踪的人",
                        supporting = "${ui.idea.length}/2000 字")
                } else {
                    io.wenyou.textquest.ui.common.RawText(ui.idea, style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    io.wenyou.textquest.ui.common.AppText(if (ui.saving) "正在保存…" else if (ui.draft != null) "正在修改草稿…" else if (ui.kind == CreationKind.STORY) (if (ui.mode == io.wenyou.textquest.data.model.StoryMode.SCRIPT) "正在创作分支剧本与人设…" else "正在创作剧情与人设…") else "正在创作人物设定…")
                }
                if (ui.busy || ui.draft != null || ui.error.isNotBlank()) UsagePanel(container.chatClient.usage)
                io.wenyou.textquest.ui.common.AppText(if (profile == null) "还没有配置 AI 服务" else "使用 AI 服务：${profile.name}", style = MaterialTheme.typography.bodySmall)
                if (ui.kind == CreationKind.STORY) Text(if (ui.mode == io.wenyou.textquest.data.model.StoryMode.SCRIPT) "生成可离线游玩的分支剧本：多个场景、选项与结局，以及人物与变量；保存前可用一句话继续修改。" else "生成开场、世界观与人物，游玩时由 AI 导演即兴续写；保存前可用一句话继续修改。", style = MaterialTheme.typography.bodySmall)
                if (profile == null) AppTextButton(onClick = { close(); nav.navigate(R.PROVIDERS) }) { Text("配置 AI 服务") }
                if (ui.error.isNotBlank()) io.wenyou.textquest.ui.common.AppText(ui.error, color = MaterialTheme.colorScheme.error)
                ui.draft?.let { draft ->
                    CreationPreview(draft)
                    if (!ui.busy) {
                        AppField(ui.revision, vm::setRevision, "一句话修改", modifier = Modifier.testTag("creation-revision"), minLines = 2, maxLines = 4, supporting = "修改当前草稿，未提及内容保留")
                        AppTextButton(onClick = vm::revise, enabled = ui.revision.isNotBlank() && profile != null) { Text("修改草稿") }
                        AppTextButton(onClick = vm::generate) { Text("重新生成") }
                    }
                }
            }
        },
        confirmButton = {
            Button(modifier = Modifier.testTag("creation-confirm"), onClick = { focus.clearFocus(); if (ui.draft == null) vm.generate() else vm.save() }, enabled = !ui.busy && (ui.draft != null || profile != null) && ui.idea.isNotBlank()) {
                io.wenyou.textquest.ui.common.AppText(if (ui.draft == null) "开始创建" else "保存并编辑")
            }
        },
        dismissButton = {
            AppTextButton(onClick = close, enabled = !ui.saving) { io.wenyou.textquest.ui.common.AppText(if (ui.busy) "取消生成" else "关闭") }
        }
    )
}
