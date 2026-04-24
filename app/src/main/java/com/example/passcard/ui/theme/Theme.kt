package com.example.passcard.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private const val THEME_ANIMATION_DURATION_MS = 420

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
private fun animateThemeColors(target: ThemeColors): ThemeColors {
    val background by animateColorAsState(targetValue = target.background, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_bg")
    val surface by animateColorAsState(targetValue = target.surface, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_surface")
    val surfaceVariant by animateColorAsState(targetValue = target.surfaceVariant, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_surface_variant")
    val onBackground by animateColorAsState(targetValue = target.onBackground, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_on_bg")
    val onSurface by animateColorAsState(targetValue = target.onSurface, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_on_surface")
    val onSurfaceVariant by animateColorAsState(targetValue = target.onSurfaceVariant, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_on_surface_variant")
    val muted by animateColorAsState(targetValue = target.muted, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_muted")
    val border by animateColorAsState(targetValue = target.border, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_border")
    val tabInactive by animateColorAsState(targetValue = target.tabInactive, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_tab_inactive")
    val textPrimary by animateColorAsState(targetValue = target.textPrimary, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_text_primary")
    val textSecondary by animateColorAsState(targetValue = target.textSecondary, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_text_secondary")
    val textTertiary by animateColorAsState(targetValue = target.textTertiary, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_text_tertiary")
    val iconBackground by animateColorAsState(targetValue = target.iconBackground, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_icon_bg")
    val errorContainer by animateColorAsState(targetValue = target.errorContainer, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_error_container")
    val warningContainer by animateColorAsState(targetValue = target.warningContainer, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_warning_container")
    val successContainer by animateColorAsState(targetValue = target.successContainer, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_success_container")
    val primary by animateColorAsState(targetValue = target.primary, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_primary")
    val error by animateColorAsState(targetValue = target.error, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_error")
    val warning by animateColorAsState(targetValue = target.warning, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_warning")
    val success by animateColorAsState(targetValue = target.success, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "theme_success")

    return target.copy(
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        onBackground = onBackground,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
        muted = muted,
        border = border,
        tabInactive = tabInactive,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textTertiary = textTertiary,
        iconBackground = iconBackground,
        errorContainer = errorContainer,
        warningContainer = warningContainer,
        successContainer = successContainer,
        primary = primary,
        error = error,
        warning = warning,
        success = success
    )
}

@Composable
private fun animateMaterialColorScheme(target: ColorScheme): ColorScheme {
    val primary by animateColorAsState(targetValue = target.primary, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_primary")
    val onPrimary by animateColorAsState(targetValue = target.onPrimary, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_on_primary")
    val primaryContainer by animateColorAsState(targetValue = target.primaryContainer, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_primary_container")
    val onPrimaryContainer by animateColorAsState(targetValue = target.onPrimaryContainer, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_on_primary_container")
    val secondary by animateColorAsState(targetValue = target.secondary, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_secondary")
    val onSecondary by animateColorAsState(targetValue = target.onSecondary, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_on_secondary")
    val tertiary by animateColorAsState(targetValue = target.tertiary, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_tertiary")
    val onTertiary by animateColorAsState(targetValue = target.onTertiary, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_on_tertiary")
    val background by animateColorAsState(targetValue = target.background, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_background")
    val onBackground by animateColorAsState(targetValue = target.onBackground, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_on_background")
    val surface by animateColorAsState(targetValue = target.surface, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_surface")
    val onSurface by animateColorAsState(targetValue = target.onSurface, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_on_surface")
    val surfaceVariant by animateColorAsState(targetValue = target.surfaceVariant, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_surface_variant")
    val onSurfaceVariant by animateColorAsState(targetValue = target.onSurfaceVariant, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_on_surface_variant")
    val error by animateColorAsState(targetValue = target.error, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_error")
    val onError by animateColorAsState(targetValue = target.onError, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_on_error")
    val errorContainer by animateColorAsState(targetValue = target.errorContainer, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_error_container")
    val onErrorContainer by animateColorAsState(targetValue = target.onErrorContainer, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_on_error_container")
    val outline by animateColorAsState(targetValue = target.outline, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_outline")
    val outlineVariant by animateColorAsState(targetValue = target.outlineVariant, animationSpec = tween(THEME_ANIMATION_DURATION_MS), label = "m3_outline_variant")

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        tertiary = tertiary,
        onTertiary = onTertiary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant
    )
}

@Composable
fun PassCardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val targetCustomColors = buildThemeColors(isDark = darkTheme)
    val customColors = animateThemeColors(targetCustomColors)

    val targetColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val colorScheme = animateMaterialColorScheme(targetColorScheme)
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
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
