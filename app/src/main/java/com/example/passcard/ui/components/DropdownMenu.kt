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
    itemHeight: Int = 60
) {
    val density = LocalDensity.current
    val themeColors = rememberThemeColors()
    
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            val menuWidthPx = with(density) { 180.dp.toPx().toInt() }
            val adjustedOffset = IntOffset(
                x = offset.x + itemWidth - menuWidthPx,
                y = offset.y + itemHeight
            )
            
            Popup(
                offset = adjustedOffset,
                onDismissRequest = onDismissRequest
            ) {
                Box(
                    modifier = Modifier.width(180.dp).clip(RoundedCornerShape(16.dp))
                        .background(themeColors.surface)
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                option = option,
                                isSelected = option.value == selectedValue,
                                onClick = {
                                    onOptionSelected(option)
                                    onDismissRequest()
                                },
                                colors = themeColors
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownMenuItem(
    option: DropdownOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    colors: ThemeColors = rememberThemeColors()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (isSelected) Modifier.background(colors.primary.copy(alpha = 0.1f)) else Modifier)
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
            color = if (isSelected) colors.primary else colors.onBackground
        )
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
