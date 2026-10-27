package com.example.passcard.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
    itemHeight: Int = 0,
    onOptionSelectedAt: ((DropdownOption, Offset) -> Unit)? = null
) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded
    val transition = updateTransition(visibility, label = "dropdown_visibility")
    if (!visibility.currentState && !visibility.targetState && visibility.isIdle) return

    val density = LocalDensity.current
    val themeColors = rememberThemeColors()
    val menuAlpha = transition.animateFloat(
        transitionSpec = { tween(if (targetState) MotionDuration.Standard else MotionDuration.Exit, easing = MotionEasing.EnterExit) },
        label = "dropdown_alpha"
    ) { if (it) 1f else 0f }
    val menuScale = transition.animateFloat(
        transitionSpec = { tween(if (targetState) MotionDuration.Standard else MotionDuration.Exit, easing = MotionEasing.EnterExit) },
        label = "dropdown_scale"
    ) { if (it) 1f else MotionScale.Enter }
    var menuTransformOrigin by remember { mutableStateOf(TransformOrigin(1f, 0f)) }
    var menuPosition by remember { mutableStateOf(IntOffset.Zero) }
    val screenMarginPx = with(density) { Spacing8.roundToPx() }
    val positionProvider = remember(offset, itemWidth, itemHeight, screenMarginPx) {
        DropdownPopupPositionProvider(
            anchorOffset = offset,
            anchorSize = IntSize(itemWidth, itemHeight),
            screenMarginPx = screenMarginPx,
            onPositionCalculated = { position, size ->
                menuPosition = position
                menuTransformOrigin = calculateDropdownMenuTransformOrigin(offset, IntSize(itemWidth, itemHeight), position, size)
            }
        )
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = { if (expanded) onDismissRequest() },
        properties = PopupProperties(focusable = expanded)
    ) {
        Surface(
            modifier = modifier
                .width(180.dp)
                .graphicsLayer {
                    alpha = menuAlpha.value
                    scaleX = menuScale.value
                    scaleY = menuScale.value
                    transformOrigin = menuTransformOrigin
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
                        enabled = expanded,
                        onClick = { centerInPopup ->
                            if (onOptionSelectedAt != null) {
                                onOptionSelectedAt(option, Offset(menuPosition.x.toFloat(), menuPosition.y.toFloat()) + centerInPopup)
                            } else {
                                onOptionSelected(option)
                            }
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
    private val screenMarginPx: Int,
    private val onPositionCalculated: (IntOffset, IntSize) -> Unit
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val position = calculateDropdownMenuPosition(
            anchorOffset = anchorOffset,
            anchorSize = anchorSize,
            popupSize = popupContentSize,
            windowSize = windowSize,
            screenMarginPx = screenMarginPx
        )
        onPositionCalculated(position, popupContentSize)
        return position
    }
}

internal fun calculateDropdownMenuTransformOrigin(
    anchorOffset: IntOffset,
    anchorSize: IntSize,
    popupOffset: IntOffset,
    popupSize: IntSize
): TransformOrigin = TransformOrigin(
    pivotFractionX = ((anchorOffset.x + anchorSize.width - popupOffset.x).toFloat() / popupSize.width.coerceAtLeast(1)).coerceIn(0f, 1f),
    pivotFractionY = ((anchorOffset.y + anchorSize.height / 2f - popupOffset.y) / popupSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
)

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
    enabled: Boolean,
    onClick: (Offset) -> Unit,
    colors: ThemeColors = rememberThemeColors()
) {
    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .onGloballyPositioned { coordinates ->
                itemCoordinates = coordinates
            }
            .clip(RoundedCornerShape(12.dp))
            .then(if (isSelected) Modifier.background(colors.primary.copy(alpha = 0.1f)) else Modifier)
            .semantics { selected = isSelected }
            .clickable(enabled = enabled, role = Role.RadioButton) {
                val coordinates = itemCoordinates
                val centerInPopup = if (coordinates?.isAttached == true) {
                    coordinates.localToRoot(Offset(coordinates.size.width / 2f, coordinates.size.height / 2f))
                } else Offset.Zero
                onClick(centerInPopup)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = option.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W500
            ),
            color = if (isSelected) colors.primary else colors.onBackground
        )
        
        if (isSelected) {
            Spacer(Modifier.width(Spacing8))
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
