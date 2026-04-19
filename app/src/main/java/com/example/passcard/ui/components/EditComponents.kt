package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.theme.*

@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    isMultiline: Boolean = false,
    trailingIcons: @Composable RowScope.() -> Unit = {}
) {
    val themeColors = rememberThemeColors()
    var passwordVisible by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = themeColors.onBackground
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isMultiline) Modifier.height(120.dp) else Modifier.height(56.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(themeColors.surface),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500,
                    color = themeColors.onBackground
                )
                
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = textStyle,
                    visualTransformation = if (isPassword && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    singleLine = !isMultiline,
                    maxLines = if (isMultiline) 4 else 1,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = textStyle.copy(color = themeColors.muted),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
            
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPassword) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "复制",
                        tint = themeColors.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "隐藏" else "显示",
                        tint = themeColors.onBackground,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { passwordVisible = !passwordVisible }
                    )
                }
                trailingIcons()
            }
        }
    }
}

@Composable
fun CategorySelector(
    selectedCategory: String?,
    categories: List<String>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "分类",
            style = MaterialTheme.typography.labelLarge,
            color = themeColors.onBackground
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(themeColors.surface)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCategory != null) {
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColors.onSurfaceVariant.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = selectedCategory,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.W600),
                        color = themeColors.onBackground
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "移除",
                        tint = themeColors.onSurfaceVariant,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onCategorySelected("") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "选择",
                tint = themeColors.muted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LogoSelector(
    currentIcon: String,
    onIconChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(themeColors.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Key,
                contentDescription = "图标",
                tint = Primary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Text(
            text = "更换图标",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
            color = themeColors.onBackground,
            modifier = Modifier.clickable { onIconChange() }
        )
    }
}

@Composable
fun DeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(themeColors.errorContainer)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = "删除",
            tint = themeColors.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "删除密码",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
            color = themeColors.error
        )
    }
}
