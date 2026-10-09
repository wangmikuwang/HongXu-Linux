package io.wenyou.textquest.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** "#RRGGBB" or "#AARRGGBB"; throws IllegalArgumentException like android.graphics.Color.parseColor. */
fun parseHexColor(hex: String): Color {
    require(hex.startsWith('#') && (hex.length == 7 || hex.length == 9)) { "Unknown color: $hex" }
    val value = hex.substring(1).toLong(16)
    return Color(if (hex.length == 7) value or 0xFF000000 else value)
}

/** WCAG contrast ratio of two opaque colours, 1–21. */
fun contrastRatio(a: Color, b: Color): Double {
    val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (light + 0.05) / (dark + 0.05)
}

/** Hue 0–360, saturation and value 0–1, matching android.graphics.Color.colorToHSV. */
fun Color.toHsv(): FloatArray {
    val max = maxOf(red, green, blue)
    val delta = max - minOf(red, green, blue)
    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta).mod(6f))
        max == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    return floatArrayOf(hue, if (max == 0f) 0f else delta / max, max)
}
