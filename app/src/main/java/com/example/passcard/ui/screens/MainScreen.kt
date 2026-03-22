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
import androidx.compose.ui.platform.LocalDensity
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
    // 首页数据
    val passwords: List<PasswordItem> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    // 设置状态 - 包含菜单位置和尺寸
    val showThemeDropdown: Boolean = false,
    val themeDropdownOffset: IntOffset = IntOffset.Zero,
    val themeDropdownSize: androidx.compose.ui.unit.IntSize = androidx.compose.ui.unit.IntSize.Zero,
    val showLanguageDropdown: Boolean = false,
    val languageDropdownOffset: IntOffset = IntOffset.Zero,
    val languageDropdownSize: androidx.compose.ui.unit.IntSize = androidx.compose.ui.unit.IntSize.Zero
)

@Composable
fun MainScreen(
    preferencesManager: PreferencesManager? = null,
    onThemeChanged: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 示例密码数据
    val samplePasswords = remember {
        listOf(
            PasswordItem(
                id = "1",
                name = "Google Account",
                username = "alex@gmail.com",
                email = "alex@gmail.com",
                password = "MySecretPassword123",
                category = "社交媒体",
                note = "主账号"
            ),
            PasswordItem(
                id = "2",
                name = "Netflix",
                username = "alex@gmail.com",
                email = "alex@gmail.com",
                password = "NetflixPass456",
                category = "娱乐",
                note = ""
            ),
            PasswordItem(
                id = "3",
                name = "Facebook",
                username = "alex.morgan",
                email = "alex@design.com",
                password = "FacebookPass789",
                category = "社交媒体",
                note = ""
            ),
            PasswordItem(
                id = "4",
                name = "Twitter",
                username = "alex_twitter",
                email = "",
                password = "TwitterPass000",
                category = "",
                note = ""
            ),
            PasswordItem(
                id = "5",
                name = "Amazon",
                username = "alex@amazon.com",
                email = "alex@amazon.com",
                password = "AmazonPass111",
                category = "购物",
                note = "Prime 会员"
            ),
            PasswordItem(
                id = "6",
                name = "芜职大教育企业邮箱",
                username = "李四",
                phone = "18888888888",
                email = "23000000@whit.edu.cn",
                password = "Me72916i!",
                category = "工作",
                note = "绑定了微信，和qq邮箱，手机号"
            ),
            PasswordItem(
                id = "7",
                name = "硅基流动",
                username = "",
                phone = "18888888888",
                email = "",
                password = "",
                category = "AI",
                note = "微信登陆"
            )
        )
    }
    
    var uiState by remember { mutableStateOf(MainUiState(passwords = samplePasswords)) }
    val context = LocalContext.current
    
    // 获取当前主题和语言设置
    val currentTheme = preferencesManager?.theme ?: "LIGHT"
    val currentLanguage = preferencesManager?.language ?: "CHINESE"
    
    // 主题选项
    val themeOptions = remember {
        listOf(
            DropdownOption("浅色", "LIGHT"),
            DropdownOption("深色", "DARK"),
            DropdownOption("跟随系统", "SYSTEM")
        )
    }
    
    // 语言选项
    val languageOptions = remember {
        listOf(
            DropdownOption("中文", "CHINESE"),
            DropdownOption("English", "ENGLISH")
        )
    }
    
    // 获取当前选中项的显示文本
    val currentThemeLabel = themeOptions.find { it.value == currentTheme }?.label ?: "浅色"
    val currentLanguageLabel = languageOptions.find { it.value == currentLanguage }?.label ?: "中文"
    
    // File picker launcher for import
    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        // TODO: Parse CSV file
    }
    
    // Share intent launcher for export
    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }
    
    // Edit Screen overlay
    if (uiState.showEditScreen) {
        val currentPassword = uiState.passwords.find { it.id == uiState.editPasswordId }
        EditScreen(
            password = currentPassword,
            onBack = { uiState = uiState.copy(showEditScreen = false, editPasswordId = null) },
            onSave = { updatedPassword ->
                val newList = uiState.passwords.map {
                    if (it.id == updatedPassword.id) updatedPassword else it
                }.let { list ->
                    if (uiState.editPasswordId == null) {
                        list + updatedPassword.copy(id = System.currentTimeMillis().toString())
                    } else list
                }
                uiState = uiState.copy(
                    showEditScreen = false,
                    editPasswordId = null,
                    passwords = newList
                )
            },
            onDelete = {
                val newList = uiState.passwords.filter { it.id != uiState.editPasswordId }
                uiState = uiState.copy(
                    showEditScreen = false,
                    editPasswordId = null,
                    passwords = newList
                )
            }
        )
        return
    }
    
    // Import Preview Screen
    if (uiState.showImportPreview) {
        ImportPreviewScreen(
            entries = uiState.importEntries,
            onConfirm = {
                uiState = uiState.copy(showImportPreview = false, importEntries = emptyList())
            },
            onCancel = {
                uiState = uiState.copy(showImportPreview = false, importEntries = emptyList())
            }
        )
        return
    }
    
    // All Passwords Screen
    if (uiState.showAllPasswords) {
        AllPasswordsScreen(
            onBack = { uiState = uiState.copy(showAllPasswords = false) },
            passwords = uiState.passwords,
            onPasswordClick = { id ->
                uiState = uiState.copy(showAllPasswords = false, showEditScreen = true, editPasswordId = id)
            }
        )
        return
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Page Content
        when (uiState.selectedTab) {
            TabItem.HOME -> HomeContent(
                passwords = uiState.passwords,
                selectedCategory = uiState.selectedCategory,
                searchQuery = uiState.searchQuery,
                onCategorySelected = { uiState = uiState.copy(selectedCategory = it) },
                onSearchQueryChange = { uiState = uiState.copy(searchQuery = it) },
                onNavigateToAllPasswords = { uiState = uiState.copy(showAllPasswords = true) },
                onPasswordClick = { id ->
                    uiState = uiState.copy(showEditScreen = true, editPasswordId = id)
                }
            )
            TabItem.SECURITY -> SecurityContent()
            TabItem.SETTINGS -> SettingsContent(
                onNavigateToImport = {
                    importFilePickerLauncher.launch(arrayOf(
                        "text/csv",
                        "text/comma-separated-values",
                        "application/csv",
                        "*/*"
                    ))
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
                // 主题下拉
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
                // 语言下拉
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
                currentLanguageValue = currentLanguage,
                onLanguageSelected = { option ->
                    preferencesManager?.language = option.value
                    uiState = uiState.copy(showLanguageDropdown = false)
                },
                currentLanguageLabel = currentLanguageLabel
            )
            TabItem.PLACEHOLDER -> PlaceholderContent()
        }
        
        // 下拉菜单 - 根据位置显示
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
                selectedValue = currentLanguage,
                onOptionSelected = { option ->
                    preferencesManager?.language = option.value
                    uiState = uiState.copy(showLanguageDropdown = false)
                },
                offset = uiState.languageDropdownOffset,
                itemWidth = uiState.languageDropdownSize.width,
                itemHeight = uiState.languageDropdownSize.height
            )
        }
        
        // Bottom Tab Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .zIndex(1f)
        ) {
            TabBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab -> 
                    uiState = uiState.copy(selectedTab = tab)
                },
                onAddClick = { 
                    uiState = uiState.copy(showEditScreen = true, editPasswordId = null)
                },
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

@Composable
private fun HomeContent(
    passwords: List<PasswordItem>,
    selectedCategory: String?,
    searchQuery: String,
    onCategorySelected: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToAllPasswords: () -> Unit,
    onPasswordClick: (String) -> Unit
) {
    val filteredPasswords = remember(passwords, selectedCategory, searchQuery) {
        passwords.filter { p ->
            val matchCategory = selectedCategory == null || 
                               selectedCategory == "All" || 
                               p.category == selectedCategory
            val matchSearch = searchQuery.isEmpty() ||
                            p.name.contains(searchQuery, ignoreCase = true) ||
                            p.username.contains(searchQuery, ignoreCase = true) ||
                            p.email.contains(searchQuery, ignoreCase = true) ||
                            p.phone.contains(searchQuery, ignoreCase = true) ||
                            p.note.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }.take(5)
    }
    
    val allCategories = remember(passwords) {
        listOf("All") + passwords.map { it.category }.filter { it.isNotEmpty() }.distinct()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
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
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "欢迎回来",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Alex Smith",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            
            SearchBar(
                value = searchQuery,
                onValueChange = onSearchQueryChange
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onNavigateToAllPasswords() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔑",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Text(
                            text = "${passwords.size} 个密码",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.W600
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🛡️",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Text(
                            text = "98% 安全",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.W600
                            ),
                            color = Color.White
                        )
                    }
                }
            }
            
            CategoryTagRow(
                categories = allCategories,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最近登录",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "查看全部",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.W500
                        ),
                        color = Primary,
                        modifier = Modifier.clickable { onNavigateToAllPasswords() }
                    )
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredPasswords.forEach { password ->
                        PasswordListItem(
                            name = password.name,
                            email = password.email.ifEmpty { password.username },
                            password = password.password,
                            iconText = password.name.take(1).uppercase(),
                            iconBackgroundColor = getIconBackgroundColor(password.name),
                            iconTextColor = getIconTextColor(password.name),
                            onClick = { onPasswordClick(password.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
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
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "Security Center",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            SecurityScoreCard(
                score = 85,
                description = "Your password health is looking good, but there are a few items to fix."
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecurityStatCard(
                    icon = Icons.Outlined.Storage,
                    value = "142",
                    label = "Total Passwords",
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    valueColor = MaterialTheme.colorScheme.onBackground,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                
                SecurityStatCard(
                    icon = Icons.Outlined.Warning,
                    value = "3",
                    label = "Weak Passwords",
                    backgroundColor = ErrorLight,
                    iconTint = Error,
                    valueColor = Error,
                    labelColor = Error,
                    modifier = Modifier.weight(1f)
                )
                
                SecurityStatCard(
                    icon = Icons.Outlined.Refresh,
                    value = "12",
                    label = "Reused",
                    backgroundColor = WarningContainer,
                    iconTint = Warning,
                    valueColor = Warning,
                    labelColor = Warning,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Attention Needed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                SecurityListItem(
                    iconBackgroundColor = ErrorContainer,
                    icon = Icons.Outlined.LockOpen,
                    iconTint = Error,
                    title = "Compromised Passwords",
                    description = "1 account found in data breaches",
                    onClick = { }
                )
                
                SecurityListItem(
                    iconBackgroundColor = WarningLight,
                    icon = Icons.Outlined.Warning,
                    iconTint = Warning,
                    title = "Weak Passwords",
                    description = "3 accounts need stronger passwords",
                    onClick = { }
                )
            }
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Security Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                SecuritySuggestionItem(
                    title = "Enable 2-Factor Auth",
                    description = "Add an extra layer of security to your main vault account."
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    onNavigateToImport: () -> Unit,
    onNavigateToExport: () -> Unit,
    // 主题下拉
    showThemeDropdown: Boolean,
    onThemeDropdownToggle: (offset: IntOffset, size: IntSize) -> Unit,
    onThemeDismiss: () -> Unit,
    themeOptions: List<DropdownOption>,
    currentThemeValue: String,
    onThemeSelected: (DropdownOption) -> Unit,
    currentThemeLabel: String,
    // 语言下拉
    showLanguageDropdown: Boolean,
    onLanguageDropdownToggle: (offset: IntOffset, size: IntSize) -> Unit,
    onLanguageDismiss: () -> Unit,
    languageOptions: List<DropdownOption>,
    currentLanguageValue: String,
    onLanguageSelected: (DropdownOption) -> Unit,
    currentLanguageLabel: String
) {
    // 用于获取设置项位置和尺寸的变量
    var themeItemOffset by remember { mutableStateOf(IntOffset.Zero) }
    var themeItemSize by remember { mutableStateOf(IntSize.Zero) }
    var languageItemOffset by remember { mutableStateOf(IntOffset.Zero) }
    var languageItemSize by remember { mutableStateOf(IntSize.Zero) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 47.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(title = "账户")
            
            ProfileCard(
                userName = "Alex Morgan",
                userEmail = "alex@example.com",
                onClick = { }
            )
            
            SettingItem(
                icon = Icons.Outlined.Shield,
                label = "主密码",
                onClick = { }
            )
        }
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(title = "应用设置")
            
            // 主题外观 - 获取位置并回调
            SettingItem(
                icon = Icons.Outlined.DarkMode,
                label = "主题外观",
                trailingText = currentThemeLabel,
                onClick = { onThemeDropdownToggle(themeItemOffset, themeItemSize) },
                onPositioned = { offset, size -> 
                    themeItemOffset = offset
                    themeItemSize = size
                }
            )
            
            // 语言 - 获取位置并回调
            SettingItem(
                icon = Icons.Outlined.Language,
                label = "语言",
                trailingText = currentLanguageLabel,
                onClick = { onLanguageDropdownToggle(languageItemOffset, languageItemSize) },
                onPositioned = { offset, size ->
                    languageItemOffset = offset
                    languageItemSize = size
                }
            )
            
            var soundEnabled by remember { mutableStateOf(true) }
            SettingToggleItem(
                icon = Icons.Outlined.VolumeUp,
                label = "声音反馈",
                checked = soundEnabled,
                onCheckedChange = { soundEnabled = it }
            )
        }
        
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
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(title = "更多")
            
            SettingItem(
                icon = Icons.Outlined.HelpOutline,
                label = "使用帮助",
                onClick = { }
            )
            
            SettingItem(
                icon = Icons.Outlined.Lock,
                label = "隐私条款",
                onClick = { }
            )
            
            SettingItem(
                icon = Icons.Outlined.Info,
                label = "关于我们",
                onClick = { }
            )
        }
    }
}

@Composable
private fun PlaceholderContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 47.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "暂缺",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "This tab is not yet implemented",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getIconBackgroundColor(name: String): Color {
    return when (name.lowercase()) {
        "google" -> IconBackground
        "netflix" -> Color.Black
        "facebook" -> Color(0xFF1877F2)
        "twitter" -> Color.Black
        "amazon" -> Color(0xFFFF9900)
        "twitter", "x" -> Color.Black
        else -> IconBackground
    }
}

private fun getIconTextColor(name: String): Color {
    return when (name.lowercase()) {
        "netflix" -> Color(0xFFE50914)
        "facebook" -> Color.White
        "twitter", "amazon" -> Color.White
        else -> TextPrimary
    }
}
