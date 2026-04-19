package com.example.passcard.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.passcard.ui.components.*
import com.example.passcard.ui.theme.*
import com.example.passcard.util.CsvExporter
import com.example.passcard.util.ExportPasswordEntry
import com.example.passcard.util.PreferencesManager

data class MainUiState(
    val selectedTab: TabItem = TabItem.HOME,
    val showEditScreen: Boolean = false,
    val editPasswordId: String? = null,
    val showImportPreview: Boolean = false,
    val importEntries: List<ImportEntry> = emptyList(),
    val showAllPasswords: Boolean = false,
    val passwords: List<PasswordItem> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val showThemeDropdown: Boolean = false,
    val themeDropdownOffset: IntOffset = IntOffset.Zero,
    val themeDropdownSize: IntSize = IntSize.Zero,
    val showLanguageDropdown: Boolean = false,
    val languageDropdownOffset: IntOffset = IntOffset.Zero,
    val languageDropdownSize: IntSize = IntSize.Zero
)

@Composable
fun MainScreen(
    preferencesManager: PreferencesManager? = null,
    onThemeChanged: (() -> Unit)? = null,
    languageKey: String = "CHINESE",
    passwords: List<PasswordItem> = emptyList(),
    onSavePassword: ((PasswordItem) -> Unit)? = null,
    onDeletePassword: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentLanguage = remember(languageKey) {
        if (languageKey == "ENGLISH") AppLanguage.ENGLISH else AppLanguage.CHINESE
    }
    
    var uiState by remember { mutableStateOf(MainUiState(passwords = passwords)) }
    
    // 同步外部密码数据
    LaunchedEffect(passwords) {
        uiState = uiState.copy(passwords = passwords)
    }
    val context = LocalContext.current
    val currentTheme = preferencesManager?.theme ?: "LIGHT"
    val themeColors = rememberThemeColors()
    
    // 计算字符串（非 Composable）
    val welcomeText = if (currentLanguage == AppLanguage.CHINESE) "欢迎回来" else "Welcome Back"
    val searchPlaceholder = if (currentLanguage == AppLanguage.CHINESE) "搜索密码..." else "Search passwords..."
    val pwdCountText = if (currentLanguage == AppLanguage.CHINESE) "${uiState.passwords.size} 个密码" else "${uiState.passwords.size} Passwords"
    val secScoreText = if (currentLanguage == AppLanguage.CHINESE) "98% 安全" else "98% Secure"
    val recentText = if (currentLanguage == AppLanguage.CHINESE) "最近登录" else "Recent Logins"
    val viewAllText = if (currentLanguage == AppLanguage.CHINESE) "查看全部" else "View All"
    
    val themeOptions = listOf(
        DropdownOption(if (currentLanguage == AppLanguage.CHINESE) "浅色" else "Light", "LIGHT"),
        DropdownOption(if (currentLanguage == AppLanguage.CHINESE) "深色" else "Dark", "DARK"),
        DropdownOption(if (currentLanguage == AppLanguage.CHINESE) "跟随系统" else "System", "SYSTEM")
    )
    val languageOptions = listOf(
        DropdownOption("中文", "CHINESE"),
        DropdownOption("English", "ENGLISH")
    )
    
    val currentThemeLabel = themeOptions.find { it.value == currentTheme }?.label ?: (if (currentLanguage == AppLanguage.CHINESE) "浅色" else "Light")
    val currentLanguageLabel = languageOptions.find { it.value == languageKey }?.label ?: "中文"
    
    val importFilePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? -> }
    val shareLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { }
    
    if (uiState.showEditScreen) {
        val currentPassword = uiState.passwords.find { it.id == uiState.editPasswordId }
        EditScreen(
            password = currentPassword,
            currentLanguage = currentLanguage,
            onBack = { uiState = uiState.copy(showEditScreen = false, editPasswordId = null) },
            onSave = { updatedPassword ->
                // 如果是新密码，生成新 ID
                val itemToSave = if (uiState.editPasswordId == null) {
                    updatedPassword.copy(id = System.currentTimeMillis().toString())
                } else {
                    updatedPassword
                }
                onSavePassword?.invoke(itemToSave)
                uiState = uiState.copy(showEditScreen = false, editPasswordId = null)
            },
            onDelete = {
                uiState.editPasswordId?.let { onDeletePassword?.invoke(it) }
                uiState = uiState.copy(showEditScreen = false, editPasswordId = null)
            }
        )
        return
    }
    
    if (uiState.showImportPreview) {
        ImportPreviewScreen(
            entries = uiState.importEntries,
            onConfirm = { uiState = uiState.copy(showImportPreview = false, importEntries = emptyList()) },
            onCancel = { uiState = uiState.copy(showImportPreview = false, importEntries = emptyList()) }
        )
        return
    }
    
    if (uiState.showAllPasswords) {
        AllPasswordsScreen(
            currentLanguage = currentLanguage,
            onBack = { uiState = uiState.copy(showAllPasswords = false) },
            passwords = uiState.passwords,
            onPasswordClick = { id -> uiState = uiState.copy(showAllPasswords = false, showEditScreen = true, editPasswordId = id) }
        )
        return
    }
    
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (uiState.selectedTab) {
            TabItem.HOME -> HomeContent(
                passwords = uiState.passwords,
                selectedCategory = uiState.selectedCategory,
                searchQuery = uiState.searchQuery,
                currentLanguage = currentLanguage,
                welcomeText = welcomeText,
                searchPlaceholder = searchPlaceholder,
                pwdCountText = pwdCountText,
                secScoreText = secScoreText,
                recentText = recentText,
                viewAllText = viewAllText,
                onCategorySelected = { uiState = uiState.copy(selectedCategory = it) },
                onSearchQueryChange = { uiState = uiState.copy(searchQuery = it) },
                onNavigateToAllPasswords = { uiState = uiState.copy(showAllPasswords = true) },
                onPasswordClick = { id -> uiState = uiState.copy(showEditScreen = true, editPasswordId = id) },
                themeColors = themeColors
            )
            TabItem.SECURITY -> SecurityContent(currentLanguage, themeColors)
            TabItem.SETTINGS -> SettingsContent(
                currentLanguage = currentLanguage,
                onNavigateToImport = {
                    importFilePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "*/*"))
                },
                onNavigateToExport = {
                    val exportData = uiState.passwords.map { p ->
                        ExportPasswordEntry(service = p.name, username = p.username, phone = p.phone, email = p.email, password = p.password, note = p.note, category = p.category)
                    }
                    val result = CsvExporter.exportToCsv(context, exportData)
                    result.onSuccess { uri ->
                        val shareIntent = CsvExporter.createShareIntent(uri, "passwords_export")
                        shareLauncher.launch(Intent.createChooser(shareIntent, "Export Passwords"))
                    }
                },
                showThemeDropdown = uiState.showThemeDropdown,
                onThemeDropdownToggle = { offset, size ->
                    uiState = uiState.copy(showThemeDropdown = !uiState.showThemeDropdown, themeDropdownOffset = offset, themeDropdownSize = size, showLanguageDropdown = false)
                },
                onThemeDismiss = { uiState = uiState.copy(showThemeDropdown = false) },
                themeOptions = themeOptions,
                currentThemeValue = currentTheme,
                onThemeSelected = { option ->
                    preferencesManager?.theme = option.value
                    uiState = uiState.copy(showThemeDropdown = false)
                    onThemeChanged?.invoke()
                },
                currentThemeLabel = currentThemeLabel,
                showLanguageDropdown = uiState.showLanguageDropdown,
                onLanguageDropdownToggle = { offset, size ->
                    uiState = uiState.copy(showLanguageDropdown = !uiState.showLanguageDropdown, languageDropdownOffset = offset, languageDropdownSize = size, showThemeDropdown = false)
                },
                onLanguageDismiss = { uiState = uiState.copy(showLanguageDropdown = false) },
                languageOptions = languageOptions,
                currentLanguageValue = languageKey,
                onLanguageSelected = { option ->
                    preferencesManager?.language = option.value
                    uiState = uiState.copy(showLanguageDropdown = false)
                    onThemeChanged?.invoke()
                },
                currentLanguageLabel = currentLanguageLabel,
                themeColors = themeColors
            )
            TabItem.PLACEHOLDER -> PlaceholderContent(currentLanguage, themeColors)
        }
        
        if (uiState.showThemeDropdown) {
            DropdownSelectMenu(
                expanded = true,
                onDismissRequest = { uiState = uiState.copy(showThemeDropdown = false) },
                options = themeOptions,
                selectedValue = currentTheme,
                onOptionSelected = { option ->
                    preferencesManager?.theme = option.value
                    uiState = uiState.copy(showThemeDropdown = false)
                    onThemeChanged?.invoke()
                },
                offset = uiState.themeDropdownOffset,
                itemWidth = uiState.themeDropdownSize.width,
                itemHeight = uiState.themeDropdownSize.height
            )
        }
        
        if (uiState.showLanguageDropdown) {
            DropdownSelectMenu(
                expanded = true,
                onDismissRequest = { uiState = uiState.copy(showLanguageDropdown = false) },
                options = languageOptions,
                selectedValue = languageKey,
                onOptionSelected = { option ->
                    preferencesManager?.language = option.value
                    uiState = uiState.copy(showLanguageDropdown = false)
                    onThemeChanged?.invoke()
                },
                offset = uiState.languageDropdownOffset,
                itemWidth = uiState.languageDropdownSize.width,
                itemHeight = uiState.languageDropdownSize.height
            )
        }
        
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).zIndex(1f)
        ) {
            TabBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab -> uiState = uiState.copy(selectedTab = tab) },
                onAddClick = { uiState = uiState.copy(showEditScreen = true, editPasswordId = null) },
                modifier = Modifier.shadow(elevation = 12.dp, shape = RoundedCornerShape(36.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Black.copy(alpha = 0.1f))
            )
        }
    }
}

@Composable
private fun HomeContent(
    passwords: List<PasswordItem>,
    selectedCategory: String?,
    searchQuery: String,
    currentLanguage: AppLanguage,
    welcomeText: String,
    searchPlaceholder: String,
    pwdCountText: String,
    secScoreText: String,
    recentText: String,
    viewAllText: String,
    onCategorySelected: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToAllPasswords: () -> Unit,
    onPasswordClick: (String) -> Unit,
    themeColors: ThemeColors
) {
    val categories = if (currentLanguage == AppLanguage.CHINESE)
        listOf("全部", "社交媒体", "工作", "金融", "购物", "娱乐", "AI")
    else
        listOf("All", "Social Media", "Work", "Finance", "Shopping", "Entertainment", "AI")
    
    val filteredPasswords = passwords.filter { p ->
        val matchCat = selectedCategory == null || selectedCategory == "全部" || selectedCategory == "All" || p.category == selectedCategory
        val matchSearch = searchQuery.isEmpty() || p.name.contains(searchQuery, ignoreCase = true) ||
            p.username.contains(searchQuery, ignoreCase = true) || p.email.contains(searchQuery, ignoreCase = true) ||
            p.phone.contains(searchQuery, ignoreCase = true)
        matchCat && matchSearch
    }.take(5)
    
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(47.dp), contentAlignment = Alignment.Center) {
            Text(text = "9:41", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600), color = themeColors.onBackground)
        }
        
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = welcomeText, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
                    Text(text = "Alex Smith", style = MaterialTheme.typography.headlineMedium, color = themeColors.onBackground)
                }
            }
            
            SearchBar(value = searchQuery, onValueChange = onSearchQueryChange, placeholder = searchPlaceholder)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(140.dp).clip(RoundedCornerShape(16.dp))
                        .background(themeColors.surfaceVariant).clickable { onNavigateToAllPasswords() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp))
                            .background(if (themeColors.isDark) Color(0xFF333) else Color.White), contentAlignment = Alignment.Center) {
                            Text(text = "🔑", style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(text = pwdCountText, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600), color = themeColors.onBackground)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).height(140.dp).clip(RoundedCornerShape(16.dp)).background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text(text = "🛡️", style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(text = secScoreText, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600), color = Color.White)
                    }
                }
            }
            
            CategoryTagRow(categories = categories, selectedCategory = selectedCategory, onCategorySelected = onCategorySelected)
            
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = recentText, style = MaterialTheme.typography.titleMedium, color = themeColors.onBackground)
                    Text(text = viewAllText, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W500), color = Primary, modifier = Modifier.clickable { onNavigateToAllPasswords() })
                }
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredPasswords.forEach { password ->
                        PasswordListItem(
                            name = password.name,
                            email = password.email.ifEmpty { password.username },
                            password = password.password,
                            iconText = password.name.take(1).uppercase(),
                            iconBackgroundColor = themeColors.iconBackground,
                            iconTextColor = themeColors.onBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityContent(currentLanguage: AppLanguage, themeColors: ThemeColors) {
    val scoreLabel = if (currentLanguage == AppLanguage.CHINESE) "安全评分" else "Security Score"
    val scoreDesc = if (currentLanguage == AppLanguage.CHINESE) "您的密码健康状况良好，但有几项需要修复。" else "Your password health is good, but some items need attention."
    val totalLabel = if (currentLanguage == AppLanguage.CHINESE) "密码总数" else "Total Passwords"
    val weakLabel = if (currentLanguage == AppLanguage.CHINESE) "弱密码" else "Weak Passwords"
    val reusedLabel = if (currentLanguage == AppLanguage.CHINESE) "重复使用" else "Reused"
    val attentionLabel = if (currentLanguage == AppLanguage.CHINESE) "需要注意" else "Attention Needed"
    val compromisedTitle = if (currentLanguage == AppLanguage.CHINESE) "泄露密码" else "Compromised"
    val compromisedDesc = if (currentLanguage == AppLanguage.CHINESE) "个账户在数据泄露中发现" else "accounts found in breaches"
    val weakTitle = if (currentLanguage == AppLanguage.CHINESE) "弱密码" else "Weak Passwords"
    val weakDesc = if (currentLanguage == AppLanguage.CHINESE) "个账户需要更强的密码" else "accounts need stronger passwords"
    val suggestionLabel = if (currentLanguage == AppLanguage.CHINESE) "安全建议" else "Security Suggestions"
    val faTitle = if (currentLanguage == AppLanguage.CHINESE) "启用两步验证" else "Enable 2FA"
    val faDesc = if (currentLanguage == AppLanguage.CHINESE) "为您的主密码库账户添加额外的安全保护。" else "Add extra security to your vault account."
    
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(47.dp), contentAlignment = Alignment.Center) {
            Text(text = "9:41", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600), color = themeColors.onBackground)
        }
        Row(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Back", tint = themeColors.onBackground, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = if (currentLanguage == AppLanguage.CHINESE) "安全中心" else "Security Center", style = MaterialTheme.typography.titleLarge, color = themeColors.onBackground)
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            SecurityScoreCard(score = 85, description = scoreDesc)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecurityStatCard(icon = Icons.Outlined.Storage, value = "142", label = totalLabel, backgroundColor = themeColors.surfaceVariant, iconTint = themeColors.onSurfaceVariant, valueColor = themeColors.onBackground, labelColor = themeColors.onSurfaceVariant, modifier = Modifier.weight(1f))
                SecurityStatCard(icon = Icons.Outlined.Warning, value = "3", label = weakLabel, backgroundColor = themeColors.errorContainer, iconTint = themeColors.error, valueColor = themeColors.error, labelColor = themeColors.error, modifier = Modifier.weight(1f))
                SecurityStatCard(icon = Icons.Outlined.Refresh, value = "12", label = reusedLabel, backgroundColor = themeColors.warningContainer, iconTint = themeColors.warning, valueColor = themeColors.warning, labelColor = themeColors.warning, modifier = Modifier.weight(1f))
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = attentionLabel, style = MaterialTheme.typography.titleMedium, color = themeColors.onBackground)
                SecurityListItem(iconBackgroundColor = themeColors.errorContainer, icon = Icons.Outlined.LockOpen, iconTint = themeColors.error, title = compromisedTitle, description = "1 $compromisedDesc", onClick = { })
                SecurityListItem(iconBackgroundColor = themeColors.warningContainer, icon = Icons.Outlined.Warning, iconTint = themeColors.warning, title = weakTitle, description = "3 $weakDesc", onClick = { })
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = suggestionLabel, style = MaterialTheme.typography.titleMedium, color = themeColors.onBackground)
                SecuritySuggestionItem(title = faTitle, description = faDesc)
            }
        }
    }
}

@Composable
private fun SettingsContent(
    currentLanguage: AppLanguage,
    onNavigateToImport: () -> Unit,
    onNavigateToExport: () -> Unit,
    showThemeDropdown: Boolean,
    onThemeDropdownToggle: (offset: IntOffset, size: IntSize) -> Unit,
    onThemeDismiss: () -> Unit,
    themeOptions: List<DropdownOption>,
    currentThemeValue: String,
    onThemeSelected: (DropdownOption) -> Unit,
    currentThemeLabel: String,
    showLanguageDropdown: Boolean,
    onLanguageDropdownToggle: (offset: IntOffset, size: IntSize) -> Unit,
    onLanguageDismiss: () -> Unit,
    languageOptions: List<DropdownOption>,
    currentLanguageValue: String,
    onLanguageSelected: (DropdownOption) -> Unit,
    currentLanguageLabel: String,
    themeColors: ThemeColors
) {
    var themeItemOffset by remember { mutableStateOf(IntOffset.Zero) }
    var themeItemSize by remember { mutableStateOf(IntSize.Zero) }
    var languageItemOffset by remember { mutableStateOf(IntOffset.Zero) }
    var languageItemSize by remember { mutableStateOf(IntSize.Zero) }
    var soundEnabled by remember { mutableStateOf(true) }
    
    val settingsTitle = if (currentLanguage == AppLanguage.CHINESE) "设置" else "Settings"
    val accountTitle = if (currentLanguage == AppLanguage.CHINESE) "账户" else "Account"
    val appSettingsTitle = if (currentLanguage == AppLanguage.CHINESE) "应用设置" else "App Settings"
    val masterPwdLabel = if (currentLanguage == AppLanguage.CHINESE) "主密码" else "Master Password"
    val themeLabel = if (currentLanguage == AppLanguage.CHINESE) "主题外观" else "Theme"
    val langLabel = if (currentLanguage == AppLanguage.CHINESE) "语言" else "Language"
    val soundLabel = if (currentLanguage == AppLanguage.CHINESE) "声音反馈" else "Sound Feedback"
    val dataTitle = if (currentLanguage == AppLanguage.CHINESE) "数据管理" else "Data Management"
    val exportLabel = if (currentLanguage == AppLanguage.CHINESE) "导出密码" else "Export Passwords"
    val importLabel = if (currentLanguage == AppLanguage.CHINESE) "导入密码" else "Import Passwords"
    val moreTitle = if (currentLanguage == AppLanguage.CHINESE) "更多" else "More"
    val helpLabel = if (currentLanguage == AppLanguage.CHINESE) "使用帮助" else "Help"
    val privacyLabel = if (currentLanguage == AppLanguage.CHINESE) "隐私条款" else "Privacy Policy"
    val aboutLabel = if (currentLanguage == AppLanguage.CHINESE) "关于我们" else "About Us"
    
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 47.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
        Text(text = settingsTitle, style = MaterialTheme.typography.displayLarge, color = themeColors.onBackground)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(title = accountTitle, colors = themeColors)
            ProfileCard(userName = "Alex Morgan", userEmail = "alex@example.com", onClick = { }, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Shield, label = masterPwdLabel, onClick = { }, colors = themeColors)
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(title = appSettingsTitle, colors = themeColors)
            SettingItem(icon = Icons.Outlined.DarkMode, label = themeLabel, trailingText = currentThemeLabel, onClick = { onThemeDropdownToggle(themeItemOffset, themeItemSize) }, onPositioned = { offset, size -> themeItemOffset = offset; themeItemSize = size }, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Language, label = langLabel, trailingText = currentLanguageLabel, onClick = { onLanguageDropdownToggle(languageItemOffset, languageItemSize) }, onPositioned = { offset, size -> languageItemOffset = offset; languageItemSize = size }, colors = themeColors)
            SettingToggleItem(icon = Icons.Outlined.VolumeUp, label = soundLabel, checked = soundEnabled, onCheckedChange = { soundEnabled = it }, colors = themeColors)
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(title = dataTitle, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Upload, label = exportLabel, onClick = onNavigateToExport, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Download, label = importLabel, onClick = onNavigateToImport, colors = themeColors)
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(title = moreTitle, colors = themeColors)
            SettingItem(icon = Icons.Outlined.HelpOutline, label = helpLabel, onClick = { }, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Lock, label = privacyLabel, onClick = { }, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Info, label = aboutLabel, onClick = { }, colors = themeColors)
        }
    }
}

@Composable
private fun PlaceholderContent(currentLanguage: AppLanguage, themeColors: ThemeColors) {
    val title = if (currentLanguage == AppLanguage.CHINESE) "暂缺" else "Coming Soon"
    val desc = if (currentLanguage == AppLanguage.CHINESE) "此功能即将推出" else "This feature is coming soon"
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 47.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = themeColors.onBackground)
        Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
    }
}
