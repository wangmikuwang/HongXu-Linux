package io.wenyou.textquest.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Optional raw accent colors. Empty keeps the standard theme unchanged. */
internal val LocalAccentPalette = staticCompositionLocalOf<List<Color>> { emptyList() }

internal fun paletteAccent(colors: List<Color>, index: Int, fallback: Color): Color =
    if (colors.isEmpty()) fallback else colors[Math.floorMod(index, colors.size)]

@Composable
internal fun distributedAccent(index: Int, fallback: Color): Color =
    paletteAccent(LocalAccentPalette.current, index, fallback)

/** Black or white always gives at least 4.5:1 contrast for an opaque accent. */
internal fun accentForeground(color: Color): Color =
    if (color.luminance() > 0.179f) Color.Black else Color.White
