package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.components.*
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
    val isNew: Boolean = true
)

// EditUiState 的 Saver，用于 rememberSaveable 防止屏幕旋转数据丢失
val EditUiStateSaver = listSaver<EditUiState, Any>(
    save = { state ->
        listOf(
            state.id, state.name, state.username, state.phone, state.email,
            state.password, state.category, state.note, state.isNew
        )
    },
    restore = { list ->
        EditUiState(
            id = list[0] as String,
            name = list[1] as String,
            username = list[2] as String,
            phone = list[3] as String,
            email = list[4] as String,
            password = list[5] as String,
            category = list[6] as String,
            note = list[7] as String,
            isNew = list[8] as Boolean
        )
    }
)

val COMMON_CATEGORIES_ZH = listOf("社交媒体", "工作", "金融", "购物", "娱乐", "AI", "游戏", "教育", "其他")
val COMMON_CATEGORIES_EN = listOf("Social Media", "Work", "Finance", "Shopping", "Entertainment", "AI", "Gaming", "Education", "Other")

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
    val backInteractionSource = remember { MutableInteractionSource() }
    val saveInteractionSource = remember { MutableInteractionSource() }
    
    // 使用 rememberSaveable 防止屏幕旋转数据丢失
    var uiState by rememberSaveable(stateSaver = EditUiStateSaver) {
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
        // 顶部导航栏
        Row(
            modifier = Modifier.fillMaxWidth().height(100.dp).padding(top = 12.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 返回按钮 - 扩大点击区域并添加涟漪效果
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = backInteractionSource,
                        indication = rememberRipple(bounded = false, radius = 28.dp),
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = themeColors.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Text(
                text = if (uiState.isNew) AppStrings.addPassword(currentLanguage) else AppStrings.editPassword(currentLanguage),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
                color = themeColors.onBackground
            )
            
            // 保存按钮 - 扩大点击区域并添加涟漪效果
            Text(
                text = AppStrings.save(currentLanguage),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                color = Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = saveInteractionSource,
                        indication = rememberRipple(bounded = false, radius = 24.dp),
                        onClick = {
                            val item = PasswordItem(
                                id = uiState.id.ifEmpty { java.util.UUID.randomUUID().toString() },
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
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        
        // 滚动内容区
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            
            // 图标选择器
            LogoSelector(
                name = uiState.name,
                onChangeIcon = { /* TODO: 图标选择功能 */ },
                changeIconText = AppStrings.changeIcon(currentLanguage)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 名称输入
            InputField(
                label = AppStrings.name(currentLanguage),
                value = uiState.name,
                onValueChange = { uiState = uiState.copy(name = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "服务名称" else "Service name"
            )
            
            // 用户名输入
            InputField(
                label = AppStrings.username(currentLanguage),
                value = uiState.username,
                onValueChange = { uiState = uiState.copy(username = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "用户名或邮箱" else "Username or email"
            )
            
            // 手机号输入
            InputField(
                label = AppStrings.phone(currentLanguage),
                value = uiState.phone,
                onValueChange = { uiState = uiState.copy(phone = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "手机号码" else "Phone number"
            )
            
            // 邮箱输入
            InputField(
                label = AppStrings.email(currentLanguage),
                value = uiState.email,
                onValueChange = { uiState = uiState.copy(email = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "邮箱地址" else "Email address"
            )
            
            // 密码输入（带复制功能）
            InputField(
                label = AppStrings.passwordLabel(currentLanguage),
                value = uiState.password,
                onValueChange = { uiState = uiState.copy(password = it) },
                isPassword = true,
                onCopy = { /* TODO: 复制密码到剪贴板 */ },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "输入密码" else "Enter password"
            )
            
            // 分类选择器
            CategorySelector(
                label = AppStrings.category(currentLanguage),
                selectedCategory = uiState.category,
                categories = categories,
                onCategorySelected = { uiState = uiState.copy(category = it) },
                currentLanguageLabel = if (currentLanguage == AppLanguage.CHINESE) "当前: " else "Current: "
            )
            
            // 备注输入
            InputField(
                label = AppStrings.note(currentLanguage),
                value = uiState.note,
                onValueChange = { uiState = uiState.copy(note = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "添加备注..." else "Add note...",
                isMultiline = true
            )
            
            // 删除按钮（仅编辑模式显示）
            if (!uiState.isNew) {
                Spacer(modifier = Modifier.height(12.dp))
                DeleteButton(
                    text = AppStrings.deletePassword(currentLanguage),
                    onClick = onDelete
                )
            }
        }
    }
}
