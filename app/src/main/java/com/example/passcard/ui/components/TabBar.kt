package com.example.passcard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.theme.*

enum class TabItem { HOME, SECURITY, CLOUD, SETTINGS }

@Composable
fun TabBar(
    selectedTab: TabItem,
    onTabSelected: (TabItem) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val haptic = LocalHapticFeedback.current

    val bgColor = themeColors.surface.copy(alpha = 0.88f)
    val indicatorColor = if (themeColors.isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val horizontalPadding = 8.dp
    val addButtonWidth = 64.dp

    BoxWithConstraints(
        modifier = modifier
            .width(345.dp)
            .height(72.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(36.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(36.dp))
            .background(bgColor)
    ) {
        val tabSlotWidth = (maxWidth - horizontalPadding * 2 - addButtonWidth) / 4
        val targetIndicatorOffset = when (selectedTab) {
            TabItem.HOME -> 0.dp
            TabItem.SECURITY -> tabSlotWidth
            TabItem.CLOUD -> tabSlotWidth * 2 + addButtonWidth
            TabItem.SETTINGS -> tabSlotWidth * 3 + addButtonWidth
        }
        val indicatorOffset by animateDpAsState(
            targetValue = targetIndicatorOffset,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "tabCapsuleOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = horizontalPadding + indicatorOffset, y = 8.dp)
                .width(tabSlotWidth)
                .height(56.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(indicatorColor)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton(
                selected = selectedTab == TabItem.HOME,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(TabItem.HOME)
                },
                icon = if (selectedTab == TabItem.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                label = "首页",
                modifier = Modifier.weight(1f)
            )
            TabButton(
                selected = selectedTab == TabItem.SECURITY,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(TabItem.SECURITY)
                },
                icon = if (selectedTab == TabItem.SECURITY) Icons.Filled.Shield else Icons.Outlined.Shield,
                label = "安全",
                modifier = Modifier.weight(1f)
            )

            AddButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddClick()
                }
            )

            TabButton(
                selected = selectedTab == TabItem.CLOUD,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(TabItem.CLOUD)
                },
                icon = if (selectedTab == TabItem.CLOUD) Icons.Filled.Cloud else Icons.Outlined.Cloud,
                label = "加密",
                modifier = Modifier.weight(1f)
            )
            TabButton(
                selected = selectedTab == TabItem.SETTINGS,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(TabItem.SETTINGS)
                },
                icon = if (selectedTab == TabItem.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                label = "设置",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "tabScale"
    )
    
    // 选中项颜色动画
    val labelColor by animateColorAsState(
        targetValue = if (selected) themeColors.onBackground else themeColors.tabInactive,
        animationSpec = tween(durationMillis = 200),
        label = "tabLabelColor"
    )
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(26.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = labelColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            color = labelColor
        )
    }
}

@Composable
private fun AddButton(
    onClick: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "addScale"
    )
    
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(themeColors.primary)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
