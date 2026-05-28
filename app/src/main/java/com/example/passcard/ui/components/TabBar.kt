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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

    Box(
        modifier = modifier
            .width(350.dp)
            .height(70.dp)
            .shadow(
                elevation = if (themeColors.isDark) 20.dp else 12.dp,
                shape = RoundedCornerShape(35.dp),
                ambientColor = if (themeColors.isDark)
                    Color.Black.copy(alpha = 0.35f)
                else
                    Color.Black.copy(alpha = 0.06f),
                spotColor = if (themeColors.isDark)
                    Color.Black.copy(alpha = 0.45f)
                else
                    Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(35.dp))
            .background(themeColors.tabBarBackground)
            .drawBehind {
                // iOS 风格：暗色模式顶部微光线条
                if (themeColors.isDark) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.15f, 0.5f),
                        end = Offset(size.width * 0.85f, 0.5f),
                        strokeWidth = 0.5f
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 首页
            IOSTabButton(
                selected = selectedTab == TabItem.HOME,
                onClick = {
                    if (selectedTab != TabItem.HOME) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(TabItem.HOME)
                    }
                },
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                label = "首页",
                activeColor = themeColors.tabActive,
                inactiveColor = themeColors.tabInactive,
                modifier = Modifier.weight(1f)
            )
            // 安全
            IOSTabButton(
                selected = selectedTab == TabItem.SECURITY,
                onClick = {
                    if (selectedTab != TabItem.SECURITY) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(TabItem.SECURITY)
                    }
                },
                selectedIcon = Icons.Filled.Shield,
                unselectedIcon = Icons.Outlined.Shield,
                label = "安全",
                activeColor = themeColors.tabActive,
                inactiveColor = themeColors.tabInactive,
                modifier = Modifier.weight(1f)
            )

            // 中间 + 按钮
            IOSAddButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddClick()
                },
                themeColors = themeColors
            )

            // 加密
            IOSTabButton(
                selected = selectedTab == TabItem.CLOUD,
                onClick = {
                    if (selectedTab != TabItem.CLOUD) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(TabItem.CLOUD)
                    }
                },
                selectedIcon = Icons.Filled.Cloud,
                unselectedIcon = Icons.Outlined.Cloud,
                label = "加密",
                activeColor = themeColors.tabActive,
                inactiveColor = themeColors.tabInactive,
                modifier = Modifier.weight(1f)
            )
            // 设置
            IOSTabButton(
                selected = selectedTab == TabItem.SETTINGS,
                onClick = {
                    if (selectedTab != TabItem.SETTINGS) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(TabItem.SETTINGS)
                    }
                },
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                label = "设置",
                activeColor = themeColors.tabActive,
                inactiveColor = themeColors.tabInactive,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun IOSTabButton(
    selected: Boolean,
    onClick: () -> Unit,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按压缩放（弹性回弹）
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        label = "pressScale"
    )

    // 选中弹性放大
    val selectionScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "selectionScale"
    )

    // 选中时上移
    val offsetY by animateFloatAsState(
        targetValue = if (selected) -1f else 0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "offsetY"
    )

    // 颜色过渡
    val tintColor by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "tintColor"
    )

    // 图标尺寸
    val iconSize by animateFloatAsState(
        targetValue = if (selected) 22f else 20f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "iconSize"
    )

    // 标签透明度
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.55f,
        animationSpec = tween(140),
        label = "labelAlpha"
    )

    // 圆点指示器弹出
    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "dotScale"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .graphicsLayer {
                scaleX = pressScale * selectionScale
                scaleY = pressScale * selectionScale
                translationY = offsetY * density
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(iconSize.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.W600 else FontWeight.W400,
            color = tintColor,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha }
        )
        Spacer(modifier = Modifier.height(3.dp))
        // iOS 风格圆点指示器
        Box(
            modifier = Modifier
                .size(4.dp)
                .graphicsLayer {
                    scaleX = dotScale
                    scaleY = dotScale
                    alpha = dotScale
                }
                .clip(CircleShape)
                .background(tintColor)
        )
    }
}

@Composable
private fun IOSAddButton(
    onClick: () -> Unit,
    themeColors: ThemeColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按压弹性缩放
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        label = "addScale"
    )

    // 按压时阴影收缩
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 3.dp else if (themeColors.isDark) 10.dp else 8.dp,
        animationSpec = tween(120),
        label = "addElevation"
    )

    // 渐变色
    val gradientColors = if (themeColors.isDark) {
        listOf(
            themeColors.primary,
            themeColors.primary.copy(alpha = 0.8f)
        )
    } else {
        listOf(themeColors.primary, AddButtonGradientEnd)
    }

    Box(
        modifier = Modifier
            .size(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation,
                shape = CircleShape,
                ambientColor = themeColors.primary.copy(alpha = if (themeColors.isDark) 0.25f else 0.15f),
                spotColor = themeColors.primary.copy(alpha = if (themeColors.isDark) 0.35f else 0.25f)
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(gradientColors))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}
