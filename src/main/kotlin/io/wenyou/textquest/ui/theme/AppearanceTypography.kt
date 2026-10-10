package io.wenyou.textquest.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import io.wenyou.textquest.data.AppearanceFiles
import java.io.File

internal val DefaultAppFont = FontFamily(Font("bundled_kai_regular.ttf"))

// Many Linux systems have no colour emoji font, and story covers and messages use emoji, so one is bundled.
private val EmojiFontFamily = FontFamily(Font("NotoColorEmoji.ttf"))

/**
 * Compose Desktop falls back, glyph by glyph, to every font it has registered before trying system fonts.
 * Resolving the emoji font once registers it, so all text (whatever its font) draws emoji in colour.
 */
@Composable
internal fun RegisterEmojiFallback() {
    val resolver = LocalFontFamilyResolver.current
    remember(resolver) { resolver.resolve(EmojiFontFamily) }
}

internal fun appearanceTypography(base: Typography, prefs: AppearancePrefs, context: Context): Typography {
    val family = if (prefs.fontFile.isEmpty()) null else runCatching {
        FontFamily(Font(File(AppearanceFiles.fontDirectory(context), prefs.fontFile)))
    }.getOrNull()
    fun TextStyle.applyPrefs() = copy(fontFamily = family ?: DefaultAppFont,
        fontWeight = prefs.resolveFontWeight(fontWeight))
    return base.copy(displayLarge = base.displayLarge.applyPrefs(), displayMedium = base.displayMedium.applyPrefs(),
        displaySmall = base.displaySmall.applyPrefs(), headlineLarge = base.headlineLarge.applyPrefs(),
        headlineMedium = base.headlineMedium.applyPrefs(), headlineSmall = base.headlineSmall.applyPrefs(),
        titleLarge = base.titleLarge.applyPrefs(), titleMedium = base.titleMedium.applyPrefs(), titleSmall = base.titleSmall.applyPrefs(),
        bodyLarge = base.bodyLarge.applyPrefs(), bodyMedium = base.bodyMedium.applyPrefs(), bodySmall = base.bodySmall.applyPrefs(),
        labelLarge = base.labelLarge.applyPrefs(), labelMedium = base.labelMedium.applyPrefs(), labelSmall = base.labelSmall.applyPrefs())
}
