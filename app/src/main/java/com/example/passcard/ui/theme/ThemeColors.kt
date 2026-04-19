package com.example.passcard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

data class ThemeColors(
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val muted: Color,
    val border: Color,
    val tabInactive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val iconBackground: Color,
    val errorContainer: Color,
    val warningContainer: Color,
    val successContainer: Color,
    val primary: Color,
    val error: Color,
    val warning: Color,
    val success: Color,
    val isDark: Boolean
)

@Composable
fun rememberThemeColors(): ThemeColors {
    val isDark = isSystemInDarkTheme()
    
    return remember(isDark) {
        if (isDark) {
            ThemeColors(
                surface = SurfaceDark,
                surfaceVariant = SurfaceVariantDark,
                onBackground = OnBackgroundDark,
                onSurface = OnSurfaceDark,
                onSurfaceVariant = OnSurfaceVariantDark,
                muted = MutedDark,
                border = BorderDark,
                tabInactive = TabInactiveDark,
                textPrimary = TextPrimaryDark,
                textSecondary = TextSecondaryDark,
                textTertiary = TextTertiaryDark,
                iconBackground = IconBackgroundDark,
                errorContainer = ErrorContainerDark,
                warningContainer = WarningContainerDark,
                successContainer = SuccessContainerDark,
                primary = Primary,
                error = Error,
                warning = Warning,
                success = Success,
                isDark = true
            )
        } else {
            ThemeColors(
                surface = Surface,
                surfaceVariant = SurfaceVariant,
                onBackground = OnBackground,
                onSurface = OnSurface,
                onSurfaceVariant = OnSurfaceVariant,
                muted = Muted,
                border = Border,
                tabInactive = TabInactive,
                textPrimary = TextPrimary,
                textSecondary = TextSecondary,
                textTertiary = TextTertiary,
                iconBackground = IconBackground,
                errorContainer = ErrorContainer,
                warningContainer = WarningContainer,
                successContainer = SuccessContainer,
                primary = Primary,
                error = Error,
                warning = Warning,
                success = Success,
                isDark = false
            )
        }
    }
}
