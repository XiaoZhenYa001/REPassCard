package com.example.passcard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.passcard.ui.theme.*

/**
 * 下拉选择菜单选项
 */
data class DropdownOption(
    val label: String,
    val value: String
)

/**
 * 下拉选择菜单
 * @param offset 菜单位置（设置项在屏幕上的绝对位置）
 * @param itemWidth 设置项宽度（用于计算右下角）
 * @param itemHeight 设置项高度
 */
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
    itemHeight: Int = 60
) {
    val density = LocalDensity.current
    
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // 计算菜单位置：显示在设置项的右下角
            // X: 设置项右边缘 - 菜单宽度
            // Y: 设置项底部
            val menuWidthPx = with(density) { 180.dp.toPx().toInt() }
            val adjustedOffset = IntOffset(
                x = offset.x + itemWidth - menuWidthPx, // 右边缘对齐
                y = offset.y + itemHeight               // 下方
            )
            
            Popup(
                offset = adjustedOffset,
                onDismissRequest = onDismissRequest
            ) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                option = option,
                                isSelected = option.value == selectedValue,
                                onClick = {
                                    onOptionSelected(option)
                                    onDismissRequest()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单个下拉菜单项
 */
@Composable
private fun DropdownMenuItem(
    option: DropdownOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.background(Primary.copy(alpha = 0.1f))
                } else {
                    Modifier
                }
            )
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
            color = if (isSelected) Primary else TextPrimary
        )
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
