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

val AppleTypography = AppTypography.copy(
    headlineLarge = AppTypography.headlineLarge.copy(fontFamily = FontFamily.Default, fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = 0.sp),
    displaySmall = AppTypography.displaySmall.copy(fontFamily = FontFamily.Default, fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = 0.sp),
    titleLarge = AppTypography.titleLarge.copy(fontFamily = FontFamily.Default, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    labelSmall = AppTypography.labelSmall.copy(fontFamily = FontFamily.Default, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.sp),
    bodyLarge = AppTypography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 27.sp, letterSpacing = 0.sp),
    bodyMedium = AppTypography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 24.sp, letterSpacing = 0.sp)
)
