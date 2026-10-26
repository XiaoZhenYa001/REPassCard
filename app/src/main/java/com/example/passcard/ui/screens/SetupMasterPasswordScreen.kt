package com.example.passcard.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.passcard.ui.components.LoadingIndicator
import com.example.passcard.data.PasswordAssessment
import com.example.passcard.data.PasswordHealthAnalyzer
import com.example.passcard.data.PasswordWeakReason
import com.example.passcard.ui.theme.*
import com.example.passcard.util.PreferencesManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
    val scope = rememberCoroutineScope()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPwdVisible by remember { mutableStateOf(false) }
    var newPwdVisible by remember { mutableStateOf(false) }
    var confirmPwdVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showRemovalDialog by remember { mutableStateOf(false) }
    var removalPassword by remember { mutableStateOf("") }
    var removalPasswordVisible by remember { mutableStateOf(false) }
    var removalError by remember { mutableStateOf<String?>(null) }
    var isRemoving by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Tie a background result to its input so editing never displays the previous password's result.
    val strengthCheck by produceState<Pair<String, PasswordAssessment?>?>(initialValue = null, key1 = newPassword) {
        val candidate = newPassword
        value = null
        if (candidate.isEmpty()) return@produceState
        delay(120)
        try {
            val assessment = withContext(Dispatchers.Default) { PasswordHealthAnalyzer.assessPassword(candidate) }
            value = candidate to assessment
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            value = candidate to null
        }
    }
    val currentStrengthCheck = strengthCheck?.takeIf { it.first == newPassword }
    val currentAssessment = currentStrengthCheck?.second

    fun handleSave() {
        if (isSaving || isRemoving) return
        errorMessage = null
        successMessage = null

        if (newPassword.length < 4) {
            errorMessage = if (isZh) "密码至少 4 个字符" else "Password must be at least 4 characters"
            return
        }

        if (newPassword != confirmPassword) {
            errorMessage = if (isZh) "两次输入的密码不一致" else "Passwords do not match"
            return
        }

        val currentCandidate = currentPassword
        val newCandidate = newPassword
        isSaving = true
        scope.launch {
            var passwordSaved = false
            try {
                val currentPasswordValid = if (isEditing) {
                    withContext(Dispatchers.Default) {
                        preferencesManager.verifyMasterPassword(currentCandidate)
                    }
                } else {
                    true
                }

                if (!currentPasswordValid) {
                    currentPassword = ""
                    errorMessage = if (isZh) "当前密码错误" else "Current password is incorrect"
                } else {
                    withContext(Dispatchers.Default) {
                        preferencesManager.setMasterPassword(newCandidate)
                    }
                    passwordSaved = true
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                errorMessage = if (isZh) "暂时无法保存主密码，请重试" else "Unable to save master password. Try again."
            } finally {
                isSaving = false
            }

            if (passwordSaved) {
                onPasswordSet()
            }
        }
    }

    fun dismissRemoval() {
        showRemovalDialog = false
        removalPassword = ""
        removalPasswordVisible = false
        removalError = null
    }

    fun handleRemoval() {
        if (isRemoving || removalPassword.isEmpty()) return
        removalError = null
        val candidate = removalPassword
        removalPassword = ""
        isRemoving = true
        scope.launch {
            var removed = false
            try {
                removed = withContext(Dispatchers.Default) {
                    preferencesManager.clearMasterPassword(candidate)
                }
                if (!removed) {
                    removalError = if (isZh) "当前主密码错误，请重新输入" else "Current master password is incorrect. Try again."
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                removalError = if (isZh) "暂时无法移除主密码，请重试" else "Unable to remove master password. Try again."
            } finally {
                isRemoving = false
            }
            if (removed) {
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""
                dismissRemoval()
                onPasswordSet()
            }
        }
    }

    BackHandler(enabled = isSaving || isRemoving) { }

    if (showRemovalDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRemoving) dismissRemoval() },
            containerColor = themeColors.surface,
            titleContentColor = themeColors.onBackground,
            textContentColor = themeColors.onSurfaceVariant,
            title = { Text(if (isZh) "确认移除主密码" else "Remove master password?") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        if (isZh) "移除后，打开应用将不再验证主密码，指纹或人脸解锁也会关闭。请输入当前主密码以确认。"
                        else "After removal, opening the app will no longer require a master password, and biometric unlock will be turned off. Enter your current master password to confirm."
                    )
                    PasswordInputField(
                        value = removalPassword,
                        onValueChange = { removalPassword = it; removalError = null },
                        label = if (isZh) "当前主密码" else "Current master password",
                        placeholder = if (isZh) "输入当前主密码" else "Enter current master password",
                        visible = removalPasswordVisible,
                        onToggleVisible = { removalPasswordVisible = !removalPasswordVisible },
                        themeColors = themeColors,
                        enabled = !isRemoving,
                        imeAction = ImeAction.Done,
                        onDone = { handleRemoval() }
                    )
                    removalError?.let { Text(it, color = themeColors.error, style = MaterialTheme.typography.bodySmall) }
                    if (isRemoving) {
                        LoadingIndicator(
                            message = if (isZh) "正在验证主密码" else "Verifying master password",
                            compact = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { handleRemoval() },
                    enabled = removalPassword.isNotEmpty() && !isRemoving,
                    colors = ButtonDefaults.textButtonColors(contentColor = themeColors.error)
                ) {
                    Text(if (isZh) "验证并移除" else "Verify and remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissRemoval() }, enabled = !isRemoving) {
                    Text(if (isZh) "取消" else "Cancel")
                }
            }
        )
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
            IconButton(onClick = onBack, enabled = !isSaving && !isRemoving) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = if (isZh) "返回" else "Back",
                    tint = themeColors.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
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
                    enabled = !isSaving && !isRemoving,
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
                enabled = !isSaving && !isRemoving,
                focusRequester = if (!isEditing) focusRequester else null,
                imeAction = ImeAction.Next
            )

            // 密码强度指示器
            if (newPassword.isNotEmpty()) {
                when {
                    currentStrengthCheck == null -> LoadingIndicator(
                        message = if (isZh) "正在本机检查密码…" else "Checking password locally…",
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    currentAssessment == null -> Text(
                        text = if (isZh) "暂时无法评估密码，请使用独立的长随机密码或口令。" else "Unable to assess the password. Use a unique long random password or passphrase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant
                    )
                    else -> PasswordStrengthIndicator(assessment = currentAssessment, isZh = isZh, themeColors = themeColors)
                }
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
                enabled = !isSaving && !isRemoving,
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColors.primary,
                    contentColor = themeColors.onPrimary
                ),
                enabled = newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && !isSaving && !isRemoving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = themeColors.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
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
            }

            if (isSaving) {
                LoadingIndicator(
                    message = if (isZh) "正在保存主密码" else "Saving master password",
                    compact = true
                )
            }

            // 删除主密码按钮（仅修改时）
            if (isEditing) {
                OutlinedButton(
                    onClick = {
                        removalPassword = ""
                        removalPasswordVisible = false
                        removalError = null
                        showRemovalDialog = true
                    },
                    enabled = !isSaving && !isRemoving,
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
    enabled: Boolean = true,
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
            enabled = enabled,
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
                    IconButton(
                        onClick = onToggleVisible,
                        enabled = enabled,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (visible) "Hide password" else "Show password",
                            tint = themeColors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun PasswordStrengthIndicator(
    assessment: PasswordAssessment,
    isZh: Boolean,
    themeColors: ThemeColors
) {
    val (label, color, segments) = when {
        !assessment.isWeak -> Triple(if (isZh) "未发现明显弱模式" else "No obvious weak pattern", themeColors.success, 3)
        assessment.reasons.all { it == PasswordWeakReason.TOO_SHORT } -> Triple(if (isZh) "建议增加长度" else "Use a longer password", themeColors.warning, 2)
        else -> Triple(if (isZh) "建议改进" else "Needs improvement", themeColors.error, 1)
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
            text = "${if (isZh) "本地检查" else "Local check"}: $label",
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.W500
        )
        Text(
            text = if (assessment.isWeak) {
                assessment.reasons.joinToString(if (isZh) " · " else " / ") { it.healthDescription(isZh) } +
                    if (isZh) "。建议使用至少 15 个字符的随机密码或不相关词语组成的长口令。" else ". Use a random password of at least 15 characters or a long passphrase of unrelated words."
            } else {
                if (isZh) "仅本地启发式检查，不包含密码泄露查询。" else "Local heuristics only; breaches are not checked."
            },
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.onSurfaceVariant
        )
    }
}
