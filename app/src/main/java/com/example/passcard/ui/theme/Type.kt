package com.example.passcard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.passcard.R

// Font Families - will use system fonts as fallback if custom fonts not available
val OutfitFontFamily = FontFamily.Default
val InterFontFamily = FontFamily.Default

// Custom Typography based on design specs
val AppTypography = Typography(
    // Display - 用于设置页面大标题 (40sp, 800)
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 40.sp,
        letterSpacing = (-1).sp
    ),
    
    // Headline - 用于用户名等 (24sp, 700)
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp
    ),
    
    // Title Large - 用于安全中心标题等 (20sp, 700)
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 20.sp
    ),
    
    // Title Medium - 用于 Section 标题 (18sp, 600)
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp
    ),
    
    // Body Large - 用于输入框文字 (16sp, 500/600)
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp
    ),
    
    // Body Medium - 用于正文 (14sp, 400/500/600)
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp
    ),
    
    // Body Small - 用于辅助文字 (12sp, 400)
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp
    ),
    
    // Label - 用于 Tab 标签 (10sp, 500)
    labelSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    ),
    
    // Label Medium - 用于分类标签 (14sp, 500/600)
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp
    ),
    
    // Label Large - 用于输入框标签 (14sp, 600)
    labelLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp
    )
)

// Additional text styles for specific use cases
val InputTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.W500,
    fontSize = 16.sp
)

val PasswordPlaceholderStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.W500,
    fontSize = 16.sp
)

val CategoryPillStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.W600,
    fontSize = 13.sp
)

val SecurityScoreStyle = TextStyle(
    fontFamily = OutfitFontFamily,
    fontWeight = FontWeight.W800,
    fontSize = 64.sp,
    lineHeight = 1.sp
)

val StatValueStyle = TextStyle(
    fontFamily = OutfitFontFamily,
    fontWeight = FontWeight.W700,
    fontSize = 24.sp
)

val StatLabelStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.W500,
    fontSize = 11.sp
)
