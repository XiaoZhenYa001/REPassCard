package com.example.passcard.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.screens.AppLanguage
import com.example.passcard.ui.theme.*

enum class ExportFormat { CSV, JSON }

/**
 * 导出格式选择底部弹窗
 * iOS 风格：遮罩淡入 + 卡片从底部滑入 + 内容交错动画
 */
@Composable
fun FormatPickerSheet(
    visible: Boolean,
    currentLanguage: AppLanguage,
    onFormatSelected: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE

    val title = if (isZh) "选择导出格式" else "Export Format"
    val subtitle = if (isZh) "选择你偏好的文件格式" else "Choose your preferred file format"

    // ---- 遮罩淡入/淡出 ----
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(260)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            // ---- 底部卡片从底部滑入 ----
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 380f
                    )
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(220)
                ) + fadeOut(animationSpec = tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(themeColors.background)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* 阻止点击穿透 */ }
                        .padding(horizontal = 24.dp)
                        .padding(top = 12.dp, bottom = 36.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 顶部拖拽指示条
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(themeColors.border)
                        )
                    }

                    // 标题 + 关闭按钮（带淡入动画）
                    SheetContentItem(delay = 60) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.W700),
                                    color = themeColors.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themeColors.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = themeColors.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { onDismiss() }
                            )
                        }
                    }

                    // CSV 卡片（交错延迟入场）
                    SheetContentItem(delay = 140) {
                        FormatOptionCard(
                            icon = Icons.Outlined.TableChart,
                            title = "CSV",
                            description = if (isZh)
                                "通用表格格式，兼容 Excel 和大多数密码管理器"
                            else
                                "Universal spreadsheet format, compatible with Excel",
                            tagText = if (isZh) "通用" else "Universal",
                            themeColors = themeColors,
                            onClick = { onFormatSelected(ExportFormat.CSV) }
                        )
                    }

                    // JSON 卡片（更大延迟入场）
                    SheetContentItem(delay = 220) {
                        FormatOptionCard(
                            icon = Icons.Outlined.DataObject,
                            title = "JSON",
                            description = if (isZh)
                                "结构化数据格式，包含完整元信息，便于程序处理"
                            else
                                "Structured data format with metadata, ideal for apps",
                            tagText = if (isZh) "推荐" else "Recommended",
                            tagHighlight = true,
                            themeColors = themeColors,
                            onClick = { onFormatSelected(ExportFormat.JSON) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 交错入场动画包装器
 * 每个子项带独立的延迟淡入 + 上移动画
 */
@Composable
private fun SheetContentItem(
    delay: Int,
    content: @Composable () -> Unit
) {
    var appeared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        appeared = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "itemAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (appeared) 0f else 20f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "itemOffsetY"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY * density
        }
    ) {
        content()
    }
}

@Composable
private fun FormatOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    tagText: String,
    tagHighlight: Boolean = false,
    themeColors: ThemeColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "formatCardScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isPressed) themeColors.primary.copy(alpha = 0.5f) else themeColors.border,
        animationSpec = tween(200),
        label = "formatCardBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(themeColors.surface)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标容器
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (tagHighlight)
                        themeColors.primary.copy(alpha = 0.1f)
                    else
                        themeColors.iconBackground
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (tagHighlight) themeColors.primary else themeColors.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 文字内容
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
                    color = themeColors.onBackground
                )
                // 标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (tagHighlight)
                                themeColors.primary.copy(alpha = 0.12f)
                            else
                                themeColors.surfaceVariant
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tagText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W600,
                        color = if (tagHighlight) themeColors.primary else themeColors.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
