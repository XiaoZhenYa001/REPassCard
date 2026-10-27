package com.example.passcard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.theme.*
import com.example.passcard.util.playPassCardClick

enum class TabItem { HOME, SECURITY, CLOUD, SETTINGS }

@Composable
fun TabBar(
    selectedTab: TabItem,
    isChinese: Boolean,
    onTabSelected: (TabItem) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val density = LocalDensity.current
    val animateSelection = LocalInputModeManager.current.inputMode == InputMode.Touch
    val navigationDuration = if (animateSelection) MotionDuration.Navigation else 0
    val tabCoordinates = remember { mutableMapOf<TabItem, LayoutCoordinates>() }
    val tabBounds = remember { mutableStateMapOf<TabItem, Rect>() }
    var barCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var addCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var addBounds by remember { mutableStateOf<Rect?>(null) }

    fun updateBounds() {
        val bar = barCoordinates?.takeIf { it.isAttached } ?: return
        tabCoordinates.forEach { (tab, coordinates) ->
            if (coordinates.isAttached) {
                tabBounds[tab] = bar.localBoundingBoxOf(coordinates, clipBounds = false)
            }
        }
        addCoordinates?.takeIf { it.isAttached }?.let { coordinates ->
            addBounds = bar.localBoundingBoxOf(coordinates, clipBounds = false)
        }
    }

    fun recordTabBounds(tab: TabItem, coordinates: LayoutCoordinates) {
        tabCoordinates[tab] = coordinates
        updateBounds()
    }

    val barHeight = maxOf(70.dp, 50.dp + with(LocalDensity.current) { 16.sp.toDp() })
    val barElevation = if (themeColors.isDark) 12.dp else 8.dp
    val ambientShadowColor = Color.Black.copy(alpha = if (themeColors.isDark) 0.35f else 0.06f)
    val spotShadowColor = Color.Black.copy(alpha = if (themeColors.isDark) 0.45f else 0.1f)
    val topHighlightColor = if (themeColors.isDark) Color.White.copy(alpha = 0.08f) else Color.Transparent

    Box(
        modifier = modifier
            .widthIn(max = 350.dp)
            .fillMaxWidth()
            .height(barHeight)
            .shadow(
                elevation = barElevation,
                shape = RoundedCornerShape(35.dp),
                ambientColor = ambientShadowColor,
                spotColor = spotShadowColor
            )
            .clip(RoundedCornerShape(35.dp))
            .background(themeColors.tabBarBackground)
            .onGloballyPositioned { coordinates ->
                barCoordinates = coordinates
                updateBounds()
            }
            .drawBehind {
                if (topHighlightColor.alpha > 0f) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                topHighlightColor,
                                topHighlightColor,
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
        val selectedBounds = tabBounds[selectedTab]
        val centerBounds = addBounds
        if (selectedBounds != null && centerBounds != null) {
            val capsuleOffset = animateOffsetAsState(
                targetValue = selectedBounds.topLeft,
                animationSpec = tween(navigationDuration, easing = MotionEasing.EnterExit),
                label = "navigation_capsule"
            )
            // Clip only the indicator layer, keeping the add button outside selection motion.
            Box(
                Modifier.matchParentSize().drawWithCache {
                    val indicatorClip = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(Offset.Zero, size))
                        addRect(Rect(centerBounds.left, 0f, centerBounds.right, size.height))
                    }
                    onDrawWithContent { clipPath(indicatorClip) { this@onDrawWithContent.drawContent() } }
                },
                contentAlignment = AbsoluteAlignment.TopLeft
            ) {
                Box(
                    Modifier
                        .size(
                            width = with(density) { selectedBounds.width.toDp() },
                            height = with(density) { selectedBounds.height.toDp() }
                        )
                        .graphicsLayer {
                            translationX = capsuleOffset.value.x
                            translationY = capsuleOffset.value.y
                        }
                        .background(themeColors.primaryLight, RoundedCornerShape(20.dp))
                )
            }
        }
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
                        view.playPassCardClick()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(TabItem.HOME)
                    }
                },
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                label = if (isChinese) "首页" else "Home",
                activeColor = themeColors.tabActive,
                inactiveColor = themeColors.tabInactive,
                animateSelection = animateSelection,
                onPositioned = { recordTabBounds(TabItem.HOME, it) },
                modifier = Modifier.weight(1f)
            )
            // 安全
            IOSTabButton(
                selected = selectedTab == TabItem.SECURITY,
                onClick = {
                    if (selectedTab != TabItem.SECURITY) {
                        view.playPassCardClick()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(TabItem.SECURITY)
                    }
                },
                selectedIcon = Icons.Filled.Shield,
                unselectedIcon = Icons.Outlined.Shield,
                label = if (isChinese) "安全" else "Security",
                activeColor = themeColors.tabActive,
                inactiveColor = themeColors.tabInactive,
                animateSelection = animateSelection,
                onPositioned = { recordTabBounds(TabItem.SECURITY, it) },
                modifier = Modifier.weight(1f)
            )

            // 中间 + 按钮
            Box(Modifier.onGloballyPositioned { coordinates ->
                addCoordinates = coordinates
                updateBounds()
            }) {
                IOSAddButton(
                    onClick = {
                        view.playPassCardClick()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddClick()
                    },
                    themeColors = themeColors,
                    contentDescription = if (isChinese) "添加密码" else "Add password"
                )
            }

            // 加密
            IOSTabButton(
                selected = selectedTab == TabItem.CLOUD,
                onClick = {
                    if (selectedTab != TabItem.CLOUD) {
                        view.playPassCardClick()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(TabItem.CLOUD)
                    }
                },
                selectedIcon = Icons.Filled.Cloud,
                unselectedIcon = Icons.Outlined.Cloud,
                label = if (isChinese) "备份" else "Backup",
                activeColor = themeColors.tabActive,
                inactiveColor = themeColors.tabInactive,
                animateSelection = animateSelection,
                onPositioned = { recordTabBounds(TabItem.CLOUD, it) },
                modifier = Modifier.weight(1f)
            )
            // 设置
            IOSTabButton(
                selected = selectedTab == TabItem.SETTINGS,
                onClick = {
                    if (selectedTab != TabItem.SETTINGS) {
                        view.playPassCardClick()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(TabItem.SETTINGS)
                    }
                },
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                label = if (isChinese) "设置" else "Settings",
                activeColor = themeColors.tabActive,
                inactiveColor = themeColors.tabInactive,
                animateSelection = animateSelection,
                onPositioned = { recordTabBounds(TabItem.SETTINGS, it) },
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
    animateSelection: Boolean,
    onPositioned: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tintColor = animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = tween(
            if (animateSelection) MotionDuration.Navigation else 0,
            easing = MotionEasing.EnterExit
        ),
        label = "navigation_tint"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 8.dp, horizontal = 2.dp)
            .onGloballyPositioned(onPositioned)
            .clip(RoundedCornerShape(20.dp))
            .semantics { this.selected = selected }
            .clickable(
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 4.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                val canvas = drawContext.canvas.nativeCanvas
                // Compose 1.6 ignores Offscreen for software View.draw at alpha=1.
                // Isolate this small glyph layer explicitly so SrcIn cannot tint the bar.
                val saved = if (!canvas.isHardwareAccelerated) {
                    canvas.saveLayer(0f, 0f, size.width, size.height, null)
                } else -1
                try {
                    drawContent()
                    drawRect(color = tintColor.value, blendMode = BlendMode.SrcIn)
                } finally {
                    if (saved >= 0) canvas.restoreToCount(saved)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.W600,
            color = Color.White
        )
    }
}

@Composable
private fun IOSAddButton(
    onClick: () -> Unit,
    themeColors: ThemeColors,
    contentDescription: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val touchInput = LocalInputModeManager.current.inputMode == InputMode.Touch

    // 按压弹性缩放
    val scale by animateFloatAsState(
        targetValue = if (isPressed && touchInput) MotionScale.Pressed else 1f,
        animationSpec = tween(
            durationMillis = if (!touchInput) 0 else if (isPressed) MotionDuration.Press else MotionDuration.Release,
            easing = MotionEasing.EnterExit
        ),
        label = "addScale"
    )

    val elevation = if (themeColors.isDark) 6.dp else 4.dp

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
            contentDescription = contentDescription,
            tint = themeColors.onPrimary,
            modifier = Modifier.size(26.dp)
        )
    }
}
