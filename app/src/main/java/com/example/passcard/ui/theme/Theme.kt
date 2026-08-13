package com.example.passcard.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
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
    primary = PrimaryDarkMode,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLightDark,
    onPrimaryContainer = OnBackgroundDark,
    secondary = PrimaryDarkModePressed,
    onSecondary = OnPrimary,
    tertiary = Success,
    onTertiary = OnPrimary,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorDark,
    onError = OnPrimary,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorDark,
    outline = BorderDark,
    outlineVariant = SurfaceVariantDark
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
    // Theme values switch atomically. Animating every color independently broadcasts
    // frame-by-frame invalidations to nearly the entire composition tree.
    val context = LocalContext.current
    val customColors = remember(darkTheme) { buildThemeColors(isDark = darkTheme) }
    val colorScheme: ColorScheme = remember(dynamicColor, darkTheme, context) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalThemeColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
