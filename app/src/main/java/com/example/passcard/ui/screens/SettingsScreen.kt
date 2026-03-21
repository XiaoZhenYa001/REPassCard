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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.passcard.ui.components.*
import com.example.passcard.ui.theme.*

data class SettingsUiState(
    val userName: String = "Alex Morgan",
    val userEmail: String = "alex@example.com",
    val theme: String = "浅色",
    val soundEnabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
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
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
            
            // Scroll Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Header
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary
                )
                
                // Account Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionTitle(title = "账户")
                    
                    ProfileCard(
                        userName = uiState.userName,
                        userEmail = uiState.userEmail,
                        onClick = onNavigateToProfile
                    )
                    
                    SettingItem(
                        icon = Icons.Outlined.Shield,
                        label = "主密码",
                        onClick = onNavigateToMasterPassword
                    )
                }
                
                // App Settings Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionTitle(title = "应用设置")
                    
                    SettingItem(
                        icon = Icons.Outlined.DarkMode,
                        label = "主题外观",
                        trailingText = uiState.theme,
                        onClick = onNavigateToTheme
                    )
                    
                    SettingToggleItem(
                        icon = Icons.Outlined.VolumeUp,
                        label = "声音反馈",
                        checked = uiState.soundEnabled,
                        onCheckedChange = { uiState = uiState.copy(soundEnabled = it) }
                    )
                }
                
                // Data Management Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionTitle(title = "数据管理")
                    
                    SettingItem(
                        icon = Icons.Outlined.Upload,
                        label = "导出密码",
                        onClick = onNavigateToExport
                    )
                    
                    SettingItem(
                        icon = Icons.Outlined.Download,
                        label = "导入密码",
                        onClick = onNavigateToImport
                    )
                }
                
                // More Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionTitle(title = "更多")
                    
                    SettingItem(
                        icon = Icons.Outlined.HelpOutline,
                        label = "使用帮助",
                        onClick = onNavigateToHelp
                    )
                    
                    SettingItem(
                        icon = Icons.Outlined.Lock,
                        label = "隐私条款",
                        onClick = onNavigateToPrivacy
                    )
                    
                    SettingItem(
                        icon = Icons.Outlined.Info,
                        label = "关于我们",
                        onClick = onNavigateToAbout
                    )
                }
            }
        }
        
        // Bottom Tab Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .zIndex(1f)
        ) {
            TabBar(
                selectedTab = TabItem.SETTINGS,
                onTabSelected = { /* Handle tab selection */ },
                onAddClick = { /* Handle add */ },
                modifier = Modifier.shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(36.dp),
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
            )
        }
    }
}
