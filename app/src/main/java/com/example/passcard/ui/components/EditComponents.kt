package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.theme.*

/**
 * 统一输入框组件
 * 支持单行/多行/密码模式
 */
@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    isMultiline: Boolean = false,
    onCopy: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcons: @Composable RowScope.() -> Unit = {}
) {
    val themeColors = rememberThemeColors()
    var passwordVisible by remember { mutableStateOf(false) }
    
    // 智能键盘类型
    val smartKeyboardOptions = remember(label, isPassword, isMultiline) {
        when {
            isPassword -> keyboardOptions.copy(keyboardType = KeyboardType.Password)
            label.contains("邮箱", ignoreCase = true) || label.equals("email", ignoreCase = true) -> 
                keyboardOptions.copy(keyboardType = KeyboardType.Email)
            label.contains("手机", ignoreCase = true) || label.contains("phone", ignoreCase = true) -> 
                keyboardOptions.copy(keyboardType = KeyboardType.Phone)
            label.contains("网址", ignoreCase = true) || label.contains("url", ignoreCase = true) -> 
                keyboardOptions.copy(keyboardType = KeyboardType.Uri)
            isMultiline -> keyboardOptions.copy(imeAction = ImeAction.Default)
            else -> keyboardOptions
        }
    }
    
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
                .then(if (isMultiline) Modifier.heightIn(min = 120.dp) else Modifier.height(56.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(themeColors.surface),
            contentAlignment = if (isMultiline) Alignment.TopStart else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = if (isMultiline) 12.dp else 0.dp),
                contentAlignment = if (isMultiline) Alignment.TopStart else Alignment.CenterStart
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
                    keyboardOptions = smartKeyboardOptions,
                    keyboardActions = keyboardActions,
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
                    onCopy?.let { copyAction ->
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "复制",
                            tint = themeColors.onBackground,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { copyAction() }
                        )
                    }
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

/**
 * 分类选择器组件
 * 包含分类标签行和当前选择显示
 */
@Composable
fun CategorySelector(
    label: String,
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    currentLanguageLabel: String = "当前: "
) {
    val themeColors = rememberThemeColors()
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = themeColors.onBackground
        )
        
        // 分类标签行
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) Primary else themeColors.surface)
                        .clickable { onCategorySelected(if (isSelected) "" else category) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White else themeColors.onBackground
                    )
                }
            }
        }
        
        // 当前选择显示
        if (selectedCategory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColors.surface)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentLanguageLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.onSurfaceVariant
                )
                Text(
                    text = selectedCategory,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                    color = Primary
                )
            }
        }
    }
}

/**
 * 图标选择器组件
 * 显示服务图标或名称首字母
 */
@Composable
fun LogoSelector(
    name: String,
    iconType: String,
    iconValue: String,
    onChangeIcon: () -> Unit,
    modifier: Modifier = Modifier,
    changeIconText: String = "更换图标"
) {
    val themeColors = rememberThemeColors()
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PasswordIcon(
            label = name,
            iconType = iconType,
            iconValue = iconValue,
            size = 80.dp,
            cornerRadius = 24.dp,
            modifier = Modifier.clickable { onChangeIcon() }
        )
        
        Text(
            text = changeIconText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
            color = themeColors.onBackground,
            modifier = Modifier.clickable { onChangeIcon() }
        )
    }
}

/**
 * 删除按钮组件
 */
@Composable
fun DeleteButton(
    text: String,
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
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
            color = themeColors.error
        )
    }
}
