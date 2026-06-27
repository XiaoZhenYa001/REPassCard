package com.example.passcard.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ElevationLevel {
    Flat,
    Card,
    Elevated,
    Floating
}

fun Modifier.softShadow(
    colors: ThemeColors,
    shape: Shape,
    level: ElevationLevel = ElevationLevel.Card
): Modifier {
    val elevation = when (level) {
        ElevationLevel.Flat -> 0.dp
        ElevationLevel.Card -> if (colors.isDark) 4.dp else 8.dp
        ElevationLevel.Elevated -> if (colors.isDark) 8.dp else 12.dp
        ElevationLevel.Floating -> if (colors.isDark) 12.dp else 16.dp
    }
    val spot = if (colors.isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.10f)
    val ambient = if (colors.isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.05f)

    return shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = ambient,
        spotColor = spot
    )
}

fun Modifier.appleSurface(
    colors: ThemeColors,
    radius: Dp = Radius18,
    level: ElevationLevel = ElevationLevel.Card
): Modifier {
    val shape = RoundedCornerShape(radius)
    return softShadow(colors = colors, shape = shape, level = level)
        .clip(shape)
        .background(colors.surface)
}
