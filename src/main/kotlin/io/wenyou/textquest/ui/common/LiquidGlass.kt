package io.wenyou.textquest.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import io.wenyou.textquest.platform.glassShader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

private class Backdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
    var scrolling by mutableStateOf(false)
}
private val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** Record only the content behind the controls; never record the glass itself. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GlassBackdrop(content: @Composable () -> Unit, controls: @Composable BoxScope.() -> Unit,
    footer: (@Composable () -> Unit)? = null) {
    val capturing = LocalScrollCaptureInProgress.current
    val layer = rememberGraphicsLayer()
    val backdrop = remember(layer) { Backdrop(layer) }
    val scope = rememberCoroutineScope()
    val scrollObserver = remember(backdrop, scope) {
        object : NestedScrollConnection {
            private var lastMovement = 0L
            private var settle: Job? = null
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed != Offset.Zero) {
                    lastMovement = System.nanoTime() / 1_000_000
                    backdrop.scrolling = true
                    if (settle?.isActive != true) settle = scope.launch {
                        while (true) {
                            val remaining = 120L - (System.nanoTime() / 1_000_000 - lastMovement)
                            if (remaining <= 0) break
                            delay(remaining)
                        }
                        backdrop.scrolling = false
                    }
                }
                return Offset.Zero
            }
        }
    }
    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Box(Modifier.fillMaxSize().nestedScroll(scrollObserver).onGloballyPositioned { backdrop.origin = it.positionInRoot() }
                .drawWithContent {
                    if (backdrop.scrolling) {
                        drawContent()
                    } else {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
                }) { content() }
            // Keep control measurements stable while excluding overlays from capture tiles.
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (capturing) 0f else 1f }) { controls() }
        }
        footer?.let { bottom ->
            Box(Modifier.graphicsLayer { alpha = if (capturing) 0f else 1f }) { bottom() }
        }
        }
    }
}

/**
 * Background sampling, blur and edge lensing are isolated from readable foreground text.
 * [clipContent] false keeps the glass in its shape but lets content (the dock lens) overhang it.
 */
@Composable
fun Modifier.liquidGlass(pill: Boolean = false, clipContent: Boolean = true): Modifier {
    val material = io.wenyou.textquest.ui.theme.LocalAppearance.current.glassMaterial
    val blurDp = when (material) { "clear" -> 3f; "frosted" -> 12f; else -> 7f }
    val glassAlpha = when (material) { "clear" -> .12f; "frosted" -> .45f; else -> .20f }
    val backdrop = LocalBackdrop.current
    val sample = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tint = MaterialTheme.colorScheme.surface
    val shader = remember { glassShader(GLASS_SHADER) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    var frozen by remember(backdrop, dark, tint) { mutableStateOf<ImageBitmap?>(null) }
    val densityKey = LocalDensity.current
    LaunchedEffect(backdrop, sample, bounds, dark, tint, densityKey) {
        if (backdrop != null) {
            snapshotFlow { backdrop.scrolling }.collectLatest { moving ->
                if (!moving) {
                    // Finish a draw before taking a small, already-filtered background snapshot.
                    withFrameNanos { }
                    withFrameNanos { }
                }
                if (sample.size.width > 0 && sample.size.height > 0) frozen = sample.toImageBitmap()
            }
        }
    }
    val shape = if (pill) RoundedCornerShape(50) else RoundedCornerShape(30.dp)
    return this.shadow(6.dp, shape, clip = false).then(if (clipContent) Modifier.clip(shape) else Modifier).onSizeChanged { bounds = it }.onGloballyPositioned { origin = it.positionInRoot() }
        .drawWithCache {
            // Cap background samples on high-resolution displays; text and controls stay full resolution.
            val sampleScale = minOf(0.5f, 540f / size.width.coerceAtLeast(1f))
            val sampleSize = IntSize(ceil(size.width * sampleScale).toInt().coerceAtLeast(1), ceil(size.height * sampleScale).toInt().coerceAtLeast(1))
            val corner = if (pill) minOf(size.width, size.height) / 2f else minOf(30.dp.toPx(), size.height / 2f)
            sample.clip = true
            if (backdrop != null) {
                val blur = blurDp.dp.toPx() * sampleScale
                sample.renderEffect = if (shader != null) {
                    shader.uniform("extent", sampleSize.width.toFloat(), sampleSize.height.toFloat())
                    shader.uniform("radius", corner * sampleScale)
                    shader.uniform("density", density * sampleScale)
                    shader.effect("backdrop", blur)
                } else BlurEffect(blur, blur, TileMode.Clamp)
            }
            val highlight = Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) 0.04f else 0.06f), Color.Transparent))
            val border = Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) 0.34f else 0.60f), Color.White.copy(alpha = 0.03f), Color.White.copy(alpha = 0.22f)))
            val radius = androidx.compose.ui.geometry.CornerRadius(corner)
            val stroke = Stroke(0.6.dp.toPx())
            val outline = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), radius)) }
            onDrawWithContent {
                clipPath(outline) {
                    if (backdrop != null && backdrop.scrolling) {
                        val image = frozen
                        if (image != null) drawImage(image, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                        else drawRect(tint.copy(alpha = 0.82f))
                        drawRect(tint.copy(alpha = glassAlpha))
                    } else if (backdrop != null) {
                        val offset = origin - backdrop.origin
                        sample.record(size = sampleSize) {
                            drawRect(tint)
                            scale(sampleScale, sampleScale, pivot = Offset.Zero) {
                                translate(-offset.x, -offset.y) { drawLayer(backdrop.layer) }
                            }
                        }
                        scale(1f / sampleScale, 1f / sampleScale, pivot = Offset.Zero) { drawLayer(sample) }
                        drawRect(tint.copy(alpha = glassAlpha))
                    } else {
                        // Outside a GlassBackdrop there is nothing to sample; keep readable tinted glass.
                        drawRect(tint.copy(alpha = 0.94f))
                    }
                    drawRect(highlight)
                }
                drawRoundRect(border, cornerRadius = radius, style = stroke)
                drawContent()
            }
        }
}

// Original AGSL: rounded-rectangle normal drives the edge lens over a Gaussian-blurred backdrop.
internal const val GLASS_SHADER = """
uniform shader backdrop;
uniform float2 extent;
uniform float radius;
uniform float density;
half4 main(float2 p) {
    float2 centered = p - extent * 0.5;
    float2 q = abs(centered) - (extent * 0.5 - radius);
    float2 outside = max(q, 0.0);
    float outsideLength = length(outside);
    float d = outsideLength + min(max(q.x, q.y), 0.0) - radius;
    // Exact rounded-rectangle normal: one distance calculation instead of five finite differences.
    float2 axis = q.x > q.y ? float2(1, 0) : float2(0, 1);
    float2 normal = (outsideLength > 0.001 ? outside / outsideLength : axis) * sign(centered);
    float rim = 1.0 - smoothstep(0.0, 21.0 * density, abs(d));
    float2 uv = clamp(p - normal * rim * 8.0 * density, float2(0.5), extent - 0.5);
    half4 c = backdrop.eval(uv);
    float light = max(dot(normal, float2(-0.70710678, -0.70710678)), 0.0);
    // Grazing reflections stay at the rim; the center remains transparent.
    float edge = 1.0 - smoothstep(0.0, 2.0 * density, abs(d));
    float opposite = max(dot(normal, float2(0.70710678, 0.70710678)), 0.0);
    float glint = light * light * light * rim * 0.10 + opposite * edge * 0.035;
    return half4(min(c.rgb + half3(glint), half3(1.0)), c.a);
}
"""
