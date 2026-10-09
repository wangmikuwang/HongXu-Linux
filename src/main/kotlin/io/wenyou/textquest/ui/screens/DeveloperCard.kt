package io.wenyou.textquest.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.ui.common.TonalCard
import io.wenyou.textquest.ui.common.AppText as Text

/** Shown only after developer mode is unlocked. */
@Composable
fun DeveloperCard(container: WenYouApp.AppContainer) {
    val dev by container.devMode.state.collectAsStateWithLifecycle()
    if (!dev.unlocked) return
    TonalCard {
        Text("开发者模式", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("与导演直接对话", style = MaterialTheme.typography.labelLarge)
                Text("开启后，对话页「⋯」菜单多出「与导演对话」：跳出剧情直接询问导演的构思，或提出后续剧情要求。要求会记为导演备忘，随存档保存；全程遵守底层基调。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Switch(dev.directorChat, container.devMode::setDirectorChat, Modifier.testTag("dev-director-chat"))
        }
    }
}
