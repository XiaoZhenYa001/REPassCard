package com.example.passcard.ui.screens

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.passcard.ui.components.PressableScale
import com.example.passcard.ui.theme.*
import com.example.passcard.util.PreferencesManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 锁屏界面 — 输入主密码或指纹解锁
 */
@Composable
fun LockScreen(
    preferencesManager: PreferencesManager,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val context = LocalContext.current
    val isZh = preferencesManager.language == "CHINESE"
    val scope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shakeOffset by remember { mutableFloatStateOf(0f) }
    var isVerifying by remember { mutableStateOf(false) }
    var hasAutoPromptedBiometric by rememberSaveable { mutableStateOf(false) }

    // 抖动动画
    val shakeAnim by animateFloatAsState(
        targetValue = shakeOffset,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 800f),
        finishedListener = { shakeOffset = 0f },
        label = "shake"
    )

    // 自动弹出指纹
    fun attemptUnlock() {
        if (password.isEmpty() || isVerifying) return
        val candidate = password
        password = ""
        isVerifying = true
        scope.launch {
            val isValid = try {
                withContext(Dispatchers.Default) {
                    preferencesManager.verifyMasterPassword(candidate)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                errorMessage = if (isZh) "暂时无法验证主密码，请重试" else "Unable to verify master password. Try again."
                return@launch
            } finally {
                isVerifying = false
            }
            if (isValid) {
                onUnlocked()
            } else {
                errorMessage = if (isZh) "密码错误，请重试" else "Incorrect password, try again"
                shakeOffset = 16f
            }
        }
    }

    LaunchedEffect(preferencesManager.biometricEnabled) {
        if (preferencesManager.biometricEnabled && !hasAutoPromptedBiometric) {
            hasAutoPromptedBiometric = true
            triggerBiometric(context, preferencesManager, onUnlocked)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Spacing28),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 锁图标
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(themeColors.primaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Lock",
                tint = themeColors.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(Spacing24))

        Text(
            text = if (isZh) "欢迎回来" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.W700),
            color = themeColors.onBackground
        )

        Spacer(modifier = Modifier.height(Spacing8))

        Text(
            text = if (isZh) "请输入主密码解锁" else "Enter master password to unlock",
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing32))

        // 密码输入框（带抖动动画）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeAnim * density }
        ) {
            BasicTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .appleSurface(colors = themeColors, radius = Radius16)
                    .border(
                        width = if (errorMessage != null) 1.dp else 0.dp,
                        color = if (errorMessage != null) themeColors.error else Color.Transparent,
                        shape = RoundedCornerShape(Radius16)
                    )
                    .padding(horizontal = Spacing16),
                textStyle = InputTextStyle.copy(color = themeColors.onBackground),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { attemptUnlock() }),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = themeColors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing12))
                        Box(modifier = Modifier.weight(1f)) {
                            if (password.isEmpty()) {
                                Text(
                                    text = if (isZh) "输入主密码" else "Master password",
                                    color = themeColors.muted,
                                    style = InputTextStyle
                                )
                            }
                            innerTextField()
                        }
                        PressableScale(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = themeColors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        }

        // 错误提示
        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(Spacing8))
            Text(
                text = message,
                color = themeColors.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(Spacing20))

        // 解锁按钮
        Button(
            onClick = { attemptUnlock() },
            modifier = Modifier
                .fillMaxWidth()
                .height(ActionButtonHeight),
            shape = RoundedCornerShape(Radius16),
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
            enabled = password.isNotEmpty() && !isVerifying
        ) {
            if (isVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isZh) "解锁" else "Unlock",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.W700)
                )
            }
        }

        // 指纹解锁按钮（仅在开启时显示）
        if (preferencesManager.biometricEnabled) {
            Spacer(modifier = Modifier.height(Spacing24))

            PressableScale(
                enabled = !isVerifying,
                onClick = {
                    triggerBiometric(context, preferencesManager, onUnlocked)
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(themeColors.primaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = "Fingerprint",
                            tint = themeColors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing8))
                    Text(
                        text = if (isZh) "点击指纹解锁" else "Tap to use fingerprint",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 触发系统指纹/生物识别对话框
 */
private fun triggerBiometric(
    context: Context,
    prefs: PreferencesManager,
    onSuccess: () -> Unit
) {
    val isZh = prefs.language == "CHINESE"
    val activity = context.findFragmentActivity()
    if (activity == null) {
        Toast.makeText(
            context,
            if (isZh) "无法启动指纹验证" else "Unable to start biometric prompt",
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    val biometricManager = BiometricManager.from(context)
    val canAuth = biometricManager.canAuthenticate(authenticators)
    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
        val message = when (canAuth) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                if (isZh) "未录入指纹，请先在系统设置中添加" else "No biometrics enrolled"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                if (isZh) "设备不支持指纹" else "No biometric hardware"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                if (isZh) "指纹硬件不可用" else "Biometric hardware unavailable"
            else -> if (isZh) "指纹不可用" else "Biometric not available"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        return
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(if (isZh) "指纹解锁" else "Fingerprint Unlock")
        .setSubtitle(if (isZh) "使用指纹验证身份" else "Verify your identity")
        .setNegativeButtonText(if (isZh) "使用密码" else "Use Password")
        .setAllowedAuthenticators(authenticators)
        .build()

    val biometricPrompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(context),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val wasCancelled = errorCode == BiometricPrompt.ERROR_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                if (!wasCancelled) {
                    Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onAuthenticationFailed() {
                Toast.makeText(
                    context,
                    if (isZh) "指纹识别失败" else "Fingerprint not recognized",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    biometricPrompt.authenticate(promptInfo)
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return if (current is FragmentActivity) current else null
}
