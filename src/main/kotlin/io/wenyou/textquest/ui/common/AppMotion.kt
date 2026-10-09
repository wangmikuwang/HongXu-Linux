package io.wenyou.textquest.ui.common

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Shared timing keeps navigation and touch feedback consistent without layout work per frame. */
internal object AppMotion {
    val ease = CubicBezierEasing(.2f, .8f, .2f, 1f)
    fun <T> page() = tween<T>(300, easing = ease)
    fun <T> fade() = tween<T>(160, easing = ease)
    fun <T> selection() = spring<T>(dampingRatio = .78f, stiffness = 430f)
    fun <T> press() = spring<T>(dampingRatio = 1f, stiffness = 1000f)
}

// Owned by the navigation host so changing tabs does not restart the lens at its destination.
internal val LocalDockPosition = staticCompositionLocalOf<State<Float>?> { null }

@Composable
internal fun Modifier.pressMotion(source: MutableInteractionSource): Modifier {
    val pressed = source.collectIsPressedAsState()
    val scale = animateFloatAsState(if (pressed.value) .97f else 1f, AppMotion.press(), label = "control-press")
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}
