package io.wenyou.textquest.ui.theme

import io.wenyou.textquest.platform.platformContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 外观三态：跟随系统 / 浅色 / 深色。 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ThemeStyle {
    MATERIAL, APPLE, MIUIX;
    companion object {
        fun fromStored(raw: String?): ThemeStyle = entries.firstOrNull { it.name == raw } ?: MATERIAL
    }
}

/** Apple HIG 风格：系统蓝、分组背景；继续使用 Android 原生字体与控件。 */
internal fun appleColors(dark: Boolean) = if (dark) darkColorScheme(
    primary = Color(0xFF80B8FF), onPrimary = Color(0xFF002B55),
    primaryContainer = Color(0xFF12395C), onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Color(0xFFBFC6D0), secondaryContainer = Color(0xFF30343B), onSecondaryContainer = Color.White,
    tertiaryContainer = Color(0xFF242426), onTertiaryContainer = Color(0xFFF5F5F7),
    background = Color.Black, onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF1C1C1E), onSurface = Color(0xFFF5F5F7),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF242426), surfaceContainerHigh = Color(0xFF2C2C2E), surfaceContainerHighest = Color(0xFF3A3A3C),
    surfaceVariant = Color(0xFF2C2C2E), onSurfaceVariant = Color(0xFFCACAD0), outline = Color(0xFF96969D), outlineVariant = Color(0xFF38383A), surfaceTint = Color.Transparent
) else lightColorScheme(
    primary = Color(0xFF0066CC), onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F0FF), onPrimaryContainer = Color(0xFF003366),
    secondary = Color(0xFF526070), secondaryContainer = Color(0xFFE9EDF2), onSecondaryContainer = Color(0xFF252A31),
    tertiaryContainer = Color.White, onTertiaryContainer = Color(0xFF1C1C1E),
    background = Color(0xFFF2F2F7), onBackground = Color(0xFF1C1C1E),
    surface = Color.White, onSurface = Color(0xFF1C1C1E),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF8F8FA), surfaceContainerHigh = Color(0xFFECECF1), surfaceContainerHighest = Color(0xFFE4E4E9),
    surfaceVariant = Color(0xFFECECF1), onSurfaceVariant = Color(0xFF56565E), outline = Color(0xFF767680), outlineVariant = Color(0xFFD1D1D6), surfaceTint = Color.Transparent
)
val LocalThemeStyle = staticCompositionLocalOf { ThemeStyle.MATERIAL }
private val AppleShapes = Shapes(extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(30.dp))
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)


internal val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = BrandOnSecondary,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandTertiary,
    onTertiary = Color.White,
    tertiaryContainer = BrandTertiaryContainer,
    onTertiaryContainer = Color(0xFF301505),
    background = BrandBackgroundLight,
    onBackground = Color(0xFF211D24),
    surface = BrandSurfaceLight,
    onSurface = Color(0xFF211D24),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2EEE8),
    surfaceContainer = Color(0xFFEDE8E2),
    surfaceContainerHigh = Color(0xFFE7E1DC),
    surfaceContainerHighest = Color(0xFFE1DBD6),
    surfaceVariant = Color(0xFFE7E0E8), onSurfaceVariant = Color(0xFF514954),
    outline = Color(0xFF807681), outlineVariant = Color(0xFFD0C6D0),
    inverseSurface = Color(0xFF332E35), inverseOnSurface = Color(0xFFF7EFF7),
    inversePrimary = Color(0xFFD7B9EB)
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFD7B9EB),
    onPrimary = Color(0xFF382348),
    primaryContainer = Color(0xFF4F395F),
    onPrimaryContainer = BrandPrimaryContainer,
    secondary = Color(0xFFC1CDB6),
    onSecondary = Color(0xFF2C3625),
    secondaryContainer = Color(0xFF424D3A),
    onSecondaryContainer = BrandSecondaryContainer,
    tertiary = Color(0xFFF1B995), onTertiary = Color(0xFF492913),
    tertiaryContainer = Color(0xFF633E27), onTertiaryContainer = BrandTertiaryContainer,
    background = BrandBackgroundDark,
    onBackground = Color(0xFFECE5EB),
    surface = BrandSurfaceDark, onSurface = Color(0xFFECE5EB),
    surfaceContainerLowest = Color(0xFF100E11),
    surfaceContainerLow = Color(0xFF211E23), surfaceContainer = Color(0xFF27232A),
    surfaceContainerHigh = Color(0xFF302B33), surfaceContainerHighest = Color(0xFF3A343D),
    surfaceVariant = Color(0xFF4A424E), onSurfaceVariant = Color(0xFFD0C4D3),
    outline = Color(0xFF9B8F9E), outlineVariant = Color(0xFF4A424E),
    inverseSurface = Color(0xFFECE5EB), inverseOnSurface = Color(0xFF332E35),
    inversePrimary = BrandPrimary
)

/** 共享主题入口：风格与明暗模式独立；Material You 支持 Android 12+ 壁纸取色。 */
@Composable
fun WenYouTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    style: ThemeStyle = ThemeStyle.MATERIAL,
    prideTheme: PrideTheme? = null,
    appearance: AppearancePrefs = AppearancePrefs(glassEnabled = style == ThemeStyle.APPLE),
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = platformContext()
    RegisterEmojiFallback()
    val baseColors = androidx.compose.runtime.remember(style, dark, dynamicColor, prideTheme, appearance.colorSource, appearance.seed, appearance.paletteStyle, appearance.colorSpec, context) {
    val original = when {
        appearance.colorSource == "custom" -> customColors(if (dark) DarkColors else LightColors, appearance, dark)
        style == ThemeStyle.APPLE -> appleColors(dark)
        style == ThemeStyle.MIUIX -> appleColors(dark).copy(primary = if (dark) Color(0xFF89B6FF) else Color(0xFF3482FF))
        dark -> DarkColors
        else -> LightColors
    }
    original
    }
    val amoledBase = if (dark && appearance.amoled) baseColors.copy(background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black) else baseColors
    val configured = overrideColors(amoledBase, appearance, dark)
    val colorScheme = prideTheme?.let { prideColors(configured, it) } ?: configured
    val systemDensity = androidx.compose.ui.platform.LocalDensity.current
    val densityScale = appearance.uiScale * (if (appearance.displayScale == 0) 1f else appearance.displayScale / 100f)
    val typography = androidx.compose.runtime.remember(style, appearance.fontFile, appearance.fontWeight, appearance.fontBold, context) {
        appearanceTypography(if (style == ThemeStyle.APPLE) AppleTypography else AppTypography, appearance, context)
    }
    androidx.compose.runtime.SideEffect { io.wenyou.textquest.ui.common.appLanguage = appearance.language }
    CompositionLocalProvider(LocalThemeStyle provides style, LocalAppearance provides appearance, LocalGlassEnabled provides appearance.glassEnabled,
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(systemDensity.density * densityScale, systemDensity.fontScale * appearance.fontScale),
        LocalAccentPalette provides (prideTheme?.let { theme ->
            (theme.colors + if (theme == PrideTheme.DEMISEXUAL) listOf(Color.Black) else emptyList()).distinct()
        } ?: emptyList())) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = if (style == ThemeStyle.APPLE || style == ThemeStyle.MIUIX) AppleShapes else AppShapes,
            content = content
        )
    }
}
