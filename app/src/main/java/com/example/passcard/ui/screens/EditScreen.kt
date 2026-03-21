package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

/**
 * 编辑界面状态
 */
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

// 常用分类列表
val COMMON_CATEGORIES = listOf(
    "All",
    "Social Media",
    "Work",
    "Finance",
    "Shopping",
    "Entertainment",
    "AI",
    "Gaming",
    "Education",
    "Other"
)

@Composable
fun EditScreen(
    password: PasswordItem? = null,
    onBack: () -> Unit,
    onSave: (PasswordItem) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 如果有密码数据则编辑，否则新建
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
    
    // 是否显示分类选择器
    var showCategoryPicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Status Bar Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(47.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "9:41",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.W600
                ),
                color = TextPrimary
            )
        }
        
        // Nav Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            
            Text(
                text = if (uiState.isNew) "Add Login" else "Edit Login",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.W700
                ),
                color = TextPrimary
            )
            
            Text(
                text = "Save",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.W600
                ),
                color = Primary,
                modifier = Modifier.clickable {
                    // 保存
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
        
        // Scroll Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logo Selector
            Spacer(modifier = Modifier.height(4.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.name.take(1).uppercase().ifEmpty { "?" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Primary
                    )
                }
                
                Text(
                    text = "Change Icon",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.W600
                    ),
                    color = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Form Fields
            InputFieldEdit(
                label = "Name",
                value = uiState.name,
                onValueChange = { uiState = uiState.copy(name = it) },
                placeholder = "Service name"
            )
            
            InputFieldEdit(
                label = "Username",
                value = uiState.username,
                onValueChange = { uiState = uiState.copy(username = it) },
                placeholder = "Username or email"
            )
            
            InputFieldEdit(
                label = "Phone",
                value = uiState.phone,
                onValueChange = { uiState = uiState.copy(phone = it) },
                placeholder = "Phone number"
            )
            
            InputFieldEdit(
                label = "Email",
                value = uiState.email,
                onValueChange = { uiState = uiState.copy(email = it) },
                placeholder = "Email address"
            )
            
            // Password Field
            PasswordFieldEdit(
                label = "Password",
                value = uiState.password,
                onValueChange = { uiState = uiState.copy(password = it) },
                visible = uiState.passwordVisible,
                onToggleVisibility = { uiState = uiState.copy(passwordVisible = !uiState.passwordVisible) }
            )
            
            // Category Selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                
                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(COMMON_CATEGORIES.filter { it != "All" }) { category ->
                        val isSelected = uiState.category == category
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) Primary else Surface)
                                .clickable { 
                                    uiState = uiState.copy(
                                        category = if (isSelected) "" else category
                                    )
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
                
                // Selected category display
                if (uiState.category.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = uiState.category,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.W600
                            ),
                            color = Primary
                        )
                    }
                }
            }
            
            InputFieldEdit(
                label = "Note",
                value = uiState.note,
                onValueChange = { uiState = uiState.copy(note = it) },
                placeholder = "Add a note...",
                isMultiline = true
            )
            
            // Delete Button (only for existing items)
            if (!uiState.isNew) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ErrorContainer)
                        .clickable { onDelete() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Password",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.W600
                        ),
                        color = Error
                    )
                }
            }
        }
    }
}

@Composable
private fun InputFieldEdit(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isMultiline: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isMultiline) Modifier.height(100.dp) else Modifier.height(56.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(
                    horizontal = if (isMultiline) 16.dp else 16.dp,
                    vertical = if (isMultiline) 12.dp else 0.dp
                ),
            contentAlignment = if (isMultiline) Alignment.TopStart else Alignment.CenterStart
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted
                )
            }
            
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.W500,
                    color = TextPrimary
                ),
                singleLine = !isMultiline,
                maxLines = if (isMultiline) 4 else 1
            )
        }
    }
}

@Composable
private fun PasswordFieldEdit(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.W500,
                        color = TextPrimary
                    ),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true
                )
                
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide" else "Show",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onToggleVisibility() }
                )
            }
        }
    }
}
