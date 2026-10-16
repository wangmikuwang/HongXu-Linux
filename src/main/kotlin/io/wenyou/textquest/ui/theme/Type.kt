package io.wenyou.textquest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 纸页式标题搭配系统正文；长文保留舒展行距，不依赖在线字体。
 */
val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

private fun ios(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = 0.sp)

/**
 * Apple HIG iOS text styles (Large Title 34 … Caption 2 11). Sizes follow the HIG; line heights are
 * opened up for CJK glyphs, and Body keeps the long-reading leading of the story text.
 */
val AppleTypography = Typography(
    displayLarge = ios(34, 44, FontWeight.Bold), displayMedium = ios(34, 44, FontWeight.Bold),
    displaySmall = ios(34, 44, FontWeight.Bold),      // Large Title
    headlineLarge = ios(34, 44, FontWeight.Bold),     // Large Title
    headlineMedium = ios(28, 36, FontWeight.Bold),    // Title 1
    headlineSmall = ios(22, 30, FontWeight.Bold),     // Title 2
    titleLarge = ios(20, 28, FontWeight.SemiBold),    // Title 3
    titleMedium = ios(17, 24, FontWeight.SemiBold),   // Headline
    titleSmall = ios(15, 21, FontWeight.SemiBold),    // Subheadline, emphasized
    bodyLarge = ios(17, 27),                          // Body
    bodyMedium = ios(15, 23),                         // Subheadline
    bodySmall = ios(13, 18),                          // Footnote
    labelLarge = ios(17, 22, FontWeight.Medium),      // Buttons
    labelMedium = ios(12, 16),                        // Caption 1
    labelSmall = ios(11, 14)                          // Caption 2
)
