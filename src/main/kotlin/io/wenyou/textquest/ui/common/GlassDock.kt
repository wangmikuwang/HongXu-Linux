package io.wenyou.textquest.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import io.wenyou.textquest.ui.common.AppIcon as Icon
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.wenyou.textquest.platform.glassShader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.wenyou.textquest.ui.theme.*
import kotlin.math.roundToInt

data class DockItem(val label: String, val icon: ImageVector)

/**
 * Whole-slot lens and outline glyphs follow the reference dock's visual proportions.
 * While a finger is down the pill lifts into a clear lens that magnifies and refracts the icons beneath it
 * and may overhang the bar; it settles back into the pill on release. Android 12 and older keep the flat pill.
 */
@Composable
fun GlassDock(items: List<DockItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val prefs = LocalAppearance.current
    val density = LocalDensity.current
    var widthPx by remember { mutableFloatStateOf(1f) }
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    var pressed by remember { mutableStateOf(false) }
    val latestSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selected)
    val position = LocalDockPosition.current ?: animateFloatAsState(selected.toFloat(), AppMotion.selection(), label = "dock-lens")
    val dragging by remember { derivedStateOf { dragPosition != null } }
    val stretch = animateFloatAsState(if (!dragging) 1f else 1.04f, AppMotion.selection(), label = "dock-stretch")
    val shader = remember { glassShader(DOCK_LENS_SHADER) }
    val lift = animateFloatAsState(if (shader != null && (pressed || dragging)) 1f else 0f, AppMotion.selection(), label = "dock-lift")
    // The tab under a dragging finger takes the selected colour before the drag is released.
    val highlighted = dragPosition?.roundToInt() ?: selected
    val icons = rememberGraphicsLayer()
    val lens = rememberGraphicsLayer()
    val glass = MaterialTheme.colorScheme.surface
    BoxWithConstraints(modifier.fillMaxWidth().heightIn(min = 64.dp).onSizeChanged { widthPx = it.width.toFloat() }
        .pointerInput(Unit) {
            awaitEachGesture { awaitFirstDown(requireUnconsumed = false); pressed = true; waitForUpOrCancellation(); pressed = false }
        }
        .pointerInput(items.size) {
            detectHorizontalDragGestures(
                onDragStart = { offset -> dragPosition = (offset.x / widthPx * items.size - .5f).coerceIn(0f, items.lastIndex.toFloat()) },
                onHorizontalDrag = { change, amount -> change.consume(); dragPosition = ((dragPosition ?: currentSelected.toFloat()) + amount / widthPx * items.size).coerceIn(0f, items.lastIndex.toFloat()) },
                onDragCancel = { dragPosition = null },
                onDragEnd = { dragPosition?.roundToInt()?.let(latestSelect); dragPosition = null }
            )
        }
        .drawWithContent {
            drawContent()
            val t = lift.value
            if (shader == null || t < .01f) return@drawWithContent
            val slot = size.width / items.size
            val center = Offset(((dragPosition ?: position.value) + .5f) * slot, size.height / 2)
            val width = mix(slot - 8.dp.toPx(), slot * 1.32f, t)
            val height = mix(56.dp.toPx(), size.height + 20.dp.toPx(), t)
            val topLeft = center - Offset(width / 2, height / 2)
            val corner = CornerRadius(height / 2)
            shader.uniform("size", width, height)
            shader.uniform("radius", height / 2)
            shader.uniform("mag", 1f + .22f * t)
            shader.uniform("density", density.density)
            lens.renderEffect = shader.effect("content")
            lens.alpha = t
            lens.record(IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1))) {
                translate(-topLeft.x, -topLeft.y) { drawLayer(icons) }
            }
            // The glass body hides the icons beneath; the lens then shows them magnified, with a lit rim and faint dispersion.
            drawRoundRect(glass.copy(alpha = .97f * t), topLeft, Size(width, height), corner)
            translate(topLeft.x, topLeft.y) { drawLayer(lens) }
            drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .7f * t), Color.White.copy(alpha = .08f * t), Color.White.copy(alpha = .35f * t)),
                startY = topLeft.y, endY = topLeft.y + height), topLeft, Size(width, height), corner, style = Stroke(1.2.dp.toPx()))
            val inset = 2.dp.toPx()
            drawRoundRect(Brush.sweepGradient(listOf(Color(0x66FFD54F), Color(0x4D4FC3F7), Color(0x00FFFFFF), Color(0x4DF48FB1), Color(0x66FFD54F)), center),
                topLeft + Offset(inset, inset), Size(width - 2 * inset, height - 2 * inset), CornerRadius(height / 2 - inset),
                style = Stroke(2.dp.toPx()), alpha = t)
        }) {
        val slot = maxWidth / items.size
        val indicatorColor = distributedAccent(selected + 2, MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
        val indicatorFill = animateColorAsState(indicatorColor, AppMotion.fade(), label = "dock-color")
        Box(Modifier.padding(4.dp).width((slot - 8.dp).coerceAtLeast(1.dp)).height(56.dp)
            .graphicsLayer { translationX = (dragPosition ?: position.value) * with(density) { slot.toPx() }; scaleX = stretch.value; scaleY = 1f / stretch.value; alpha = 1f - lift.value }
            .drawBehind { drawRoundRect(indicatorFill.value, cornerRadius = CornerRadius(size.height / 2)) })
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).drawWithContent {
            icons.record { this@drawWithContent.drawContent() }
            drawLayer(icons)
        }) {
            items.forEachIndexed { index, item ->
                val accent = distributedAccent(index + 2, MaterialTheme.colorScheme.readableAccent())
                val tint = if (index == highlighted && LocalAccentPalette.current.isNotEmpty()) accentForeground(indicatorColor) else if (index == highlighted) {
                    if (contrastRatio(accent, MaterialTheme.colorScheme.surface) >= 4.5) accent else MaterialTheme.colorScheme.onSurface
                } else MaterialTheme.colorScheme.onSurface
                val interaction = remember { MutableInteractionSource() }
                Column(Modifier.weight(1f).heightIn(min = 64.dp)
                    .selectable(index == selected, role = Role.Tab, interactionSource = interaction, indication = null, onClick = { latestSelect(index) })
                    .pressMotion(interaction)
                    .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (prefs.dockLabels != "text") Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(26.dp))
                    if (prefs.dockLabels != "icons") {
                        if (prefs.dockLabels == "both") Spacer(Modifier.height(2.dp))
                        Text(item.label, color = tint, fontSize = ((if (prefs.dockLabels == "text") 15f else 11f) * density.fontScale.coerceAtMost(1.15f) / density.fontScale).sp,
                            lineHeight = (14f * density.fontScale.coerceAtMost(1.15f) / density.fontScale).sp,
                            fontWeight = if (index == highlighted) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun mix(from: Float, to: Float, t: Float) = from + (to - from) * t

// Original AGSL: the centre magnifies, the rim bends samples outward like a thick lens edge and splits red from blue.
internal const val DOCK_LENS_SHADER = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float mag;
uniform float density;
half4 main(float2 p) {
    float2 c = size * 0.5;
    float2 q = abs(p - c) - (c - radius);
    float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    if (d > 0.0) return half4(0.0);
    float rim = 1.0 - smoothstep(0.0, 16.0 * density, -d);
    rim *= rim;
    float2 dir = p - c;
    float2 uv = c + dir / mag + dir * rim * 0.2;
    float2 n = dir / max(length(dir), 0.001);
    float spread = rim * 2.5 * density;
    half4 g = content.eval(uv);
    half4 r = content.eval(uv + n * spread);
    half4 b = content.eval(uv - n * spread);
    return half4(r.r, g.g, b.b, max(g.a, max(r.a, b.a)));
}
"""
