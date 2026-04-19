package com.example.passcard.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = Surface,
    onPrimaryContainer = OnBackground,
    secondary = PrimaryDark,
    onSecondary = OnPrimary,
    tertiary = Success,
    onTertiary = OnPrimary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnBackground,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    onError = OnPrimary,
    errorContainer = ErrorContainer,
    onErrorContainer = Error,
    outline = Border,
    outlineVariant = BorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFF6366F1),
    onSecondary = OnPrimary,
    tertiary = Success,
    onTertiary = OnPrimary,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = Error,
    onError = OnPrimary,
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFFB3B3),
    outline = BorderDark,
    outlineVariant = Color(0xFF252525)
)

object DarkThemeColors {
    val Surface: Color = SurfaceDark
    val SurfaceVariant: Color = SurfaceVariantDark
    val OnBackground: Color = OnBackgroundDark
    val OnSurface: Color = OnSurfaceDark
    val OnSurfaceVariant: Color = OnSurfaceVariantDark
    val Muted: Color = MutedDark
    val Border: Color = BorderDark
    val TabInactive: Color = TabInactiveDark
    val TextPrimary: Color = TextPrimaryDark
    val TextSecondary: Color = TextSecondaryDark
    val TextTertiary: Color = TextTertiaryDark
    val IconBackground: Color = IconBackgroundDark
    val ErrorContainer: Color = ErrorContainerDark
    val WarningContainer: Color = WarningContainerDark
    val SuccessContainer: Color = SuccessContainerDark
}

@Composable
fun PassCardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
