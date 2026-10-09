package io.wenyou.textquest.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.Serializable
import androidx.compose.ui.text.font.FontWeight

/** Durable appearance preferences; user content and credentials are kept elsewhere. */
@Immutable
@Serializable
data class AppearancePrefs(
    val glassEnabled: Boolean = false,
    val displayModeId: Int = 0,
    val listStyle: String = "follow",
    val iconStyle: String = "mono",
    val popupStyle: String = "anchor",
    val amoled: Boolean = false,
    val colorSource: String = "brand",
    val seed: String = "#0066CC",
    val paletteStyle: String = "tonal",
    val colorSpec: String = "2021",
    val advancedColors: Boolean = false,
    val roleColors: Map<String, String> = emptyMap(),
    val fontScale: Float = 1f,
    val fontWeight: Int = 0,
    val fontBold: Boolean = false,
    val fontFile: String = "",
    val fontName: String = "",
    val uiScale: Float = 1f,
    val displayScale: Int = 0,
    val launcherIcon: String = "default",
    val launcherShell: String = "system",
    val splashEnabled: Boolean = false,
    val splashRandom: Boolean = false,
    val splashAnimation: Boolean = true,
    val splashWallpaper: String = "ink",
    val language: String = "system",
    val glassMaterial: String = "balanced",
    val dockLabels: String = "both"
) {
    fun normalized() = copy(
        displayModeId = displayModeId.coerceAtLeast(0),
        listStyle = listStyle.takeIf { it in listOf("follow", "rounded") } ?: "follow",
        iconStyle = iconStyle.takeIf { it in listOf("mono", "color") } ?: "mono",
        popupStyle = popupStyle.takeIf { it in listOf("anchor", "dialog") } ?: "anchor",
        colorSource = colorSource.takeIf { it in listOf("brand", "wallpaper", "custom") } ?: "brand",
        seed = normalizeHex(seed) ?: "#0066CC",
        paletteStyle = paletteStyle.takeIf { it in listOf("tonal", "vibrant", "expressive", "neutral", "mono", "fidelity", "content", "rainbow", "fruit") } ?: "tonal",
        colorSpec = colorSpec.takeIf { it in listOf("2021", "2025") } ?: "2021",
        roleColors = roleColors.mapNotNull { (key, value) -> normalizeHex(value)?.let { key to it } }.toMap(),
        fontScale = if (fontScale.isFinite()) fontScale.coerceIn(.85f, 1.3f) else 1f,
        fontWeight = fontWeight.takeIf { it == 0 || it in 300..700 } ?: 0,
        uiScale = if (uiScale.isFinite()) uiScale.coerceIn(.85f, 1.15f) else 1f,
        displayScale = if (displayScale == 0) 0 else displayScale.coerceIn(80, 120),
        fontFile = fontFile.takeIf { it.matches(Regex("font-[a-zA-Z0-9-]+\\.(ttf|otf|ttc)")) } ?: "",
        launcherIcon = launcherIcon.takeIf { it in listOf("default", "light", "dark", "ink") } ?: "default",
        launcherShell = launcherShell.takeIf { it in listOf("system", "light", "dark") } ?: "system",
        language = language.takeIf { it in listOf("system", "zh-CN", "zh-TW", "en") } ?: "system",
        glassMaterial = glassMaterial.takeIf { it in listOf("clear", "balanced", "frosted") } ?: "balanced",
        dockLabels = dockLabels.takeIf { it in listOf("both", "icons", "text") } ?: "both"
    )
}

/** Bolding is an overlay so disabling it restores the user's previous weight. */
internal fun AppearancePrefs.resolveFontWeight(original: FontWeight?): FontWeight? {
    val selected = if (fontWeight == 0) original else FontWeight(fontWeight)
    return if (fontBold) FontWeight(maxOf(selected?.weight ?: 400, 700)) else selected
}

fun normalizeHex(raw: String): String? = raw.trim().removePrefix("#").takeIf {
    it.matches(Regex("[0-9a-fA-F]{6}"))
}?.let { "#${it.uppercase()}" }

internal fun hexArgb(raw: String): Int = (0xFF000000L or requireNotNull(normalizeHex(raw)).removePrefix("#").toLong(16)).toInt()

val LocalAppearance = staticCompositionLocalOf { AppearancePrefs() }
val LocalGlassEnabled = staticCompositionLocalOf { false }
