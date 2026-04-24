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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
    val showHelp: Boolean = false,
    val showPrivacy: Boolean = false,
    val showAbout: Boolean = false,
    val showCloudSync: Boolean = false,
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
    currentTheme: String = "LIGHT",
    languageKey: String = "CHINESE",
    passwords: List<PasswordItem> = emptyList(),
    onSavePassword: ((PasswordItem) -> Unit)? = null,
    onDeletePassword: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    MainContainer(
        preferencesManager = preferencesManager,
        onThemeChanged = onThemeChanged,
        currentTheme = currentTheme,
        languageKey = languageKey,
        passwords = passwords,
        onSavePassword = onSavePassword,
        onDeletePassword = onDeletePassword,
        modifier = modifier
    )
}

@Composable
fun MainContainer(
    preferencesManager: PreferencesManager? = null,
    onThemeChanged: (() -> Unit)? = null,
    currentTheme: String = "LIGHT",
    languageKey: String = "CHINESE",
    passwords: List<PasswordItem> = emptyList(),
    onSavePassword: ((PasswordItem) -> Unit)? = null,
    onDeletePassword: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentLanguage = remember(languageKey) {
        if (languageKey == "ENGLISH") AppLanguage.ENGLISH else AppLanguage.CHINESE
    }
    val themeColors = LocalThemeColors.current
    
    var uiState by remember { mutableStateOf(MainUiState(passwords = passwords)) }
    
    // 同步外部密码数据
    LaunchedEffect(passwords) {
        uiState = uiState.copy(passwords = passwords)
    }
    val context = LocalContext.current

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

        val currentThemeLabel = themeOptions.find { it.value == currentTheme }?.label
            ?: (if (currentLanguage == AppLanguage.CHINESE) "浅色" else "Light")
        val currentLanguageLabel = languageOptions.find { it.value == languageKey }?.label ?: "中文"

    val importFilePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { _: Uri? -> }
    val shareLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { }

    when {
            uiState.showEditScreen -> {
                val currentPassword = uiState.passwords.find { it.id == uiState.editPasswordId }
                EditScreen(
                    password = currentPassword,
                    currentLanguage = currentLanguage,
                    onBack = { uiState = uiState.copy(showEditScreen = false, editPasswordId = null) },
                    onSave = { updatedPassword ->
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
            }

            uiState.showImportPreview -> {
                ImportPreviewScreen(
                    entries = uiState.importEntries,
                    onConfirm = { uiState = uiState.copy(showImportPreview = false, importEntries = emptyList()) },
                    onCancel = { uiState = uiState.copy(showImportPreview = false, importEntries = emptyList()) }
                )
            }

            uiState.showAllPasswords -> {
                AllPasswordsScreen(
                    currentLanguage = currentLanguage,
                    onBack = { uiState = uiState.copy(showAllPasswords = false) },
                    passwords = uiState.passwords,
                    onPasswordClick = { id -> uiState = uiState.copy(showAllPasswords = false, showEditScreen = true, editPasswordId = id) }
                )
            }

            else -> {
                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    containerColor = themeColors.background,
                    bottomBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TabBar(
                                selectedTab = uiState.selectedTab,
                                onTabSelected = { tab -> uiState = uiState.copy(selectedTab = tab) },
                                onAddClick = { uiState = uiState.copy(showEditScreen = true, editPasswordId = null) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(themeColors.background)
                    ) {
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
                                onPasswordClick = { id -> uiState = uiState.copy(showEditScreen = true, editPasswordId = id) }
                            )

                            TabItem.SECURITY -> SecurityContent(currentLanguage)

                            TabItem.SETTINGS -> SettingsContent(
                                currentLanguage = currentLanguage,
                                onNavigateToImport = {
                                    importFilePickerLauncher.launch(
                                        arrayOf(
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "application/csv",
                                            "*/*"
                                        )
                                    )
                                },
                                onNavigateToExport = {
                                    val exportData = uiState.passwords.map { p ->
                                        ExportPasswordEntry(
                                            service = p.name,
                                            username = p.username,
                                            phone = p.phone,
                                            email = p.email,
                                            password = p.password,
                                            note = p.note,
                                            category = p.category
                                        )
                                    }
                                    val result = CsvExporter.exportToCsv(context, exportData)
                                    result.onSuccess { uri ->
                                        val shareIntent = CsvExporter.createShareIntent(uri, "passwords_export")
                                        shareLauncher.launch(Intent.createChooser(shareIntent, "Export Passwords"))
                                    }
                                },
                                showThemeDropdown = uiState.showThemeDropdown,
                                onThemeDropdownToggle = { offset, size ->
                                    uiState = uiState.copy(
                                        showThemeDropdown = !uiState.showThemeDropdown,
                                        themeDropdownOffset = offset,
                                        themeDropdownSize = size,
                                        showLanguageDropdown = false
                                    )
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
                                    uiState = uiState.copy(
                                        showLanguageDropdown = !uiState.showLanguageDropdown,
                                        languageDropdownOffset = offset,
                                        languageDropdownSize = size,
                                        showThemeDropdown = false
                                    )
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
                                onNavigateToHelp = { uiState = uiState.copy(showHelp = true) },
                                onNavigateToPrivacy = { uiState = uiState.copy(showPrivacy = true) },
                                onNavigateToAbout = { uiState = uiState.copy(showAbout = true) }
                            )

                            TabItem.CLOUD -> CloudSyncContent(currentLanguage, themeColors)
                        }

                        if (uiState.showHelp) {
                            HelpContent(currentLanguage = currentLanguage, onBack = { uiState = uiState.copy(showHelp = false) })
                        }
                        if (uiState.showPrivacy) {
                            PrivacyContent(currentLanguage = currentLanguage, onBack = { uiState = uiState.copy(showPrivacy = false) })
                        }
                        if (uiState.showAbout) {
                            AboutContent(currentLanguage = currentLanguage, onBack = { uiState = uiState.copy(showAbout = false) })
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
                    }
                }
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
    onPasswordClick: (String) -> Unit
) {
    val themeColors = LocalThemeColors.current

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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
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
                        .background(themeColors.iconBackground), contentAlignment = Alignment.Center) {
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
                        .background(OnPrimary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text(text = "🛡️", style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(text = secScoreText, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600), color = OnPrimary)
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
                        iconTextColor = themeColors.onBackground,
                        onClick = { onPasswordClick(password.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityContent(currentLanguage: AppLanguage) {
    val themeColors = LocalThemeColors.current

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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (currentLanguage == AppLanguage.CHINESE) "安全中心" else "Security Center", style = MaterialTheme.typography.titleLarge, color = themeColors.onBackground)
        }
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

@Suppress("UNUSED_PARAMETER")
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
    onNavigateToHelp: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val themeColors = LocalThemeColors.current

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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
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
            SettingItem(icon = Icons.Outlined.HelpOutline, label = helpLabel, onClick = onNavigateToHelp, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Lock, label = privacyLabel, onClick = onNavigateToPrivacy, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Info, label = aboutLabel, onClick = onNavigateToAbout, colors = themeColors)
        }
    }
}

// ============ 云同步页面 ============
// (See CloudSyncScreen.kt)

// ============ 帮助页面 ============
@Composable
fun HelpContent(currentLanguage: AppLanguage, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val themeColors = rememberThemeColors()
    val title = if (currentLanguage == AppLanguage.CHINESE) "使用帮助" else "Help"
    
    val helpItems = if (currentLanguage == AppLanguage.CHINESE) listOf(
        Triple("添加密码", "点击底部导航栏的 + 按钮添加新密码。", Icons.Outlined.Add),
        Triple("查看密码", "在密码列表中点击任意条目查看详情。", Icons.Outlined.Visibility),
        Triple("复制密码", "双击密码列表中的任意条目即可复制密码。", Icons.Outlined.ContentCopy),
        Triple("编辑密码", "点击密码详情页的编辑按钮修改信息。", Icons.Outlined.Edit),
        Triple("删除密码", "在编辑页面点击删除按钮移除密码。", Icons.Outlined.Delete),
        Triple("搜索密码", "在首页或全部密码页面使用搜索框。", Icons.Outlined.Search),
        Triple("分类管理", "为密码添加分类标签便于管理。", Icons.Outlined.Category),
        Triple("主题切换", "在设置中选择浅色或深色主题。", Icons.Outlined.DarkMode)
    ) else listOf(
        Triple("Add Password", "Tap + button to add a new password.", Icons.Outlined.Add),
        Triple("View Password", "Tap any item in the list to view details.", Icons.Outlined.Visibility),
        Triple("Copy Password", "Double-tap any item to copy the password.", Icons.Outlined.ContentCopy),
        Triple("Edit Password", "Tap edit button to modify information.", Icons.Outlined.Edit),
        Triple("Delete Password", "Tap delete button on the edit page.", Icons.Outlined.Delete),
        Triple("Search", "Use the search bar on home or all passwords page.", Icons.Outlined.Search),
        Triple("Categories", "Add category tags to organize passwords.", Icons.Outlined.Category),
        Triple("Theme", "Choose light or dark theme in settings.", Icons.Outlined.DarkMode)
    )
    
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Back", tint = themeColors.onBackground, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = themeColors.onBackground)
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            helpItems.forEach { (itemTitle, itemDesc, itemIcon) ->
                HelpItem(icon = itemIcon, title = itemTitle, description = itemDesc, themeColors = themeColors)
            }
        }
    }
}

@Composable
private fun HelpItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, themeColors: ThemeColors) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(themeColors.surface).padding(16.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = title, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600), color = themeColors.onBackground)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
        }
    }
}

// ============ 隐私页面 ============
@Composable
fun PrivacyContent(currentLanguage: AppLanguage, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val themeColors = rememberThemeColors()
    val title = if (currentLanguage == AppLanguage.CHINESE) "隐私条款" else "Privacy Policy"
    
    val sections = if (currentLanguage == AppLanguage.CHINESE) listOf(
        Pair("数据收集", "我们收集的最少信息仅用于应用功能。我们不会收集您的密码或敏感个人信息。"),
        Pair("本地存储", "所有密码数据仅存储在您的设备本地。我们不会将您的数据传输到任何服务器。"),
        Pair("加密保护", "您的密码使用 AES-256 加密算法保护，确保数据安全。"),
        Pair("第三方访问", "我们不会与任何第三方分享您的个人信息。"),
        Pair("数据权利", "您可以随时删除您的所有数据。应用卸载后，所有数据将被自动清除。"),
        Pair("政策更新", "我们可能会不时更新此隐私政策。任何更改都将在应用内通知。")
    ) else listOf(
        Pair("Data Collection", "We collect minimal information only for app functionality. We do not collect your passwords or sensitive personal data."),
        Pair("Local Storage", "All password data is stored locally on your device only. We do not transfer your data to any servers."),
        Pair("Encryption", "Your passwords are protected with AES-256 encryption to ensure data security."),
        Pair("Third Party Access", "We do not share your personal information with any third parties."),
        Pair("Data Rights", "You can delete all your data at any time. All data will be automatically cleared after app uninstall."),
        Pair("Policy Updates", "We may update this privacy policy from time to time. Any changes will be notified within the app.")
    )
    
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Back", tint = themeColors.onBackground, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = themeColors.onBackground)
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            sections.forEach { (secTitle, secContent) ->
                PrivacySection(title = secTitle, content = secContent, themeColors = themeColors)
            }
        }
    }
}

@Composable
private fun PrivacySection(title: String, content: String, themeColors: ThemeColors) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(themeColors.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600), color = themeColors.onBackground)
        Text(text = content, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
    }
}

// ============ 关于页面 ============
@Composable
fun AboutContent(currentLanguage: AppLanguage, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val themeColors = rememberThemeColors()
    val title = if (currentLanguage == AppLanguage.CHINESE) "关于我们" else "About Us"
    
    val appName = "PassCard"
    val version = "v1.0.0"
    val desc = if (currentLanguage == AppLanguage.CHINESE)
        "PassCard 是一款安全、简洁的密码管理应用。采用先进的加密技术，帮助您安全管理所有账户密码。"
    else
        "PassCard is a secure and simple password manager. Using advanced encryption technology to help you safely manage all your account passwords."
    
    val features = if (currentLanguage == AppLanguage.CHINESE) listOf(
        "• AES-256 本地加密", "• 生物识别解锁", "• 密码生成器", "• 分类管理", "• 导入/导出功能"
    ) else listOf(
        "• AES-256 Local Encryption", "• Biometric Unlock", "• Password Generator", "• Category Management", "• Import/Export"
    )
    
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Back", tint = themeColors.onBackground, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = themeColors.onBackground)
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(Primary), contentAlignment = Alignment.Center) {
                    Text(text = "🔐", style = MaterialTheme.typography.displayLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = appName, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = themeColors.onBackground)
                Text(text = version, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
            }
            Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(themeColors.surface).padding(20.dp)) {
                Text(text = if (currentLanguage == AppLanguage.CHINESE) "功能特点" else "Features", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600), color = themeColors.onBackground)
                Spacer(modifier = Modifier.height(12.dp))
                features.forEach { feature ->
                    Text(text = feature, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            Text(text = "© 2024 PassCard. " + (if (currentLanguage == AppLanguage.CHINESE) "保留所有权利。" else "All rights reserved."), style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
        }
    }
}


