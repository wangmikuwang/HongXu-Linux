package io.wenyou.textquest.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.wenyou.textquest.platform.openAsset
import io.wenyou.textquest.platform.platformContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.wenyou.textquest.BuildConfig
import io.wenyou.textquest.ui.common.AppText as Text

/** One version's entry from the bundled changelog. */
data class ReleaseNotes(val version: String, val items: List<String>)

/** Reads `## x.y.z` sections and their `- ` items, dropping Markdown emphasis and links. */
internal fun parseChangelog(text: String): List<ReleaseNotes> {
    val notes = mutableListOf<ReleaseNotes>()
    var version: String? = null
    val items = mutableListOf<String>()
    fun flush() { version?.let { notes += ReleaseNotes(it, items.toList()) }; items.clear() }
    for (raw in text.lineSequence()) {
        val line = raw.trim()
        when {
            line.startsWith("## ") -> { flush(); version = line.removePrefix("## ").trim() }
            version != null && line.startsWith("- ") -> items += line.removePrefix("- ")
                .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1").replace("**", "").replace("`", "").trim()
        }
    }
    flush()
    return notes
}

/** "What's new": this version expanded, a few earlier ones behind a toggle. Text comes from the bundled changelog. */
@Composable
fun WhatsNewCard(earlier: Int = 3) {
    val context = platformContext()
    val notes = remember { runCatching { openAsset(context, WHATS_NEW_ASSET).bufferedReader().use { parseChangelog(it.readText()) } }.getOrDefault(emptyList()) }
    if (notes.isEmpty()) return
    var showEarlier by rememberSaveable { mutableStateOf(false) }
    TonalCard(modifier = Modifier.testTag("whats-new")) {
        Text("更新内容", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        val shown = if (showEarlier) notes.take(1 + earlier) else notes.take(1)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            shown.forEach { release ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val current = release.version == BuildConfig.VERSION_NAME.substringBefore('-')
                    Text("v${release.version}" + if (current) "（当前版本）" else "", style = MaterialTheme.typography.labelLarge,
                        color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    release.items.forEach { item ->
                        Row {
                            RawText("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(6.dp))
                            RawText(item, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (notes.size > 1) AppTextButton(onClick = { showEarlier = !showEarlier }) {
            Text(if (showEarlier) "收起" else "查看之前的版本")
        }
    }
}

internal const val WHATS_NEW_ASSET = "whats_new.md"
