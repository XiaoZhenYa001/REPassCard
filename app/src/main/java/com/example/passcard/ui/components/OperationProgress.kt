package com.example.passcard.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.LocalThemeColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/** Real work owns this component's lifetime; animation never delays completion. */
@Composable
fun LoadingIndicator(
    message: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    compact: Boolean = false
) {
    val colors = LocalThemeColors.current
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(180); animate = true }
    val accessibility = modifier.semantics(mergeDescendants = true) {
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        liveRegion = LiveRegionMode.Polite
    }
    if (compact) {
        Row(accessibility, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LoadingDots(colors.primary, animate)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            }
        }
    } else {
        Column(accessibility, horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LoadingDots(colors.primary, animate)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, textAlign = TextAlign.Center)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun LoadingDots(color: Color, animate: Boolean) {
    if (animate) {
        val transition = rememberInfiniteTransition(label = "loading_dots")
        val phase = transition.animateFloat(0f, (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            label = "loading_phase")
        Canvas(Modifier.size(42.dp, 24.dp)) {
            // Read animation state only in the draw phase; surrounding content does not recompose each frame.
            for (index in 0..2) {
                val wave = sin(phase.value - index * 0.75f)
                drawCircle(color.copy(alpha = 0.55f + 0.45f * ((wave + 1f) / 2f)), radius = 3.dp.toPx(),
                    center = Offset(size.width * (index + 1) / 4f, size.height / 2f - wave * 3.dp.toPx()))
            }
        }
    } else {
        Canvas(Modifier.size(42.dp, 24.dp)) {
            for (index in 0..2) drawCircle(color.copy(alpha = 0.6f), radius = 3.dp.toPx(),
                center = Offset(size.width * (index + 1) / 4f, size.height / 2f))
        }
    }
}
