package com.example.passcard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val OutfitFontFamily = FontFamily.SansSerif
val InterFontFamily = FontFamily.SansSerif

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    )
)

val InputTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.W500,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp
)

val PasswordPlaceholderStyle = InputTextStyle

val CategoryPillStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.W600,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp
)

val SecurityScoreStyle = TextStyle(
    fontFamily = OutfitFontFamily,
    fontWeight = FontWeight.W800,
    fontSize = 64.sp,
    lineHeight = 64.sp,
    letterSpacing = 0.sp
)

val StatValueStyle = TextStyle(
    fontFamily = OutfitFontFamily,
    fontWeight = FontWeight.W700,
    fontSize = 22.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.sp
)

val StatLabelStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.W600,
    fontSize = 12.sp,
    lineHeight = 17.sp,
    letterSpacing = 0.sp
)
