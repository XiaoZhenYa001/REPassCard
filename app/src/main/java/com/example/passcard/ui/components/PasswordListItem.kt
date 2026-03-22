package com.example.passcard.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.*

/**
 * 可复制的密码列表项
 * - 点击左侧图标区域：进入编辑页面
 * - 点击右侧 > 箭头区域：进入编辑页面
 * - 双击中间区域（排除图标和箭头）：复制密码
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordListItem(
    name: String,
    email: String,
    password: String,
    iconText: String,
    iconBackgroundColor: Color,
    iconTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // 双击检测
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 300) {
                            // 双击 - 复制密码
                            copyToClipboard(context, password)
                        }
                        lastClickTime = currentTime
                    }
                )
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon (Clickable to edit)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBackgroundColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.W600
                ),
                color = iconTextColor
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Details (Double-click to copy)
        Column(
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            copyToClipboard(context, password)
                        }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.W600
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Chevron (Click to edit)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "更多",
                tint = TabInactive,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 复制文本到剪贴板并显示 Toast
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("密码", text)
    clipboard.setPrimaryClip(clip)
    
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, "密码已复制", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 简单的密码列表项（用于纯显示，无复制功能）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimplePasswordListItem(
    name: String,
    email: String,
    iconText: String,
    iconBackgroundColor: Color,
    iconTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {}
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.W600
                ),
                color = iconTextColor
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.W600
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "更多",
            tint = TabInactive,
            modifier = Modifier.size(20.dp)
        )
    }
}
