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
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
 * 位置：紧贴触发区域的右侧 + 向下延伸
 */
@Composable
fun DropdownSelectMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    options: List<DropdownOption>,
    selectedValue: String,
    onOptionSelected: (DropdownOption) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // 半透明遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { onDismissRequest() }
            )
            
            // 菜单内容
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = onDismissRequest
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = 0.dp, y = 48.dp)
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

/**
 * 带触发区域的下拉选择字段
 */
@Composable
fun DropdownSelectField(
    label: String,
    icon: ImageVector,
    selectedLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.W600
            ),
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = selectedLabel,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.W500
            ),
            color = OnSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = "More",
            tint = TabInactive,
            modifier = Modifier.size(20.dp)
        )
    }
}
