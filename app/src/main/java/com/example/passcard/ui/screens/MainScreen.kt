package com.example.passcard.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.passcard.ui.components.*
import com.example.passcard.ui.theme.*
import com.example.passcard.util.CsvImporter
import com.example.passcard.util.CsvExporter
import com.example.passcard.util.ExportPasswordEntry
import com.example.passcard.util.ImportIssue
import com.example.passcard.util.ImportParseResult
import com.example.passcard.util.PreferencesManager
import com.example.passcard.util.JsonExporter
import com.example.passcard.util.JsonImporter
import com.example.passcard.util.FileFormatDetector

data class MainUiState(
    val selectedTab: TabItem = TabItem.HOME,
    val showEditScreen: Boolean = false,
    val editPasswordId: String? = null,
    val showImportPreview: Boolean = false,
    val importEntries: List<ImportEntry> = emptyList(),
    val importSelectedIds: Set<String> = emptySet(),
    val importIssues: List<ImportIssue> = emptyList(),
    val importReceipt: ImportReceiptUi? = null,
    val showImportReceipt: Boolean = false,
    val isImportBusy: Boolean = false,
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
    val languageDropdownSize: IntSize = IntSize.Zero,
    val showExportFormatPicker: Boolean = false,
    val showMasterPasswordSetup: Boolean = false
)

@Composable
fun MainScreen(
    preferencesManager: PreferencesManager? = null,
    onThemeChanged: (() -> Unit)? = null,
    currentTheme: String = "LIGHT",
    languageKey: String = "CHINESE",
    passwords: List<PasswordItem> = emptyList(),
    onSavePassword: ((PasswordItem) -> Unit)? = null,
    onImportPasswords: ((List<PasswordItem>) -> Unit)? = null,
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
        onImportPasswords = onImportPasswords,
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
    onImportPasswords: ((List<PasswordItem>) -> Unit)? = null,
    onDeletePassword: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentLanguage = remember(languageKey) {
        if (languageKey == "ENGLISH") AppLanguage.ENGLISH else AppLanguage.CHINESE
    }
    var displayedLanguage by remember { mutableStateOf(currentLanguage) }
    var contentVisible by remember { mutableStateOf(true) }

    LaunchedEffect(currentLanguage) {
        if (displayedLanguage != currentLanguage) {
            contentVisible = false
            delay(120)
            displayedLanguage = currentLanguage
            contentVisible = true
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "language_content_alpha"
    )

    val themeColors = LocalThemeColors.current
    
    var uiState by remember { mutableStateOf(MainUiState(passwords = passwords)) }
    var biometricEnabled by remember { mutableStateOf(preferencesManager?.biometricEnabled ?: false) }
    
    // 同步外部密码数据
    LaunchedEffect(passwords) {
        uiState = uiState.copy(passwords = passwords)
    }
    val context = LocalContext.current

        // 计算字符串（非 Composable）
        val welcomeText = if (displayedLanguage == AppLanguage.CHINESE) "欢迎回来" else "Welcome Back"
        val searchPlaceholder = if (displayedLanguage == AppLanguage.CHINESE) "搜索密码..." else "Search passwords..."
        val pwdCountText = if (displayedLanguage == AppLanguage.CHINESE) "${uiState.passwords.size} 个密码" else "${uiState.passwords.size} Passwords"
        val secScoreText = if (displayedLanguage == AppLanguage.CHINESE) "98% 安全" else "98% Secure"
        val recentText = if (displayedLanguage == AppLanguage.CHINESE) "最近登录" else "Recent Logins"
        val viewAllText = if (displayedLanguage == AppLanguage.CHINESE) "查看全部" else "View All"

        val themeOptions = listOf(
            DropdownOption(if (displayedLanguage == AppLanguage.CHINESE) "浅色" else "Light", "LIGHT"),
            DropdownOption(if (displayedLanguage == AppLanguage.CHINESE) "深色" else "Dark", "DARK"),
            DropdownOption(if (displayedLanguage == AppLanguage.CHINESE) "跟随系统" else "System", "SYSTEM")
        )
        val languageOptions = listOf(
            DropdownOption("中文", "CHINESE"),
            DropdownOption("English", "ENGLISH")
        )

        val currentThemeLabel = themeOptions.find { it.value == currentTheme }?.label
            ?: (if (displayedLanguage == AppLanguage.CHINESE) "浅色" else "Light")
        val currentLanguageLabel = languageOptions.find { it.value == languageKey }?.label ?: "中文"
    val scope = rememberCoroutineScope()
    val importMimeTypes = remember {
        arrayOf(
            "text/csv",
            "text/comma-separated-values",
            "application/csv",
            "text/tab-separated-values",
            "application/json",
            "text/json",
            "*/*"
        )
    }

    val closeImportPreview = {
        uiState = uiState.copy(
            showImportPreview = false,
            importEntries = emptyList(),
            importSelectedIds = emptySet(),
            importIssues = emptyList(),
            importReceipt = null,
            showImportReceipt = false,
            isImportBusy = false
        )
    }

    val commitSelectedImports: (Boolean) -> Unit = { closeAfterSuccess ->
        val startedAt = System.currentTimeMillis()
        val selectedEntries = uiState.importEntries.filter { it.id in uiState.importSelectedIds }
        if (selectedEntries.isEmpty()) {
            uiState = uiState.copy(
                showImportReceipt = true,
                importReceipt = buildNoSelectionReceipt(displayedLanguage)
            )
        } else {
            val existingKeys = uiState.passwords
                .map { buildImportKey(it.name, it.username) }
                .toMutableSet()
            val toInsert = mutableListOf<PasswordItem>()
            var duplicateSkipped = 0

            selectedEntries.forEachIndexed { index, entry ->
                val key = buildImportKey(entry.service, entry.username)
                if (key in existingKeys) {
                    duplicateSkipped++
                } else {
                    existingKeys.add(key)
                    toInsert.add(
                        PasswordItem(
                            id = "import_${System.currentTimeMillis()}_$index",
                            name = entry.service,
                            username = entry.username,
                            phone = entry.phone,
                            email = entry.email,
                            password = entry.password,
                            category = entry.category,
                            note = entry.note
                        )
                    )
                }
            }

            if (onImportPasswords != null) {
                onImportPasswords.invoke(toInsert)
            } else {
                toInsert.forEach { onSavePassword?.invoke(it) }
            }

            val receipt = buildImportDoneReceipt(
                importedCount = toInsert.size,
                duplicateSkipped = duplicateSkipped,
                parseIssueCount = uiState.importIssues.size,
                selectedCount = selectedEntries.size,
                durationMillis = System.currentTimeMillis() - startedAt,
                language = displayedLanguage
            )

            if (toInsert.isNotEmpty()) {
                val successMessage = if (displayedLanguage == AppLanguage.CHINESE) {
                    "导入成功：${toInsert.size} 条"
                } else {
                    "Import successful: ${toInsert.size} items"
                }
                Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
            }

            if (toInsert.isNotEmpty() && closeAfterSuccess) {
                closeImportPreview()
            } else {
                uiState = uiState.copy(
                    importReceipt = receipt,
                    showImportReceipt = true
                )
            }
        }
    }

    val importFilePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        val passwordSnapshot = uiState.passwords
        scope.launch {
            val startedAt = System.currentTimeMillis()
            uiState = uiState.copy(isImportBusy = true)

            // 自动检测文件格式并解析
            val parseResult = withContext(Dispatchers.IO) {
                try {
                    val (format, content) = FileFormatDetector.detectFromUri(context, uri)
                    when (format) {
                        FileFormatDetector.FileFormat.JSON -> JsonImporter.parseJsonContent(content).let { Result.success(it) }
                        FileFormatDetector.FileFormat.CSV -> CsvImporter.parseCsv(context, uri)
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            parseResult.onSuccess { parsed ->
                val existingKeys = passwordSnapshot.map { buildImportKey(it.name, it.username) }.toSet()
                val importKeys = mutableSetOf<String>()
                val entries = parsed.entries.mapIndexed { index, entry ->
                    val key = buildImportKey(entry.service, entry.username)
                    val duplicated = key in existingKeys || !importKeys.add(key)
                    ImportEntry(
                        id = "preview_${System.currentTimeMillis()}_$index",
                        service = entry.service,
                        username = entry.username,
                        phone = entry.phone,
                        email = entry.email,
                        password = entry.password,
                        note = entry.note,
                        category = entry.category,
                        sourceRow = entry.sourceRow,
                        isDuplicate = duplicated
                    )
                }

                val selectedIds = entries.filterNot { it.isDuplicate }.map { it.id }.toSet()
                val fileNameDisplay = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "file"
                uiState = uiState.copy(
                    isImportBusy = false,
                    showImportPreview = true,
                    importEntries = entries,
                    importSelectedIds = selectedIds,
                    importIssues = parsed.issues,
                    importReceipt = buildParseReceipt(
                        parseResult = parsed,
                        duplicateCount = entries.count { it.isDuplicate },
                        fileName = fileNameDisplay,
                        durationMillis = System.currentTimeMillis() - startedAt,
                        language = displayedLanguage
                    ),
                    showImportReceipt = true
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    isImportBusy = false,
                    showImportPreview = true,
                    importEntries = emptyList(),
                    importSelectedIds = emptySet(),
                    importIssues = listOf(
                        ImportIssue(
                            rowNumber = 0,
                            reason = error.message ?: "无法解析文件",
                            rawRow = ""
                        )
                    ),
                    importReceipt = buildParseFailureReceipt(
                        reason = error.message ?: "无法解析文件",
                        language = displayedLanguage
                    ),
                    showImportReceipt = true
                )
            }
        }
    }

    val onReceiptAction: (ImportReceiptActionType?) -> Unit = { action ->
        when (action) {
            ImportReceiptActionType.START_IMPORT -> commitSelectedImports(true)
            ImportReceiptActionType.PICK_FILE -> {
                closeImportPreview()
                importFilePickerLauncher.launch(importMimeTypes)
            }
            ImportReceiptActionType.SHOW_ISSUES -> {
                uiState = uiState.copy(showImportReceipt = false)
            }
            ImportReceiptActionType.CLOSE_PREVIEW -> {
                closeImportPreview()
            }
            null -> Unit
        }
    }

    val shareLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { }

    when {
            uiState.showEditScreen -> {
                BackHandler { uiState = uiState.copy(showEditScreen = false, editPasswordId = null) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    val currentPassword = uiState.passwords.find { it.id == uiState.editPasswordId }
                    EditScreen(
                        password = currentPassword,
                        currentLanguage = displayedLanguage,
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
            }

            uiState.showImportPreview -> {
                ImportPreviewScreen(
                    entries = uiState.importEntries,
                    selectedIds = uiState.importSelectedIds,
                    issues = uiState.importIssues,
                    receipt = if (uiState.showImportReceipt) uiState.importReceipt else null,
                    currentLanguage = displayedLanguage,
                    onToggleSelected = { id, selected ->
                        uiState = uiState.copy(
                            importSelectedIds = if (selected) {
                                uiState.importSelectedIds + id
                            } else {
                                uiState.importSelectedIds - id
                            }
                        )
                    },
                    onToggleSelectAll = { checked ->
                        val selectableIds = uiState.importEntries.map { it.id }.toSet()
                        uiState = uiState.copy(
                            importSelectedIds = if (checked) selectableIds else emptySet()
                        )
                    },
                    onConfirm = { commitSelectedImports(true) },
                    onCancel = { closeImportPreview() },
                    onDismissReceipt = { uiState = uiState.copy(showImportReceipt = false) },
                    onPrimaryReceiptAction = { onReceiptAction(uiState.importReceipt?.primaryAction) },
                    onSecondaryReceiptAction = { onReceiptAction(uiState.importReceipt?.secondaryAction) }
                )
            }

            uiState.showAllPasswords -> {
                BackHandler { uiState = uiState.copy(showAllPasswords = false) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    AllPasswordsScreen(
                        currentLanguage = displayedLanguage,
                        onBack = { uiState = uiState.copy(showAllPasswords = false) },
                        passwords = uiState.passwords,
                        onPasswordClick = { id -> uiState = uiState.copy(showAllPasswords = false, showEditScreen = true, editPasswordId = id) }
                    )
                }
            }

            uiState.showHelp -> {
                BackHandler { uiState = uiState.copy(showHelp = false) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    HelpContent(currentLanguage = displayedLanguage, onBack = { uiState = uiState.copy(showHelp = false) })
                }
            }

            uiState.showPrivacy -> {
                BackHandler { uiState = uiState.copy(showPrivacy = false) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    PrivacyContent(currentLanguage = displayedLanguage, onBack = { uiState = uiState.copy(showPrivacy = false) })
                }
            }

            uiState.showAbout -> {
                BackHandler { uiState = uiState.copy(showAbout = false) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    AboutContent(currentLanguage = displayedLanguage, onBack = { uiState = uiState.copy(showAbout = false) })
                }
            }

            uiState.showMasterPasswordSetup -> {
                BackHandler { uiState = uiState.copy(showMasterPasswordSetup = false) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    SetupMasterPasswordScreen(
                        preferencesManager = preferencesManager ?: return,
                        currentLanguage = displayedLanguage,
                        onBack = {
                            uiState = uiState.copy(showMasterPasswordSetup = false)
                            biometricEnabled = preferencesManager?.biometricEnabled ?: false
                        },
                        onPasswordSet = {
                            uiState = uiState.copy(showMasterPasswordSetup = false)
                            biometricEnabled = preferencesManager?.biometricEnabled ?: false
                        }
                    )
                }
            }

            else -> {
                Scaffold(
                    modifier = modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha },
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
                                currentLanguage = displayedLanguage,
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

                            TabItem.SECURITY -> SecurityContent(displayedLanguage)

                            TabItem.SETTINGS -> SettingsContent(
                                currentLanguage = displayedLanguage,
                                preferencesManager = preferencesManager,
                                biometricEnabled = biometricEnabled,
                                onBiometricEnabledChange = {
                                    biometricEnabled = it
                                    preferencesManager?.biometricEnabled = it
                                },
                                onNavigateToImport = {
                                    importFilePickerLauncher.launch(importMimeTypes)
                                },
                                onNavigateToExport = {
                                    uiState = uiState.copy(showExportFormatPicker = true)
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
                                onNavigateToAbout = { uiState = uiState.copy(showAbout = true) },
                                onNavigateToMasterPassword = { uiState = uiState.copy(showMasterPasswordSetup = true) }
                            )

                            TabItem.CLOUD -> CloudSyncContent(displayedLanguage, themeColors)
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

                        // 导出格式选择弹窗
                        FormatPickerSheet(
                            visible = uiState.showExportFormatPicker,
                            currentLanguage = displayedLanguage,
                            onFormatSelected = { format ->
                                uiState = uiState.copy(showExportFormatPicker = false)
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
                                val result = when (format) {
                                    ExportFormat.CSV -> CsvExporter.exportToCsv(context, exportData)
                                    ExportFormat.JSON -> JsonExporter.exportToJson(context, exportData)
                                }
                                result.onSuccess { uri ->
                                    val successMessage = if (displayedLanguage == AppLanguage.CHINESE) {
                                        "导出成功，请选择分享方式"
                                    } else {
                                        "Export successful. Choose a share target."
                                    }
                                    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                                    val shareIntent = when (format) {
                                        ExportFormat.CSV -> CsvExporter.createShareIntent(uri, "passwords_export")
                                        ExportFormat.JSON -> JsonExporter.createShareIntent(uri)
                                    }
                                    shareLauncher.launch(Intent.createChooser(shareIntent, "Export Passwords"))
                                }
                            },
                            onDismiss = { uiState = uiState.copy(showExportFormatPicker = false) }
                        )

                        if (uiState.isImportBusy) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                                        Text(
                                            text = if (displayedLanguage == AppLanguage.CHINESE) "正在解析导入文件..." else "Parsing import file...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = themeColors.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

private fun buildImportKey(service: String, username: String): String {
    return "${service.trim().lowercase()}|${username.trim().lowercase()}"
}

private fun formatImportDuration(durationMillis: Long): String {
    val seconds = durationMillis.coerceAtLeast(1) / 1000.0
    return String.format("%.1fs", seconds)
}

private fun buildParseReceipt(
    parseResult: ImportParseResult,
    duplicateCount: Int,
    fileName: String,
    durationMillis: Long,
    language: AppLanguage
): ImportReceiptUi {
    val isZh = language == AppLanguage.CHINESE
    val validCount = parseResult.entries.size
    val issueCount = parseResult.issues.size
    val hasRisk = issueCount > 0 || duplicateCount > 0
    val level = when {
        validCount == 0 -> ImportReceiptLevel.ERROR
        hasRisk -> ImportReceiptLevel.WARNING
        else -> ImportReceiptLevel.SUCCESS
    }

    val feed = mutableListOf<ImportReceiptFeedItem>()
    feed.add(
        ImportReceiptFeedItem(
            title = if (isZh) "来源文件" else "Source File",
            description = if (isZh) {
                "已读取 $fileName，分隔符识别为 '${parseResult.detectedDelimiter}'"
            } else {
                "Loaded $fileName. Detected delimiter '${parseResult.detectedDelimiter}'."
            },
            tag = if (isZh) "${parseResult.totalRows} 行" else "${parseResult.totalRows} rows",
            tone = ImportReceiptFeedTone.INFO
        )
    )
    if (duplicateCount > 0) {
        feed.add(
            ImportReceiptFeedItem(
                title = if (isZh) "发现重复项" else "Duplicates Found",
                description = if (isZh) {
                    "检测到 $duplicateCount 条与现有或本批次重复，默认不会自动覆盖。"
                } else {
                    "$duplicateCount records are duplicated with existing or current batch; they will not be overwritten by default."
                },
                tag = if (isZh) "重复" else "duplicate",
                tone = ImportReceiptFeedTone.WARNING
            )
        )
    }
    if (issueCount > 0) {
        feed.add(
            ImportReceiptFeedItem(
                title = if (isZh) "发现格式问题" else "Format Issues",
                description = if (isZh) {
                    "有 $issueCount 行格式不完整，已标记到异常列表。"
                } else {
                    "$issueCount rows are incomplete and have been marked as issues."
                },
                tag = if (isZh) "待修复" else "fix",
                tone = ImportReceiptFeedTone.ERROR
            )
        )
    }
    if (!hasRisk && validCount > 0) {
        feed.add(
            ImportReceiptFeedItem(
                title = if (isZh) "解析完成" else "Ready",
                description = if (isZh) "字段校验通过，可以开始导入。" else "Validation passed. Ready to import.",
                tag = if (isZh) "就绪" else "ready",
                tone = ImportReceiptFeedTone.SUCCESS
            )
        )
    }

    val statusLabel: String
    val title: String
    val description: String
    val primaryAction: ImportReceiptActionType
    val primaryActionText: String
    val secondaryAction: ImportReceiptActionType
    val secondaryActionText: String

    when (level) {
        ImportReceiptLevel.SUCCESS -> {
            statusLabel = if (isZh) "全部就绪" else "All Set"
            title = if (isZh) "$validCount 条记录可直接导入" else "$validCount records are ready to import"
            description = if (isZh) "没有发现重复或格式异常。" else "No duplicates or format errors found."
            primaryAction = ImportReceiptActionType.START_IMPORT
            primaryActionText = if (isZh) "开始导入" else "Import Now"
            secondaryAction = ImportReceiptActionType.CLOSE_PREVIEW
            secondaryActionText = if (isZh) "稍后处理" else "Later"
        }
        ImportReceiptLevel.WARNING -> {
            statusLabel = if (isZh) "部分待处理" else "Needs Attention"
            title = if (isZh) "$validCount 条可导入，存在风险项" else "$validCount importable records with potential risks"
            description = if (isZh) "建议先查看异常和重复记录，再执行导入。" else "Review duplicates/issues before importing."
            primaryAction = ImportReceiptActionType.START_IMPORT
            primaryActionText = if (isZh) "仅导入可用项" else "Import Valid Only"
            secondaryAction = ImportReceiptActionType.SHOW_ISSUES
            secondaryActionText = if (isZh) "查看明细" else "View Details"
        }
        ImportReceiptLevel.ERROR -> {
            statusLabel = if (isZh) "导入失败" else "Import Failed"
            title = if (isZh) "当前文件无法完成导入" else "Cannot import this file"
            description = if (isZh) "请修复编码或字段格式后重试。" else "Fix encoding or field format, then retry."
            primaryAction = ImportReceiptActionType.PICK_FILE
            primaryActionText = if (isZh) "重新选择文件" else "Pick Another File"
            secondaryAction = ImportReceiptActionType.SHOW_ISSUES
            secondaryActionText = if (isZh) "查看原因" else "See Why"
        }
    }

    return ImportReceiptUi(
        level = level,
        statusLabel = statusLabel,
        title = title,
        description = description,
        primaryValue = validCount.toString(),
        primaryLabel = if (isZh) "可导入" else "Importable",
        secondaryValue = (issueCount + duplicateCount).toString(),
        secondaryLabel = if (isZh) "待处理" else "Pending",
        durationText = formatImportDuration(durationMillis),
        primaryActionText = primaryActionText,
        secondaryActionText = secondaryActionText,
        primaryAction = primaryAction,
        secondaryAction = secondaryAction,
        feedItems = feed
    )
}

private fun buildParseFailureReceipt(reason: String, language: AppLanguage): ImportReceiptUi {
    val isZh = language == AppLanguage.CHINESE
    return ImportReceiptUi(
        level = ImportReceiptLevel.ERROR,
        statusLabel = if (isZh) "导入失败" else "Import Failed",
        title = if (isZh) "无法解析当前文件" else "Failed to parse file",
        description = reason,
        primaryValue = "0",
        primaryLabel = if (isZh) "可导入" else "Importable",
        secondaryValue = "-",
        secondaryLabel = if (isZh) "待处理" else "Pending",
        durationText = "0.0s",
        primaryActionText = if (isZh) "重新选择文件" else "Pick Another File",
        secondaryActionText = if (isZh) "查看原因" else "See Why",
        primaryAction = ImportReceiptActionType.PICK_FILE,
        secondaryAction = ImportReceiptActionType.SHOW_ISSUES,
        feedItems = listOf(
            ImportReceiptFeedItem(
                title = if (isZh) "建议" else "Suggestion",
                description = if (isZh) {
                    "请确认文件为 UTF-8 编码并包含服务、用户名、密码等字段。"
                } else {
                    "Make sure the file is UTF-8 encoded and includes service, username, and password fields."
                },
                tag = if (isZh) "修复" else "fix",
                tone = ImportReceiptFeedTone.WARNING
            )
        )
    )
}

private fun buildNoSelectionReceipt(language: AppLanguage): ImportReceiptUi {
    val isZh = language == AppLanguage.CHINESE
    return ImportReceiptUi(
        level = ImportReceiptLevel.WARNING,
        statusLabel = if (isZh) "尚未选择" else "No Selection",
        title = if (isZh) "请先勾选至少一条记录" else "Select at least one record",
        description = if (isZh) "可以逐条选择后再导入，避免误操作。" else "Select specific records before import.",
        primaryValue = "0",
        primaryLabel = if (isZh) "已选中" else "Selected",
        secondaryValue = "-",
        secondaryLabel = if (isZh) "待导入" else "To Import",
        durationText = "0.0s",
        primaryActionText = if (isZh) "查看列表" else "Back to List",
        secondaryActionText = if (isZh) "关闭" else "Close",
        primaryAction = ImportReceiptActionType.SHOW_ISSUES,
        secondaryAction = ImportReceiptActionType.CLOSE_PREVIEW,
        feedItems = listOf(
            ImportReceiptFeedItem(
                title = if (isZh) "提示" else "Tip",
                description = if (isZh) {
                    "支持全选或取消选择，也可仅导入需要的条目。"
                } else {
                    "Use select all, unselect all, or only import what you need."
                },
                tag = if (isZh) "操作建议" else "hint",
                tone = ImportReceiptFeedTone.INFO
            )
        )
    )
}

private fun buildImportDoneReceipt(
    importedCount: Int,
    duplicateSkipped: Int,
    parseIssueCount: Int,
    selectedCount: Int,
    durationMillis: Long,
    language: AppLanguage
): ImportReceiptUi {
    val isZh = language == AppLanguage.CHINESE
    val unresolved = duplicateSkipped + parseIssueCount
    val level = when {
        importedCount == 0 -> ImportReceiptLevel.ERROR
        unresolved > 0 -> ImportReceiptLevel.WARNING
        else -> ImportReceiptLevel.SUCCESS
    }

    val feed = mutableListOf<ImportReceiptFeedItem>()
    feed.add(
        ImportReceiptFeedItem(
            title = if (isZh) "导入结果" else "Import Result",
            description = if (isZh) {
                "本次选择 $selectedCount 条，成功写入 $importedCount 条。"
            } else {
                "$importedCount out of $selectedCount selected records were imported."
            },
            tag = "$importedCount/$selectedCount",
            tone = if (importedCount > 0) ImportReceiptFeedTone.SUCCESS else ImportReceiptFeedTone.ERROR
        )
    )
    if (duplicateSkipped > 0) {
        feed.add(
            ImportReceiptFeedItem(
                title = if (isZh) "重复项已跳过" else "Duplicates Skipped",
                description = if (isZh) {
                    "为避免覆盖，自动跳过 $duplicateSkipped 条重复记录。"
                } else {
                    "$duplicateSkipped duplicates were skipped to avoid overwrite."
                },
                tag = if (isZh) "跳过" else "skip",
                tone = ImportReceiptFeedTone.WARNING
            )
        )
    }
    if (parseIssueCount > 0) {
        feed.add(
            ImportReceiptFeedItem(
                title = if (isZh) "存在异常行" else "Invalid Rows",
                description = if (isZh) {
                    "$parseIssueCount 行未通过格式校验，建议修复后重新导入。"
                } else {
                    "$parseIssueCount rows failed validation. Please fix and retry."
                },
                tag = if (isZh) "异常" else "invalid",
                tone = ImportReceiptFeedTone.ERROR
            )
        )
    }
    feed.add(
        ImportReceiptFeedItem(
            title = if (isZh) "安全建议" else "Security Advice",
            description = if (isZh) {
                "为避免明文泄露，请尽快删除源 CSV 文件。"
            } else {
                "Delete the source CSV file as soon as possible to reduce plaintext exposure."
            },
            tag = if (isZh) "重要" else "important",
            tone = ImportReceiptFeedTone.INFO
        )
    )

    return ImportReceiptUi(
        level = level,
        statusLabel = if (level == ImportReceiptLevel.SUCCESS) {
            if (isZh) "导入完成" else "Import Completed"
        } else if (level == ImportReceiptLevel.WARNING) {
            if (isZh) "导入部分完成" else "Partially Completed"
        } else {
            if (isZh) "未导入成功" else "Import Not Completed"
        },
        title = if (level == ImportReceiptLevel.ERROR) {
            if (isZh) "本次没有导入成功" else "No records imported this time"
        } else {
            if (isZh) "$importedCount 条密码已写入保险库" else "$importedCount passwords imported into vault"
        },
        description = if (level == ImportReceiptLevel.ERROR) {
            if (isZh) "请检查异常项后重试。" else "Please review issues and retry."
        } else {
            if (isZh) "你可以继续处理剩余异常或直接返回。" else "You can review remaining issues or return now."
        },
        primaryValue = importedCount.toString(),
        primaryLabel = if (isZh) "成功导入" else "Imported",
        secondaryValue = unresolved.toString(),
        secondaryLabel = if (isZh) "待处理" else "Pending",
        durationText = formatImportDuration(durationMillis),
        primaryActionText = if (level == ImportReceiptLevel.ERROR) {
            if (isZh) "重新选择文件" else "Pick Another File"
        } else {
            if (isZh) "完成" else "Done"
        },
        secondaryActionText = if (isZh) "查看明细" else "View Details",
        primaryAction = if (level == ImportReceiptLevel.ERROR) ImportReceiptActionType.PICK_FILE else ImportReceiptActionType.CLOSE_PREVIEW,
        secondaryAction = ImportReceiptActionType.SHOW_ISSUES,
        feedItems = feed
    )
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
    preferencesManager: PreferencesManager?,
    biometricEnabled: Boolean,
    onBiometricEnabledChange: (Boolean) -> Unit,
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
    onNavigateToAbout: () -> Unit,
    onNavigateToMasterPassword: () -> Unit = {}
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE

    var themeItemOffset by remember { mutableStateOf(IntOffset.Zero) }
    var themeItemSize by remember { mutableStateOf(IntSize.Zero) }
    var languageItemOffset by remember { mutableStateOf(IntOffset.Zero) }
    var languageItemSize by remember { mutableStateOf(IntSize.Zero) }
    var soundEnabled by remember { mutableStateOf(true) }
    
    // 剪贴板设置状态
    var clipboardClearEnabled by remember { mutableStateOf(preferencesManager?.clipboardClearEnabled ?: false) }
    var clipboardClearDelay by remember { mutableStateOf(preferencesManager?.clipboardClearDelay ?: 30) }
    var showClipboardDelayPicker by remember { mutableStateOf(false) }
    
    val clipboardDelayOptions = listOf(
        15 to (if (isZh) "15 秒" else "15 sec"),
        30 to (if (isZh) "30 秒" else "30 sec"),
        60 to (if (isZh) "1 分钟" else "1 min"),
        300 to (if (isZh) "5 分钟" else "5 min")
    )
    val currentDelayLabel = clipboardDelayOptions.firstOrNull { it.first == clipboardClearDelay }?.second
        ?: (if (isZh) "${clipboardClearDelay} 秒" else "${clipboardClearDelay} sec")
    
    val settingsTitle = if (isZh) "设置" else "Settings"
    val accountTitle = if (isZh) "账户" else "Account"
    val appSettingsTitle = if (isZh) "应用设置" else "App Settings"
    val masterPwdLabel = if (isZh) "主密码" else "Master Password"
    val themeLabel = if (isZh) "主题外观" else "Theme"
    val langLabel = if (isZh) "语言" else "Language"
    val soundLabel = if (isZh) "声音反馈" else "Sound Feedback"
    val clipClearLabel = if (isZh) "自动清除剪贴板" else "Auto-clear Clipboard"
    val clipDelayLabel = if (isZh) "清除延迟" else "Clear Delay"
    val dataTitle = if (isZh) "数据管理" else "Data Management"
    val exportLabel = if (isZh) "导出密码" else "Export Passwords"
    val importLabel = if (isZh) "导入密码" else "Import Passwords"
    val moreTitle = if (isZh) "更多" else "More"
    val helpLabel = if (isZh) "使用帮助" else "Help"
    val privacyLabel = if (isZh) "隐私条款" else "Privacy Policy"
    val aboutLabel = if (isZh) "关于我们" else "About Us"
    
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
            SettingItem(icon = Icons.Outlined.Shield, label = masterPwdLabel, trailingText = if (preferencesManager?.hasMasterPassword == true) (if (isZh) "已设置" else "Set") else (if (isZh) "未设置" else "Not set"), onClick = onNavigateToMasterPassword, colors = themeColors)
            // 指纹解锁（仅在已设置主密码时显示）
            if (preferencesManager?.hasMasterPassword == true) {
                SettingToggleItem(
                    icon = Icons.Outlined.Fingerprint,
                    label = if (isZh) "指纹解锁" else "Fingerprint Unlock",
                    checked = biometricEnabled,
                    onCheckedChange = onBiometricEnabledChange,
                    colors = themeColors
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(title = appSettingsTitle, colors = themeColors)
            SettingItem(icon = Icons.Outlined.DarkMode, label = themeLabel, trailingText = currentThemeLabel, onClick = { onThemeDropdownToggle(themeItemOffset, themeItemSize) }, onPositioned = { offset, size -> themeItemOffset = offset; themeItemSize = size }, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Language, label = langLabel, trailingText = currentLanguageLabel, onClick = { onLanguageDropdownToggle(languageItemOffset, languageItemSize) }, onPositioned = { offset, size -> languageItemOffset = offset; languageItemSize = size }, colors = themeColors)
            SettingToggleItem(icon = Icons.AutoMirrored.Outlined.VolumeUp, label = soundLabel, checked = soundEnabled, onCheckedChange = { soundEnabled = it }, colors = themeColors)
            
            // 剪贴板自动清除开关
            SettingToggleItem(
                icon = Icons.Outlined.ContentPaste,
                label = clipClearLabel,
                checked = clipboardClearEnabled,
                onCheckedChange = {
                    clipboardClearEnabled = it
                    preferencesManager?.clipboardClearEnabled = it
                },
                colors = themeColors
            )
            // 清除延迟选择（仅在开启时显示）
            if (clipboardClearEnabled) {
                Box {
                    SettingItem(
                        icon = Icons.Outlined.Timer,
                        label = clipDelayLabel,
                        trailingText = currentDelayLabel,
                        onClick = { showClipboardDelayPicker = !showClipboardDelayPicker },
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
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(title = dataTitle, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Upload, label = exportLabel, onClick = onNavigateToExport, colors = themeColors)
            SettingItem(icon = Icons.Outlined.Download, label = importLabel, onClick = onNavigateToImport, colors = themeColors)
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(title = moreTitle, colors = themeColors)
            SettingItem(icon = Icons.AutoMirrored.Outlined.HelpOutline, label = helpLabel, onClick = onNavigateToHelp, colors = themeColors)
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
            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = themeColors.onBackground, modifier = Modifier.size(24.dp).clickable { onBack() })
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
            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = themeColors.onBackground, modifier = Modifier.size(24.dp).clickable { onBack() })
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
            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = themeColors.onBackground, modifier = Modifier.size(24.dp).clickable { onBack() })
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


