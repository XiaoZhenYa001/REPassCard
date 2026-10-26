package com.example.passcard.ui.theme

import androidx.compose.ui.graphics.Color

// Core brand colors
val Primary = Color(0xFF5564A8)
val PrimaryDark = Color(0xFF44518D)
val PrimaryDarkMode = Color(0xFFA8B5EC)
val PrimaryDarkModePressed = Color(0xFF8796D2)
val PrimaryGradStart = Color(0xFF5564A8)
val PrimaryGradEnd = Color(0xFF6270B4)
val PrimaryGradStartDark = Color(0xFFA8B5EC)
val PrimaryGradEndDark = Color(0xFFB5C0F0)
val PrimaryLight = Color(0x145564A8)
val PrimaryLightDark = Color(0x1FA8B5EC)

// Semantic status colors
val Error = Color(0xFFBA454B)
val ErrorDark = Color(0xFFF19A9F)
val Warning = Color(0xFF946218)
val Success = Color(0xFF277759)
val Blue = Color(0xFF416EA2)
val BlueDark = Color(0xFF94B8E6)
val Cyan = Color(0xFF357C94)
val CyanDark = Color(0xFF88C8D9)

// Surfaces - light
val Background = Color(0xFFF4F6FA)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFEDF0F6)
val OnBackground = Color(0xFF202738)
val OnSurface = Color(0xFF202738)
val OnSurfaceVariant = Color(0xFF596477)
val Muted = Color(0xFF657084)
val OnPrimary = Color(0xFFFFFFFF)
val OnPrimaryDark = Color(0xFF18213B)
val SuccessDark = Color(0xFF6FCBA6)
val WarningDark = Color(0xFFEBC080)

// Surfaces - dark
val BackgroundDark = Color(0xFF131821)
val SurfaceDark = Color(0xFF1C2330)
val SurfaceVariantDark = Color(0xFF283243)
val OnBackgroundDark = Color(0xFFEDF1FA)
val OnSurfaceDark = Color(0xFFEDF1FA)
val OnSurfaceVariantDark = Color(0xFFB1BBCD)
val MutedDark = Color(0xFF97A3B8)

// Containers
val ErrorContainer = Color(0x14BA454B)
val ErrorLight = Color(0x14BA454B)
val WarningContainer = Color(0x14946218)
val WarningLight = Color(0x14946218)
val SuccessContainer = Color(0x14277759)
val BlueContainer = Color(0x14416EA2)

val ErrorContainerDark = Color(0x26F19A9F)
val WarningContainerDark = Color(0x26EBC080)
val SuccessContainerDark = Color(0x266FCBA6)
val BlueContainerDark = Color(0x2694B8E6)

// Separators and borders. Keep old names for existing screens.
val Border = Color(0x1F536078)
val BorderLight = Color(0x1F536078)
val BorderDark = Color(0x3D91A0BD)

// Tab bar
val TabInactive = Color(0xFF657187)
val TabInactiveDark = Color(0xFF98A5BD)
val TabActive = Primary
val TabActiveDark = PrimaryDarkMode
val TabBarBackground = Color(0xF5FFFFFF)
val TabBarBackgroundDark = Color(0xF51C2330)
val TabBarBorder = Color(0x12536078)
val TabBarBorderDark = Color(0x2691A0BD)
val AddButtonGradientEnd = PrimaryGradEnd

// Text aliases used by older components
val CardGray = SurfaceVariant
val TextPrimary = OnBackground
val TextSecondary = OnSurfaceVariant
val TextTertiary = Muted
val CategoryActive = Color(0xFF202738)
val IconBackground = SurfaceVariant

val TextPrimaryDark = OnBackgroundDark
val TextSecondaryDark = OnSurfaceVariantDark
val TextTertiaryDark = MutedDark
val IconBackgroundDark = SurfaceVariantDark

// Fixed utility colors
val ScoreCardBackground = Color(0xFF273249)
val IconBgBlue = Color(0xFF5579A7)
val IconBgGreen = Color(0xFF40846C)
val IconBgOrange = Color(0xFFA77A43)
val IconBgPurple = Color(0xFF7369A9)
val IconBgCyan = Color(0xFF488798)
val IconBgRed = Color(0xFFB96369)
val IconBgGray = Color(0xFF748197)
val IconBgLightGreen = Color(0xFF40846C)
