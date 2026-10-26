package com.example.passcard.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.*
import com.example.passcard.data.PasswordCategories

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
    enabled: Boolean = true,
    copyDescription: String = "复制密码",
    showDescription: String = "显示密码",
    hideDescription: String = "隐藏密码",
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
        verticalArrangement = Arrangement.spacedBy(Spacing8)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = themeColors.onSurfaceVariant
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isMultiline) Modifier.heightIn(min = 120.dp) else Modifier.height(56.dp))
                .appleSurface(colors = themeColors, radius = Radius16),
            contentAlignment = if (isMultiline) Alignment.TopStart else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing16, vertical = if (isMultiline) Spacing12 else 0.dp),
                contentAlignment = if (isMultiline) Alignment.TopStart else Alignment.CenterStart
            ) {
                val textStyle = InputTextStyle.copy(color = themeColors.onBackground)
                
                BasicTextField(
                    value = value,
                    enabled = enabled,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }.padding(end = if (isPassword) {
                        if (onCopy != null) 88.dp else 44.dp
                    } else 0.dp),
                    textStyle = textStyle,
                    cursorBrush = SolidColor(themeColors.primary),
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
                                    modifier = Modifier.padding(top = Spacing2)
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
                    .padding(end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPassword) {
                    onCopy?.let { copyAction ->
                        IconButton(onClick = copyAction, enabled = enabled) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = copyDescription,
                                tint = themeColors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = enabled) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) hideDescription else showDescription,
                            tint = themeColors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                trailingIcons()
            }
        }
    }
}

/**
 * 分类选择器组件
 * 分类标签与新建入口
 */
@Composable
fun CategorySelector(
    label: String,
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    categoryLabel: (String) -> String = { it },
    enabled: Boolean = true,
    onAddCategory: (() -> Unit)? = null,
    addCategoryText: String = "添加分类"
) {
    val themeColors = rememberThemeColors()
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing8)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = themeColors.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            onAddCategory?.let { addCategory ->
                TextButton(onClick = addCategory, enabled = enabled) { Text(addCategoryText) }
            }
        }
        
        // 分类标签行
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing8)
        ) {
            items(categories) { category ->
                val isSelected = PasswordCategories.matches(selectedCategory, category)
                CategoryTag(
                    label = categoryLabel(category),
                    selected = isSelected,
                    enabled = enabled,
                    onClick = { if (enabled) onCategorySelected(if (isSelected) "" else category) }
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
        verticalArrangement = Arrangement.spacedBy(Spacing12)
    ) {
        PressableScale(onClick = onChangeIcon) {
            PasswordIcon(
                label = name,
                iconType = iconType,
                iconValue = iconValue,
                size = 82.dp,
                cornerRadius = Radius24
            )
        }
        
        PressableScale(onClick = onChangeIcon) {
            Text(
                text = changeIconText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W700),
                color = themeColors.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius14))
                    .background(themeColors.primaryLight)
                    .padding(horizontal = Spacing14, vertical = Spacing8)
            )
        }
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
    
    PressableScale(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ActionButtonHeight)
                .clip(RoundedCornerShape(Radius16))
                .background(themeColors.errorContainer)
                .padding(horizontal = Spacing16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "删除",
                tint = themeColors.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing8))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
                color = themeColors.error
            )
        }
    }
}
