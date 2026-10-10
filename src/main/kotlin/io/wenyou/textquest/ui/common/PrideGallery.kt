package io.wenyou.textquest.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import io.wenyou.textquest.ui.common.AppIcon as Icon
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.wenyou.textquest.ui.theme.PrideTheme

val LocalPrideGalleryOpen = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
internal fun PrideFlag(theme: PrideTheme, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(5f / 3f).clip(RoundedCornerShape(4.dp))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
        .semantics { contentDescription = "${theme.label}旗帜" }) {
        if (theme == PrideTheme.INTERSEX) {
            drawRect(theme.colors[0])
            drawCircle(theme.colors[1], size.height * 0.28f, style = Stroke(size.height * 0.075f))
        } else {
            val weights = theme.stripeWeights
            var y = 0f
            theme.colors.forEachIndexed { index, color ->
                val height = size.height * weights[index] / weights.sum()
                drawRect(color, Offset(0f, y), Size(size.width, height + 0.5f))
                y += height
            }
            if (theme == PrideTheme.DEMISEXUAL) drawPath(Path().apply {
                moveTo(0f, 0f); lineTo(size.width * 0.35f, size.height / 2f); lineTo(0f, size.height); close()
            }, Color.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrideGallery(unlocked: Boolean, enabled: Boolean, showLgbt: Boolean,
    onUnlock: () -> Unit, onEnabledChange: (Boolean) -> Unit, onShowLgbtChange: (Boolean) -> Unit,
    onSelect: (PrideTheme) -> Unit, onDismiss: () -> Unit, selectedTheme: PrideTheme? = null) {
    var nextIndex by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("旗帜墙") }, navigationIcon = {
            IconButton(onClick = onDismiss) { Icon(AppIcons.ArrowBack, "返回") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (unlocked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("骄傲主题选项", Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = onEnabledChange, modifier = Modifier.testTag("pride-enabled"))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("显示 LGBT（LGBTQ+）内容", Modifier.weight(1f))
                    Switch(checked = showLgbt, onCheckedChange = onShowLgbtChange, modifier = Modifier.testTag("pride-content"))
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LazyVerticalGrid(columns = GridCells.Adaptive(152.dp * LocalDensity.current.fontScale), modifier = Modifier.weight(1f).testTag("pride-flags"),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(PrideTheme.entries, key = { _, theme -> theme.name }) { index, theme ->
                        Surface(onClick = {
                            if (unlocked) onSelect(theme)
                            else {
                                nextIndex = if (index == nextIndex) nextIndex + 1 else if (index == 0) 1 else 0
                                if (nextIndex == PrideTheme.entries.size) onUnlock()
                            }
                        }, enabled = !unlocked || enabled, modifier = Modifier.testTag("pride-flag-${theme.name}")
                            .semantics { selected = enabled && selectedTheme == theme },
                            border = if (enabled && selectedTheme == theme) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
                            shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PrideFlag(theme, Modifier.fillMaxWidth())
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    io.wenyou.textquest.ui.common.AppText(theme.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                    if (enabled && selectedTheme == theme) Icon(AppIcons.Check, "已选中", Modifier.size(20.dp))
                                }
                                io.wenyou.textquest.ui.common.AppText(theme.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
