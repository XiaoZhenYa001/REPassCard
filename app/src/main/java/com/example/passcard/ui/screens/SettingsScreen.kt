package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.components.*
import com.example.passcard.ui.theme.*

data class SettingsUiState(
    val userName: String = "Alex Morgan",
    val userEmail: String = "alex@example.com",
    val theme: String = "浅色",
    val soundEnabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToMasterPassword: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf(SettingsUiState()) }
    val themeColors = LocalThemeColors.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(themeColors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.displayLarge,
            color = themeColors.textPrimary
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(title = "账户", colors = themeColors)

            ProfileCard(
                userName = uiState.userName,
                userEmail = uiState.userEmail,
                onClick = onNavigateToProfile,
                colors = themeColors
            )

            SettingItem(
                icon = Icons.Outlined.Shield,
                label = "主密码",
                onClick = onNavigateToMasterPassword,
                colors = themeColors
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(title = "应用设置", colors = themeColors)

            SettingItem(
                icon = Icons.Outlined.DarkMode,
                label = "主题外观",
                trailingText = uiState.theme,
                onClick = onNavigateToTheme,
                colors = themeColors
            )

            SettingToggleItem(
                icon = Icons.Outlined.VolumeUp,
                label = "声音反馈",
                checked = uiState.soundEnabled,
                onCheckedChange = { uiState = uiState.copy(soundEnabled = it) },
                colors = themeColors
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(title = "数据管理", colors = themeColors)

            SettingItem(
                icon = Icons.Outlined.Upload,
                label = "导出密码",
                onClick = onNavigateToExport,
                colors = themeColors
            )

            SettingItem(
                icon = Icons.Outlined.Download,
                label = "导入密码",
                onClick = onNavigateToImport,
                colors = themeColors
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(title = "更多", colors = themeColors)

            SettingItem(
                icon = Icons.Outlined.HelpOutline,
                label = "使用帮助",
                onClick = onNavigateToHelp,
                colors = themeColors
            )

            SettingItem(
                icon = Icons.Outlined.Lock,
                label = "隐私条款",
                onClick = onNavigateToPrivacy,
                colors = themeColors
            )

            SettingItem(
                icon = Icons.Outlined.Info,
                label = "关于我们",
                onClick = onNavigateToAbout,
                colors = themeColors
            )
        }
    }
}
