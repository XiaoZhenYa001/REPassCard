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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.*
import com.example.passcard.util.PasswordIconType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordListItem(
    name: String,
    email: String,
    password: String,
    iconText: String,
    iconType: String = PasswordIconType.GENERATED,
    iconValue: String = "",
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeColors = rememberThemeColors()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(themeColors.surface)
            .border(1.dp, themeColors.border, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = {}
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标区域 - 单击进入编辑
        PasswordIcon(
            label = name.ifBlank { iconText },
            iconType = iconType,
            iconValue = iconValue,
            size = 40.dp,
            cornerRadius = 8.dp
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 中间文字区域 - 双击复制密码
        Column(
            modifier = Modifier
                .weight(1f)
                .pointerInput(password) {
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
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                color = themeColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // 右侧箭头 - 单击进入编辑
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "更多",
            tint = themeColors.tabInactive,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    ClipboardHelper.copyToClipboard(context, text, label = "Password", showToast = true)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimplePasswordListItem(
    name: String,
    email: String,
    iconText: String,
    iconType: String = PasswordIconType.GENERATED,
    iconValue: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(themeColors.surface)
            .border(1.dp, themeColors.border, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {}
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PasswordIcon(
            label = name.ifBlank { iconText },
            iconType = iconType,
            iconValue = iconValue,
            size = 40.dp,
            cornerRadius = 8.dp
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                color = themeColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "更多",
            tint = themeColors.tabInactive,
            modifier = Modifier.size(20.dp)
        )
    }
}
