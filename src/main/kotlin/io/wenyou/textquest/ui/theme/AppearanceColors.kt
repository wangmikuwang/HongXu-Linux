package io.wenyou.textquest.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.wenyou.textquest.colorutilities.dynamiccolor.ColorSpec.SpecVersion
import io.wenyou.textquest.colorutilities.dynamiccolor.DynamicScheme
import io.wenyou.textquest.colorutilities.hct.Hct
import io.wenyou.textquest.colorutilities.scheme.*

/** One-tap palettes: a seed color plus a palette style, both still adjustable afterwards. */
internal data class ColorPreset(val name: String, val seed: String, val style: String)

internal val ColorPresets = listOf(
    ColorPreset("樱花", "#F48FB1", "vibrant"), ColorPreset("蜜桃", "#FF8A65", "vibrant"), ColorPreset("珊瑚", "#FF6F61", "fidelity"),
    ColorPreset("玫瑰", "#C2185B", "fidelity"), ColorPreset("琥珀", "#FFB300", "vibrant"), ColorPreset("晚霞", "#F4511E", "fidelity"),
    ColorPreset("抹茶", "#7CB342", "tonal"), ColorPreset("森林", "#2E7D32", "fidelity"), ColorPreset("极光", "#00BFA5", "vibrant"),
    ColorPreset("海盐", "#29B6F6", "vibrant"), ColorPreset("深海", "#1565C0", "fidelity"), ColorPreset("薰衣草", "#9575CD", "vibrant"),
    ColorPreset("暮紫", "#6A1B9A", "fidelity"), ColorPreset("糖果", "#EC407A", "fruit"), ColorPreset("奶茶", "#A1887F", "tonal"),
    ColorPreset("墨夜", "#455A64", "neutral"), ColorPreset("石墨", "#616161", "mono")
)

internal fun customColors(base: ColorScheme, prefs: AppearancePrefs, dark: Boolean): ColorScheme {
    val source = Hct.fromInt(hexArgb(prefs.seed))
    val spec = if (prefs.colorSpec == "2025") SpecVersion.SPEC_2025 else SpecVersion.SPEC_2021
    val platform = DynamicScheme.Platform.PHONE
    val scheme: DynamicScheme = when (prefs.paletteStyle) {
        "vibrant" -> SchemeVibrant(source, dark, 0.0, spec, platform)
        "expressive" -> SchemeExpressive(source, dark, 0.0, spec, platform)
        "neutral" -> SchemeNeutral(source, dark, 0.0, spec, platform)
        "mono" -> SchemeMonochrome(source, dark, 0.0, spec, platform)
        "fidelity" -> SchemeFidelity(source, dark, 0.0, spec, platform)
        "content" -> SchemeContent(source, dark, 0.0, spec, platform)
        "rainbow" -> SchemeRainbow(source, dark, 0.0, spec, platform)
        "fruit" -> SchemeFruitSalad(source, dark, 0.0, spec, platform)
        else -> SchemeTonalSpot(source, dark, 0.0, spec, platform)
    }
    return base.copy(
        primary = Color(scheme.primary), onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer), onPrimaryContainer = Color(scheme.onPrimaryContainer),
        secondary = Color(scheme.secondary), onSecondary = Color(scheme.onSecondary),
        secondaryContainer = Color(scheme.secondaryContainer), onSecondaryContainer = Color(scheme.onSecondaryContainer),
        tertiary = Color(scheme.tertiary), onTertiary = Color(scheme.onTertiary),
        tertiaryContainer = Color(scheme.tertiaryContainer), onTertiaryContainer = Color(scheme.onTertiaryContainer),
        background = Color(scheme.background), onBackground = Color(scheme.onBackground),
        surface = Color(scheme.surface), onSurface = Color(scheme.onSurface),
        surfaceVariant = Color(scheme.surfaceVariant), onSurfaceVariant = Color(scheme.onSurfaceVariant),
        surfaceContainerLowest = Color(scheme.surfaceContainerLowest), surfaceContainerLow = Color(scheme.surfaceContainerLow),
        surfaceContainer = Color(scheme.surfaceContainer), surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
        surfaceContainerHighest = Color(scheme.surfaceContainerHighest), outline = Color(scheme.outline), outlineVariant = Color(scheme.outlineVariant),
        inverseSurface = Color(scheme.inverseSurface), inverseOnSurface = Color(scheme.inverseOnSurface), inversePrimary = Color(scheme.inversePrimary),
        error = Color(scheme.error), onError = Color(scheme.onError), errorContainer = Color(scheme.errorContainer), onErrorContainer = Color(scheme.onErrorContainer)
    )
}

internal fun overrideColors(base: ColorScheme, prefs: AppearancePrefs, dark: Boolean): ColorScheme {
    if (!prefs.advancedColors) return base
    fun color(role: String, fallback: Color) = prefs.roleColors["${if (dark) "dark" else "light"}_$role"]
        ?.let { runCatching { Color(hexArgb(it)) }.getOrNull() } ?: fallback
    val control = color("control", base.primary)
    return base.copy(background = color("background", base.background),
        onBackground = color("text", base.onBackground), onSurface = color("text", base.onSurface),
        onSurfaceVariant = color("secondary", base.onSurfaceVariant), primary = control,
        onPrimary = if (control == base.primary) base.onPrimary else if (control.luminance() > .179f) Color.Black else Color.White)
}
