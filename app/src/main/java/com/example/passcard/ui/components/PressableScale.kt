package com.example.passcard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import com.example.passcard.util.playPassCardClick
import com.example.passcard.ui.theme.MotionDuration
import com.example.passcard.ui.theme.MotionEasing
import com.example.passcard.ui.theme.MotionScale

@Composable
fun PressableScale(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressScale: Float = MotionScale.Pressed,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val touchInput = LocalInputModeManager.current.inputMode == InputMode.Touch
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && touchInput) pressScale else 1f,
        animationSpec = tween(
            durationMillis = if (!touchInput) 0 else if (pressed) MotionDuration.Press else MotionDuration.Release,
            easing = MotionEasing.EnterExit
        ),
        label = "press_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = { view.playPassCardClick(); onClick() }
            )
    ) {
        content()
    }
}
