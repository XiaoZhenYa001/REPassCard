package com.example.passcard.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.theme.*
import com.example.passcard.util.PreferencesManager

/**
 * 设置/修改主密码界面
 */
@Composable
fun SetupMasterPasswordScreen(
    preferencesManager: PreferencesManager,
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    onPasswordSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    val isEditing = preferencesManager.hasMasterPassword

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPwdVisible by remember { mutableStateOf(false) }
    var newPwdVisible by remember { mutableStateOf(false) }
    var confirmPwdVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    // 密码强度评估
    val strength = remember(newPassword) { evaluatePasswordStrength(newPassword) }

    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    fun handleSave() {
        errorMessage = null
        successMessage = null

        // 如果已有密码，验证旧密码
        if (isEditing) {
            if (!preferencesManager.verifyMasterPassword(currentPassword)) {
                errorMessage = if (isZh) "当前密码错误" else "Current password is incorrect"
                return
            }
        }

        if (newPassword.length < 4) {
            errorMessage = if (isZh) "密码至少 4 个字符" else "Password must be at least 4 characters"
            return
        }

        if (newPassword != confirmPassword) {
            errorMessage = if (isZh) "两次输入的密码不一致" else "Passwords do not match"
            return
        }

        preferencesManager.setMasterPassword(newPassword)
        onPasswordSet()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .statusBarsPadding()
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = themeColors.onBackground,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (isEditing) {
                    if (isZh) "修改主密码" else "Change Master Password"
                } else {
                    if (isZh) "设置主密码" else "Set Master Password"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.W700),
                color = themeColors.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 顶部图标 + 说明
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(themeColors.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = themeColors.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isZh) "主密码用于保护您的所有密码数据" else "Master password protects all your data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 当前密码（仅修改时显示）
            if (isEditing) {
                PasswordInputField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; errorMessage = null },
                    label = if (isZh) "当前密码" else "Current Password",
                    placeholder = if (isZh) "输入当前主密码" else "Enter current password",
                    visible = currentPwdVisible,
                    onToggleVisible = { currentPwdVisible = !currentPwdVisible },
                    themeColors = themeColors,
                    focusRequester = focusRequester,
                    imeAction = ImeAction.Next
                )
            }

            // 新密码
            PasswordInputField(
                value = newPassword,
                onValueChange = { newPassword = it; errorMessage = null },
                label = if (isZh) "新密码" else "New Password",
                placeholder = if (isZh) "输入新主密码" else "Enter new master password",
                visible = newPwdVisible,
                onToggleVisible = { newPwdVisible = !newPwdVisible },
                themeColors = themeColors,
                focusRequester = if (!isEditing) focusRequester else null,
                imeAction = ImeAction.Next
            )

            // 密码强度指示器
            if (newPassword.isNotEmpty()) {
                PasswordStrengthIndicator(strength = strength, isZh = isZh, themeColors = themeColors)
            }

            // 确认密码
            PasswordInputField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = if (isZh) "确认密码" else "Confirm Password",
                placeholder = if (isZh) "再次输入新密码" else "Re-enter new password",
                visible = confirmPwdVisible,
                onToggleVisible = { confirmPwdVisible = !confirmPwdVisible },
                themeColors = themeColors,
                imeAction = ImeAction.Done,
                onDone = { handleSave() }
            )

            // 错误/成功信息
            errorMessage?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeColors.errorContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = themeColors.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = it, color = themeColors.error, fontSize = 13.sp)
                }
            }

            successMessage?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeColors.successContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = themeColors.success, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = it, color = themeColors.success, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 保存按钮
            Button(
                onClick = { handleSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                enabled = newPassword.isNotEmpty() && confirmPassword.isNotEmpty()
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) {
                        if (isZh) "更新密码" else "Update Password"
                    } else {
                        if (isZh) "设置密码" else "Set Password"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W600
                )
            }

            // 删除主密码按钮（仅修改时）
            if (isEditing) {
                OutlinedButton(
                    onClick = {
                        preferencesManager.clearMasterPassword()
                        preferencesManager.biometricEnabled = false
                        onPasswordSet()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.error),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, themeColors.error.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isZh) "移除主密码" else "Remove Master Password",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500
                    )
                }
            }
        }
    }
}

// ---- 内部组件 ----

@Composable
private fun PasswordInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    themeColors: ThemeColors,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.W600,
            color = themeColors.onSurfaceVariant
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(themeColors.surfaceVariant)
                .border(1.dp, themeColors.border, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = themeColors.onBackground,
                fontWeight = FontWeight.W500
            ),
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone?.invoke() }
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(text = placeholder, color = themeColors.muted, fontSize = 15.sp)
                        }
                        innerTextField()
                    }
                    Icon(
                        imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "Toggle",
                        tint = themeColors.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggleVisible() }
                    )
                }
            }
        )
    }
}

private enum class PasswordStrength { WEAK, MEDIUM, STRONG }

private fun evaluatePasswordStrength(password: String): PasswordStrength {
    if (password.length < 4) return PasswordStrength.WEAK
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return when {
        score >= 3 -> PasswordStrength.STRONG
        score >= 2 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.WEAK
    }
}

@Composable
private fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    isZh: Boolean,
    themeColors: ThemeColors
) {
    val (label, color, segments) = when (strength) {
        PasswordStrength.WEAK -> Triple(
            if (isZh) "弱" else "Weak",
            themeColors.error,
            1
        )
        PasswordStrength.MEDIUM -> Triple(
            if (isZh) "中等" else "Medium",
            themeColors.warning,
            2
        )
        PasswordStrength.STRONG -> Triple(
            if (isZh) "强" else "Strong",
            themeColors.success,
            3
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(3) { index ->
                val segColor by animateColorAsState(
                    targetValue = if (index < segments) color else themeColors.border,
                    animationSpec = tween(300),
                    label = "strengthSeg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(segColor)
                )
            }
        }
        Text(
            text = "${if (isZh) "密码强度" else "Strength"}: $label",
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.W500
        )
    }
}
