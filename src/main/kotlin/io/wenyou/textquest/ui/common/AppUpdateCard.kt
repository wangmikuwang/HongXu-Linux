package io.wenyou.textquest.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.wenyou.textquest.platform.inFlatpak
import io.wenyou.textquest.ui.vm.AppUpdateState
import io.wenyou.textquest.ui.common.AppText as Text

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppUpdateCard(state: AppUpdateState, onCheck: () -> Unit, onOpenRelease: () -> Unit) {
    TonalCard {
        Text("应用更新", style = MaterialTheme.typography.titleMedium)
        Text(if (inFlatpak) "本应用由 Flathub 提供，新版本会通过系统的软件中心（或 flatpak update）更新，本地资料会保留。"
            else "启动时自动检查官方更新；有新版本时，在发布页面下载 Linux 安装包覆盖安装即可，本地资料会保留。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (inFlatpak) return@TonalCard
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        if (state.message.isNotBlank()) Text(state.message, style = MaterialTheme.typography.bodyMedium)
        state.release?.let { release ->
            Text("新版 ${release.version}")
            if (release.notes.isNotBlank()) RawText(release.notes, style = MaterialTheme.typography.bodySmall, maxLines = 6, overflow = TextOverflow.Ellipsis)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = onCheck, enabled = !state.busy) { Text("检查更新") }
            AppTextButton(onClick = onOpenRelease) { Text("打开发布页面") }
        }
    }
}
