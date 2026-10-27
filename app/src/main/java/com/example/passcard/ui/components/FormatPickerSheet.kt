package com.example.passcard.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.passcard.ui.screens.AppLanguage
import com.example.passcard.ui.theme.*

enum class ExportFormat { CSV, JSON }

/** The backdrop and sheet share a lifetime, including interrupted exits. */
@Composable
fun FormatPickerSheet(
    visible: Boolean,
    currentLanguage: AppLanguage,
    onFormatSelected: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = visible
    val transition = updateTransition(visibility, label = "format_sheet_visibility")
    if (!visibility.currentState && !visibility.targetState && visibility.isIdle) return

    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE

    val title = if (isZh) "选择导出格式" else "Export Format"
    val subtitle = if (isZh) "选择你偏好的文件格式" else "Choose your preferred file format"

    val backdropAlpha = transition.animateFloat(
        transitionSpec = { tween(if (targetState) MotionDuration.Standard else MotionDuration.Exit, easing = MotionEasing.EnterExit) },
        label = "format_sheet_backdrop"
    ) { if (it) 0.4f else 0f }
    val sheetOffset = transition.animateFloat(
        transitionSpec = { tween(if (targetState) MotionDuration.Sheet else MotionDuration.Emphasized, easing = MotionEasing.Drawer) },
        label = "format_sheet_offset"
    ) { if (it) 0f else 1f }

    Dialog(
        onDismissRequest = { if (visible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = visible,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        val dialogWindow = (dialogView.parent as DialogWindowProvider).window
        SideEffect {
            // The existing transition owns dimming and motion; the window owns modal focus.
            dialogWindow.setDimAmount(0f)
            dialogWindow.setWindowAnimations(0)
            WindowCompat.getInsetsController(dialogWindow, dialogView).apply {
                isAppearanceLightStatusBars = !themeColors.isDark
                isAppearanceLightNavigationBars = !themeColors.isDark
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier.matchParentSize()
                    .drawBehind { drawRect(Color.Black.copy(alpha = backdropAlpha.value)) }
                    .clickable(
                        enabled = visible,
                        onClickLabel = if (isZh) "关闭导出格式选择" else "Dismiss export format selection",
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = size.height * sheetOffset.value }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(themeColors.background)
                    .semantics { paneTitle = title }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing20)
                    .padding(top = Spacing12, bottom = Spacing20)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing16)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.width(36.dp).height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(themeColors.border)
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.W700), color = themeColors.onBackground)
                        Spacer(Modifier.height(Spacing4))
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss, enabled = visible) {
                        Icon(Icons.Outlined.Close, contentDescription = if (isZh) "关闭" else "Close", tint = themeColors.onSurfaceVariant)
                    }
                }
                FormatOptionCard(
                    icon = Icons.Outlined.TableChart,
                    title = "CSV",
                    description = if (isZh) "通用表格格式，兼容 Excel 和大多数密码管理器" else "Universal spreadsheet format, compatible with Excel",
                    tagText = if (isZh) "通用" else "Universal",
                    themeColors = themeColors,
                    enabled = visible,
                    onClick = { onFormatSelected(ExportFormat.CSV) }
                )
                FormatOptionCard(
                    icon = Icons.Outlined.DataObject,
                    title = "JSON",
                    description = if (isZh) "结构化数据格式，包含完整元信息，便于程序处理" else "Structured data format with metadata, ideal for apps",
                    tagText = if (isZh) "推荐" else "Recommended",
                    tagHighlight = true,
                    themeColors = themeColors,
                    enabled = visible,
                    onClick = { onFormatSelected(ExportFormat.JSON) }
                )
            }
        }
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
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val touchInput = LocalInputModeManager.current.inputMode == InputMode.Touch

    val scale = animateFloatAsState(
        targetValue = if (isPressed && enabled && touchInput) MotionScale.Pressed else 1f,
        animationSpec = tween(
            durationMillis = if (!touchInput) 0 else if (isPressed) MotionDuration.Press else MotionDuration.Release,
            easing = MotionEasing.EnterExit
        ),
        label = "formatCardScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(16.dp))
            .background(themeColors.surface)
            .clickable(
                enabled = enabled,
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
