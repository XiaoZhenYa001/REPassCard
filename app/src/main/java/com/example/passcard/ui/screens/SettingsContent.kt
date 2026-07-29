package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.passcard.R
import com.example.passcard.ui.components.ProfileCard
import com.example.passcard.ui.components.SectionTitle
import com.example.passcard.ui.components.SettingItem
import com.example.passcard.ui.components.SettingToggleItem
import com.example.passcard.ui.theme.IconBgBlue
import com.example.passcard.ui.theme.IconBgCyan
import com.example.passcard.ui.theme.IconBgGray
import com.example.passcard.ui.theme.IconBgGreen
import com.example.passcard.ui.theme.IconBgLightGreen
import com.example.passcard.ui.theme.IconBgOrange
import com.example.passcard.ui.theme.IconBgPurple
import com.example.passcard.ui.theme.IconBgRed
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.ui.theme.Spacing10
import com.example.passcard.ui.theme.Spacing20
import com.example.passcard.ui.theme.Spacing28
import com.example.passcard.util.PreferencesManager

@Composable
fun SettingsContent(
    currentLanguage: AppLanguage,
    preferencesManager: PreferencesManager?,
    biometricEnabled: Boolean,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToExport: () -> Unit,
    onThemeDropdownToggle: (offset: IntOffset, size: IntSize) -> Unit,
    currentThemeLabel: String,
    onLanguageDropdownToggle: (offset: IntOffset, size: IntSize) -> Unit,
    currentLanguageLabel: String,
    onNavigateToHelp: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToMasterPassword: () -> Unit = {},
    onNavigateToRandomPassword: () -> Unit = {},
    scrollState: ScrollState = rememberScrollState()
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    var themeItemOffset by remember { mutableStateOf(IntOffset.Zero) }
    var themeItemSize by remember { mutableStateOf(IntSize.Zero) }
    var languageItemOffset by remember { mutableStateOf(IntOffset.Zero) }
    var languageItemSize by remember { mutableStateOf(IntSize.Zero) }
    var soundEnabled by remember { mutableStateOf(preferencesManager?.soundEnabled ?: true) }
    var clipboardClearEnabled by remember { mutableStateOf(preferencesManager?.clipboardClearEnabled ?: false) }
    var clipboardClearDelay by remember { mutableIntStateOf(preferencesManager?.clipboardClearDelay ?: 30) }
    var autoLockDelaySeconds by remember { mutableIntStateOf(preferencesManager?.autoLockDelaySeconds ?: 30) }
    var showClipboardDelayPicker by remember { mutableStateOf(false) }
    var showAutoLockDelayPicker by remember { mutableStateOf(false) }

    val autoLockDelayOptions = listOf(
        0 to (if (isZh) "立即" else "Immediately"),
        15 to (if (isZh) "15 秒" else "15 sec"),
        30 to (if (isZh) "30 秒" else "30 sec"),
        60 to (if (isZh) "1 分钟" else "1 min"),
        300 to (if (isZh) "5 分钟" else "5 min")
    )
    val currentAutoLockLabel = autoLockDelayOptions
        .firstOrNull { it.first == autoLockDelaySeconds }
        ?.second
        ?: (if (isZh) "30 秒" else "30 sec")

    val clipboardDelayOptions = listOf(
        15 to (if (isZh) "15 秒" else "15 sec"),
        30 to (if (isZh) "30 秒" else "30 sec"),
        60 to (if (isZh) "1 分钟" else "1 min"),
        300 to (if (isZh) "5 分钟" else "5 min")
    )
    val currentDelayLabel = clipboardDelayOptions.firstOrNull { it.first == clipboardClearDelay }?.second
        ?: (if (isZh) "${clipboardClearDelay} 秒" else "${clipboardClearDelay} sec")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing20, vertical = Spacing20),
        verticalArrangement = Arrangement.spacedBy(Spacing28)
    ) {
        Text(
            text = if (isZh) "设置" else "Settings",
            style = MaterialTheme.typography.displayLarge,
            color = themeColors.onBackground
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing10)) {
            SectionTitle(title = if (isZh) "保险库" else "Vault", colors = themeColors)
            ProfileCard(
                userName = if (isZh) "本地保险库" else "Local Vault",
                userEmail = if (isZh) "数据仅由本机与加密备份保存" else "Local data with encrypted backups",
                avatarResId = R.drawable.empty_records_icon,
                colors = themeColors
            )
            SettingItem(
                icon = Icons.Outlined.Shield,
                label = if (isZh) "主密码" else "Master Password",
                trailingText = if (preferencesManager?.hasMasterPassword == true) {
                    if (isZh) "已设置" else "Set"
                } else {
                    if (isZh) "未设置" else "Not set"
                },
                onClick = onNavigateToMasterPassword,
                iconBackgroundColor = IconBgBlue,
                colors = themeColors
            )
            if (preferencesManager?.hasMasterPassword == true) {
                SettingToggleItem(
                    icon = Icons.Outlined.Fingerprint,
                    label = if (isZh) "指纹解锁" else "Fingerprint Unlock",
                    checked = biometricEnabled,
                    onCheckedChange = onBiometricEnabledChange,
                    iconBackgroundColor = IconBgGreen,
                    colors = themeColors
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing10)) {
            SectionTitle(title = if (isZh) "应用设置" else "App Settings", colors = themeColors)
            SettingItem(
                icon = Icons.Outlined.DarkMode,
                label = if (isZh) "主题外观" else "Theme",
                trailingText = currentThemeLabel,
                onClick = { onThemeDropdownToggle(themeItemOffset, themeItemSize) },
                onPositioned = { offset, size -> themeItemOffset = offset; themeItemSize = size },
                iconBackgroundColor = IconBgOrange,
                colors = themeColors
            )
            SettingItem(
                icon = Icons.Outlined.Language,
                label = if (isZh) "语言" else "Language",
                trailingText = currentLanguageLabel,
                onClick = { onLanguageDropdownToggle(languageItemOffset, languageItemSize) },
                onPositioned = { offset, size -> languageItemOffset = offset; languageItemSize = size },
                iconBackgroundColor = IconBgPurple,
                colors = themeColors
            )
            SettingItem(
                icon = Icons.Outlined.Password,
                label = if (isZh) "随机密码" else "Random Password",
                trailingText = preferencesManager?.randomPasswordSpec?.length?.let { length ->
                    if (isZh) "${length} 位" else "$length chars"
                },
                onClick = onNavigateToRandomPassword,
                iconBackgroundColor = IconBgCyan,
                colors = themeColors
            )
            Box {
                SettingItem(
                    icon = Icons.Outlined.Lock,
                    label = if (isZh) "自动锁定" else "Auto Lock",
                    trailingText = currentAutoLockLabel,
                    onClick = { showAutoLockDelayPicker = !showAutoLockDelayPicker },
                    iconBackgroundColor = IconBgBlue,
                    colors = themeColors
                )
                DropdownMenu(
                    expanded = showAutoLockDelayPicker,
                    onDismissRequest = { showAutoLockDelayPicker = false },
                    modifier = Modifier.background(themeColors.surface)
                ) {
                    autoLockDelayOptions.forEach { (seconds, label) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    color = if (seconds == autoLockDelaySeconds) themeColors.primary else themeColors.onBackground,
                                    fontWeight = if (seconds == autoLockDelaySeconds) FontWeight.W700 else FontWeight.W400
                                )
                            },
                            onClick = {
                                autoLockDelaySeconds = seconds
                                preferencesManager?.autoLockDelaySeconds = seconds
                                showAutoLockDelayPicker = false
                            }
                        )
                    }
                }
            }
            SettingToggleItem(
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                label = if (isZh) "声音反馈" else "Sound Feedback",
                checked = soundEnabled,
                onCheckedChange = {
                    soundEnabled = it
                    preferencesManager?.soundEnabled = it
                },
                iconBackgroundColor = IconBgGreen,
                colors = themeColors
            )
            SettingToggleItem(
                icon = Icons.Outlined.ContentPaste,
                label = if (isZh) "自动清除剪贴板" else "Auto-clear Clipboard",
                checked = clipboardClearEnabled,
                onCheckedChange = {
                    clipboardClearEnabled = it
                    preferencesManager?.clipboardClearEnabled = it
                },
                iconBackgroundColor = IconBgRed,
                colors = themeColors
            )
            if (clipboardClearEnabled) {
                Box {
                    SettingItem(
                        icon = Icons.Outlined.Timer,
                        label = if (isZh) "清除延迟" else "Clear Delay",
                        trailingText = currentDelayLabel,
                        onClick = { showClipboardDelayPicker = !showClipboardDelayPicker },
                        iconBackgroundColor = IconBgOrange,
                        colors = themeColors
                    )
                    DropdownMenu(
                        expanded = showClipboardDelayPicker,
                        onDismissRequest = { showClipboardDelayPicker = false },
                        modifier = Modifier.background(themeColors.surface)
                    ) {
                        clipboardDelayOptions.forEach { (seconds, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        color = if (seconds == clipboardClearDelay) themeColors.primary else themeColors.onBackground,
                                        fontWeight = if (seconds == clipboardClearDelay) FontWeight.W700 else FontWeight.W400
                                    )
                                },
                                onClick = {
                                    clipboardClearDelay = seconds
                                    preferencesManager?.clipboardClearDelay = seconds
                                    showClipboardDelayPicker = false
                                }
                            )
                        }
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing10)) {
            SectionTitle(title = if (isZh) "数据管理" else "Data Management", colors = themeColors)
            SettingItem(icon = Icons.Outlined.Upload, label = if (isZh) "导出密码" else "Export Passwords", onClick = onNavigateToExport, iconBackgroundColor = IconBgLightGreen, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Download, label = if (isZh) "导入密码" else "Import Passwords", onClick = onNavigateToImport, iconBackgroundColor = IconBgBlue, colors = themeColors)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing10)) {
            SectionTitle(title = if (isZh) "更多" else "More", colors = themeColors)
            SettingItem(icon = Icons.AutoMirrored.Outlined.HelpOutline, label = if (isZh) "使用帮助" else "Help", onClick = onNavigateToHelp, iconBackgroundColor = IconBgOrange, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Lock, label = if (isZh) "隐私条款" else "Privacy Policy", onClick = onNavigateToPrivacy, iconBackgroundColor = IconBgGray, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Info, label = if (isZh) "关于" else "About", onClick = onNavigateToAbout, iconBackgroundColor = IconBgPurple, colors = themeColors)
        }
    }
}
