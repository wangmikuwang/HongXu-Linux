package io.wenyou.textquest.ui.common

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wenyou.textquest.BuildConfig
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.repo.ShareCode
import io.wenyou.textquest.platform.appLabel
import io.wenyou.textquest.platform.platformContext
import io.wenyou.textquest.platform.shareFile
import io.wenyou.textquest.platform.shareText
import io.wenyou.textquest.platform.showMessage
import kotlinx.coroutines.launch
import io.wenyou.textquest.ui.common.AppText as Text

/** Sender side: a link message, a .wenyou file, or the bare code, each recognised by every import path. */
object ShareActions {
    private const val LINK_BASE = "https://wangmikuwang.github.io/fdroid/s/"

    fun link(code: String) = "$LINK_BASE?a=${BuildConfig.SHARE_ORIGIN}#$code"

    private fun message(context: Context, kind: String, title: String, code: String): String {
        val app = appLabel(context)
        return "我在「$app」分享了$kind《$title》，点开链接即可导入：\n${link(code)}\n（也可以复制这段文字，再打开「$app」自动识别）"
    }

    fun sendLink(context: Context, kind: String, title: String, code: String) =
        shareText(context, title, message(context, kind, title, code), "分享「$title」")


    /** Writes the code to a small .wenyou file; the recipient opens it with this app to import. */
    fun sendFile(context: Context, title: String, code: String) {
        val name = title.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").trim('_').take(40).ifBlank { "share" }
        shareFile(context, "$name.wenyou", code + "\n", "发送「$title」")
    }
}

/** Previews whatever the share inbox holds and imports only after confirmation, without overwriting. */
@Composable
fun SharedImportHost(container: WenYouApp.AppContainer) {
    val code by container.shareInbox.pending.collectAsStateWithLifecycle()
    val pending = code ?: return
    val context = platformContext()
    val scope = rememberCoroutineScope()
    val stories by container.library.stories.collectAsStateWithLifecycle()
    val characters by container.library.characters.collectAsStateWithLifecycle()
    val foreign = remember(pending) { ShareCode.foreign(pending) }
    val bundle = remember(pending) { if (foreign) null else ShareCode.decode(pending) }
    var importing by remember(pending) { mutableStateOf(false) }
    val dismiss = container.shareInbox::dismiss

    if (bundle == null || (bundle.stories.isEmpty() && bundle.characters.isEmpty())) {
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("无法导入") },
            text = { Text(if (foreign) "这段内容不是由本应用分享的，无法导入。" else "分享内容不完整或已损坏，请让对方重新分享。") },
            confirmButton = { AppTextButton(onClick = dismiss) { Text("知道了") } }
        )
        return
    }
    val storyIds = stories.mapTo(mutableSetOf()) { it.id }
    val characterIds = characters.mapTo(mutableSetOf()) { it.id }
    val newStories = bundle.stories.count { it.id !in storyIds }
    val newCharacters = bundle.characters.count { it.id !in characterIds }
    AlertDialog(
        onDismissRequest = { if (!importing) dismiss() },
        title = { Text("导入分享内容") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bundle.stories.forEach { s ->
                    RawText("${s.coverEmoji} ${s.title}" + if (s.id in storyIds) "（已有）" else "", style = MaterialTheme.typography.bodyLarge)
                }
                bundle.characters.forEach { c ->
                    RawText("${c.emoji} ${c.name}" + if (c.id in characterIds) "（已有）" else "", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (newStories + newCharacters == 0) "这些内容都已经在资料库里了。"
                    else "将新增 ${listOfNotNull(newStories.takeIf { it > 0 }?.let { "$it 部剧情" }, newCharacters.takeIf { it > 0 }?.let { "$it 位角色" }).joinToString("、")}；已有内容会跳过，不会被覆盖。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            AppTextButton(enabled = !importing && newStories + newCharacters > 0, onClick = {
                importing = true
                scope.launch {
                    val message = runCatching { container.library.importShared(bundle) }
                        .fold({ "导入成功：新增 ${it.added} 条内容" }, { "导入失败：${it.message}" })
                    showMessage(context, message)
                    dismiss()
                }
            }) { Text("导入") }
        },
        dismissButton = { AppTextButton(enabled = !importing, onClick = dismiss) { Text("取消") } }
    )
}
