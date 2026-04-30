package com.example.passcard.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

data class ThemeColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val muted: Color,
    val border: Color,
    val tabInactive: Color,
    val tabActive: Color,
    val tabBarBackground: Color,
    val tabBarBorder: Color,
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

/**
 * 全局 CompositionLocal，作为主题颜色的单一真相来源。
 * 由 MainScreen 层通过 CompositionLocalProvider 注入，
 * 所有子组件通过 LocalThemeColors.current 读取颜色。
 */
val LocalThemeColors = compositionLocalOf<ThemeColors> {
    buildThemeColors(isDark = false)
}

/**
 * 根据布尔值构建 ThemeColors，不依赖 isSystemInDarkTheme()。
 * 这样 App 手动选择的主题可以正确传递到所有组件。
 */
fun buildThemeColors(isDark: Boolean): ThemeColors {
    return if (isDark) {
        ThemeColors(
            background = BackgroundDark,
            surface = SurfaceDark,
            surfaceVariant = SurfaceVariantDark,
            onBackground = OnBackgroundDark,
            onSurface = OnSurfaceDark,
            onSurfaceVariant = OnSurfaceVariantDark,
            muted = MutedDark,
            border = BorderDark,
            tabInactive = TabInactiveDark,
            tabActive = TabActiveDark,
            tabBarBackground = TabBarBackgroundDark,
            tabBarBorder = TabBarBorderDark,
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
            background = Background,
            surface = Surface,
            surfaceVariant = SurfaceVariant,
            onBackground = OnBackground,
            onSurface = OnSurface,
            onSurfaceVariant = OnSurfaceVariant,
            muted = Muted,
            border = Border,
            tabInactive = TabInactive,
            tabActive = TabActive,
            tabBarBackground = TabBarBackground,
            tabBarBorder = TabBarBorder,
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

/**
 * 兼容旧代码的 @Composable 函数。
 * 统一从 LocalThemeColors.current 读取。
 */
@Composable
fun rememberThemeColors(): ThemeColors {
    return LocalThemeColors.current
}
