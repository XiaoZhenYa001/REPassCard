package com.example.passcard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.example.passcard.ui.theme.*
import kotlin.math.max

data class DropdownOption(
    val label: String,
    val value: String
)

@Composable
fun DropdownSelectMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    options: List<DropdownOption>,
    selectedValue: String,
    onOptionSelected: (DropdownOption) -> Unit,
    modifier: Modifier = Modifier,
    offset: IntOffset = IntOffset.Zero,
    itemWidth: Int = 0,
    itemHeight: Int = 0
) {
    if (!expanded) return

    val density = LocalDensity.current
    val themeColors = rememberThemeColors()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    val menuAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(MotionDuration.Fast),
        label = "dropdown_alpha"
    )
    val menuScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else MotionScale.Enter,
        animationSpec = tween(MotionDuration.Standard),
        label = "dropdown_scale"
    )
    val screenMarginPx = with(density) { Spacing8.roundToPx() }
    val positionProvider = remember(offset, itemWidth, itemHeight, screenMarginPx) {
        DropdownPopupPositionProvider(
            anchorOffset = offset,
            anchorSize = IntSize(itemWidth, itemHeight),
            screenMarginPx = screenMarginPx
        )
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = modifier
                .width(180.dp)
                .graphicsLayer {
                    alpha = menuAlpha
                    scaleX = menuScale
                    scaleY = menuScale
                    transformOrigin = TransformOrigin(1f, 0f)
                },
            shape = RoundedCornerShape(Radius16),
            color = themeColors.surface,
            shadowElevation = Spacing8
        ) {
            Column(
                modifier = Modifier.padding(Spacing8),
                verticalArrangement = Arrangement.spacedBy(Spacing4)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        option = option,
                        isSelected = option.value == selectedValue,
                        onClick = {
                            onOptionSelected(option)
                            onDismissRequest()
                        },
                        colors = themeColors
                    )
                }
            }
        }
    }
}

private class DropdownPopupPositionProvider(
    private val anchorOffset: IntOffset,
    private val anchorSize: IntSize,
    private val screenMarginPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = calculateDropdownMenuPosition(
        anchorOffset = anchorOffset,
        anchorSize = anchorSize,
        popupSize = popupContentSize,
        windowSize = windowSize,
        screenMarginPx = screenMarginPx
    )
}

internal fun calculateDropdownMenuPosition(
    anchorOffset: IntOffset,
    anchorSize: IntSize,
    popupSize: IntSize,
    windowSize: IntSize,
    screenMarginPx: Int
): IntOffset {
    val maxX = max(screenMarginPx, windowSize.width - popupSize.width - screenMarginPx)
    val maxY = max(screenMarginPx, windowSize.height - popupSize.height - screenMarginPx)

    return IntOffset(
        x = (anchorOffset.x + anchorSize.width - popupSize.width)
            .coerceIn(screenMarginPx, maxX),
        // Context menus feel attached to their setting when their top edges align.
        // This also removes the old extra full-row vertical offset.
        y = anchorOffset.y.coerceIn(screenMarginPx, maxY)
    )
}

@Composable
private fun DropdownMenuItem(
    option: DropdownOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    colors: ThemeColors = rememberThemeColors()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (isSelected) Modifier.background(colors.primary.copy(alpha = 0.1f)) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W500
            ),
            color = if (isSelected) colors.primary else colors.onBackground
        )
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
