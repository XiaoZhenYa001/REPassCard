package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.*

data class EditUiState(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val category: String = "",
    val note: String = "",
    val isNew: Boolean = true,
    val passwordVisible: Boolean = false
)

val COMMON_CATEGORIES_ZH = listOf("全部", "社交媒体", "工作", "金融", "购物", "娱乐", "AI", "游戏", "教育", "其他")
val COMMON_CATEGORIES_EN = listOf("All", "Social Media", "Work", "Finance", "Shopping", "Entertainment", "AI", "Gaming", "Education", "Other")

@Composable
fun EditScreen(
    password: PasswordItem? = null,
    currentLanguage: AppLanguage = AppLanguage.CHINESE,
    onBack: () -> Unit,
    onSave: (PasswordItem) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    val categories = if (currentLanguage == AppLanguage.CHINESE) COMMON_CATEGORIES_ZH else COMMON_CATEGORIES_EN
    
    var uiState by remember {
        mutableStateOf(
            if (password != null) {
                EditUiState(
                    id = password.id,
                    name = password.name,
                    username = password.username,
                    phone = password.phone,
                    email = password.email,
                    password = password.password,
                    category = password.category,
                    note = password.note,
                    isNew = false
                )
            } else {
                EditUiState()
            }
        )
    }
    
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(47.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "9:41",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                color = themeColors.onBackground
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = themeColors.onBackground,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Text(
                text = if (uiState.isNew) AppStrings.addLogin(currentLanguage) else AppStrings.editLogin(currentLanguage),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
                color = themeColors.onBackground
            )
            Text(
                text = AppStrings.save(currentLanguage),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                color = Primary,
                modifier = Modifier.clickable {
                    val item = PasswordItem(
                        id = uiState.id.ifEmpty { System.currentTimeMillis().toString() },
                        name = uiState.name,
                        username = uiState.username,
                        phone = uiState.phone,
                        email = uiState.email,
                        password = uiState.password,
                        category = uiState.category,
                        note = uiState.note
                    )
                    onSave(item)
                }
            )
        }
        
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(themeColors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.name.take(1).uppercase().ifEmpty { "?" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Primary
                    )
                }
                Text(
                    text = AppStrings.changeIcon(currentLanguage),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                    color = themeColors.onBackground
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            EditInputField(
                label = AppStrings.name(currentLanguage),
                value = uiState.name,
                onValueChange = { uiState = uiState.copy(name = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "服务名称" else "Service name",
                colors = themeColors
            )
            EditInputField(
                label = AppStrings.username(currentLanguage),
                value = uiState.username,
                onValueChange = { uiState = uiState.copy(username = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "用户名或邮箱" else "Username or email",
                colors = themeColors
            )
            EditInputField(
                label = AppStrings.phone(currentLanguage),
                value = uiState.phone,
                onValueChange = { uiState = uiState.copy(phone = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "手机号码" else "Phone number",
                colors = themeColors
            )
            EditInputField(
                label = AppStrings.email(currentLanguage),
                value = uiState.email,
                onValueChange = { uiState = uiState.copy(email = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "邮箱地址" else "Email address",
                colors = themeColors
            )
            EditPasswordField(
                label = AppStrings.passwordLabel(currentLanguage),
                value = uiState.password,
                onValueChange = { uiState = uiState.copy(password = it) },
                visible = uiState.passwordVisible,
                onToggleVisibility = { uiState = uiState.copy(passwordVisible = !uiState.passwordVisible) },
                colors = themeColors
            )
            
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = AppStrings.category(currentLanguage),
                    style = MaterialTheme.typography.labelLarge,
                    color = themeColors.onBackground
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories.filter { it != "全部" && it != "All" }) { category ->
                        val isSelected = uiState.category == category
                        Box(
                            modifier = Modifier.height(36.dp).clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) Primary else themeColors.surface)
                                .clickable { uiState = uiState.copy(category = if (isSelected) "" else category) }
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
                if (uiState.category.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                            .background(themeColors.surface).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.CHINESE) "当前: " else "Current: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.onSurfaceVariant
                        )
                        Text(
                            text = uiState.category,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                            color = Primary
                        )
                    }
                }
            }
            
            EditInputField(
                label = AppStrings.note(currentLanguage),
                value = uiState.note,
                onValueChange = { uiState = uiState.copy(note = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "添加备注..." else "Add note...",
                isMultiline = true,
                colors = themeColors
            )
            
            if (!uiState.isNew) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                        .background(themeColors.errorContainer).clickable { onDelete() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = themeColors.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.deletePassword(currentLanguage),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                        color = themeColors.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EditInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isMultiline: Boolean = false,
    colors: ThemeColors = rememberThemeColors()
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.onBackground
        )
        Box(
            modifier = Modifier.fillMaxWidth()
                .then(if (isMultiline) Modifier.height(100.dp) else Modifier.height(56.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = if (isMultiline) 12.dp else 0.dp),
            contentAlignment = if (isMultiline) Alignment.TopStart else Alignment.CenterStart
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(text = placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.muted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W500, color = colors.onBackground),
                singleLine = !isMultiline,
                maxLines = if (isMultiline) 4 else 1
            )
        }
    }
}

@Composable
private fun EditPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    colors: ThemeColors = rememberThemeColors()
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.onBackground
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W500, color = colors.onBackground),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true
                )
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide" else "Show",
                    tint = colors.onBackground,
                    modifier = Modifier.size(20.dp).clickable { onToggleVisibility() }
                )
            }
        }
    }
}
