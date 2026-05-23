package com.example.passcard.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import com.example.passcard.data.PasswordSecurityStats
import com.example.passcard.data.ReusedPasswordGroup
import kotlinx.coroutines.delay
import com.example.passcard.ui.components.*
import com.example.passcard.ui.theme.*
import com.example.passcard.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun MainScreen(
    preferencesManager: PreferencesManager? = null,
    onThemeChanged: (() -> Unit)? = null,
    currentTheme: String = "LIGHT",
    languageKey: String = "CHINESE",
    passwords: List<PasswordItem> = emptyList(),
    passwordCount: Int = passwords.size,
    pagedPasswords: Flow<PagingData<PasswordItem>> = flowOf(PagingData.empty()),
    securityStats: PasswordSecurityStats = PasswordSecurityStats(),
    weakPasswords: List<PasswordItem> = emptyList(),
    reusedPasswordGroups: List<ReusedPasswordGroup> = emptyList(),
    loadAllPasswords: suspend () -> List<PasswordItem> = { passwords },
    onHomeSearchQueryChange: (String) -> Unit = {},
    onAllPasswordsSearchQueryChange: (String) -> Unit = {},
    onSavePassword: ((PasswordItem) -> Unit)? = null,
    onImportPasswords: ((List<PasswordItem>) -> Unit)? = null,
    onReplacePasswords: ((List<PasswordItem>) -> Unit)? = null,
    onDeletePassword: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    MainContainer(
        preferencesManager = preferencesManager,
        onThemeChanged = onThemeChanged,
        currentTheme = currentTheme,
        languageKey = languageKey,
        passwords = passwords,
        passwordCount = passwordCount,
        pagedPasswords = pagedPasswords,
        securityStats = securityStats,
        weakPasswords = weakPasswords,
        reusedPasswordGroups = reusedPasswordGroups,
        loadAllPasswords = loadAllPasswords,
        onHomeSearchQueryChange = onHomeSearchQueryChange,
        onAllPasswordsSearchQueryChange = onAllPasswordsSearchQueryChange,
        onSavePassword = onSavePassword,
        onImportPasswords = onImportPasswords,
        onReplacePasswords = onReplacePasswords,
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
    passwordCount: Int = passwords.size,
    pagedPasswords: Flow<PagingData<PasswordItem>> = flowOf(PagingData.empty()),
    securityStats: PasswordSecurityStats = PasswordSecurityStats(),
    weakPasswords: List<PasswordItem> = emptyList(),
    reusedPasswordGroups: List<ReusedPasswordGroup> = emptyList(),
    loadAllPasswords: suspend () -> List<PasswordItem> = { passwords },
    onHomeSearchQueryChange: (String) -> Unit = {},
    onAllPasswordsSearchQueryChange: (String) -> Unit = {},
    onSavePassword: ((PasswordItem) -> Unit)? = null,
    onImportPasswords: ((List<PasswordItem>) -> Unit)? = null,
    onReplacePasswords: ((List<PasswordItem>) -> Unit)? = null,
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
    val allPasswordsListState = rememberLazyListState()
    val settingsScrollState = rememberScrollState()
    val helpScrollState = rememberScrollState()
    var allPasswordsSearchQuery by remember { mutableStateOf("") }
    
    var uiState by remember { mutableStateOf(MainUiState(passwords = passwords)) }
    var biometricEnabled by remember { mutableStateOf(preferencesManager?.biometricEnabled ?: false) }
    
    // 同步外部密码数据
    LaunchedEffect(passwords) {
        uiState = uiState.copy(passwords = passwords)
    }
        // 计算字符串（非 Composable）
        val welcomeText = if (displayedLanguage == AppLanguage.CHINESE) "欢迎回来" else "Welcome Back"
        val searchPlaceholder = if (displayedLanguage == AppLanguage.CHINESE) "搜索密码..." else "Search passwords..."
        val pwdCountText = if (displayedLanguage == AppLanguage.CHINESE) "${passwordCount} 个密码" else "${passwordCount} Passwords"
        val secScoreText = if (displayedLanguage == AppLanguage.CHINESE) "${securityStats.score}% 安全" else "${securityStats.score}% Secure"
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
    val fileActions = rememberMainFileActions(
        uiState = uiState,
        currentLanguage = displayedLanguage,
        updateUiState = { transform -> uiState = transform(uiState) },
        loadAllPasswords = loadAllPasswords,
        onImportPasswords = onImportPasswords,
        onSavePassword = onSavePassword
    )

    when (val route = uiState.route) {
            is MainRoute.EditPassword -> {
                BackHandler { uiState = uiState.copy(route = route.returnRoute) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    val currentPassword = route.initialPassword ?: uiState.passwords.find { it.id == route.passwordId }
                    EditScreen(
                        password = currentPassword,
                        currentLanguage = displayedLanguage,
                        randomPasswordSpec = preferencesManager?.randomPasswordSpec,
                        loadAllPasswords = loadAllPasswords,
                        onBack = { uiState = uiState.copy(route = route.returnRoute) },
                        onSave = { updatedPassword ->
                            val itemToSave = if (route.passwordId == null) {
                                updatedPassword.copy(id = System.currentTimeMillis().toString())
                            } else {
                                updatedPassword
                            }
                            onSavePassword?.invoke(itemToSave)
                            uiState = uiState.copy(route = route.returnRoute)
                        },
                        onDelete = {
                            route.passwordId?.let { onDeletePassword?.invoke(it) }
                            uiState = uiState.copy(route = route.returnRoute)
                        }
                    )
                }
            }

            MainRoute.ImportPreview -> {
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
                    onConfirm = { fileActions.commitSelectedImports(true) },
                    onCancel = { fileActions.closeImportPreview() },
                    onDismissReceipt = { uiState = uiState.copy(showImportReceipt = false) },
                    onPrimaryReceiptAction = { fileActions.handleReceiptAction(uiState.importReceipt?.primaryAction) },
                    onSecondaryReceiptAction = { fileActions.handleReceiptAction(uiState.importReceipt?.secondaryAction) }
                )
            }

            MainRoute.AllPasswords -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Tabs) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    AllPasswordsScreen(
                        currentLanguage = displayedLanguage,
                        onBack = { uiState = uiState.copy(route = MainRoute.Tabs) },
                        pagedPasswords = pagedPasswords,
                        searchQuery = allPasswordsSearchQuery,
                        onSearchQueryChange = { query ->
                            allPasswordsSearchQuery = query
                            onAllPasswordsSearchQueryChange(query)
                        },
                        listState = allPasswordsListState,
                        onPasswordClick = { item ->
                            uiState = uiState.copy(
                                route = MainRoute.EditPassword(
                                    passwordId = item.id,
                                    initialPassword = item,
                                    returnRoute = MainRoute.AllPasswords
                                )
                            )
                        }
                    )
                }
            }

            MainRoute.Help -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Tabs) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    HelpContent(
                        currentLanguage = displayedLanguage,
                        onBack = { uiState = uiState.copy(route = MainRoute.Tabs) },
                        onNavigateToCloudBackupHelp = { uiState = uiState.copy(route = MainRoute.CloudBackupHelp) },
                        scrollState = helpScrollState
                    )
                }
            }

            MainRoute.CloudBackupHelp -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Help) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    CloudBackupHelpContent(
                        currentLanguage = displayedLanguage,
                        onBack = { uiState = uiState.copy(route = MainRoute.Help) }
                    )
                }
            }

            MainRoute.Privacy -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Tabs) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    PrivacyContent(currentLanguage = displayedLanguage, onBack = { uiState = uiState.copy(route = MainRoute.Tabs) })
                }
            }

            MainRoute.About -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Tabs) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    AboutContent(currentLanguage = displayedLanguage, onBack = { uiState = uiState.copy(route = MainRoute.Tabs) })
                }
            }

            MainRoute.MasterPasswordSetup -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Tabs) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    SetupMasterPasswordScreen(
                        preferencesManager = preferencesManager ?: return,
                        currentLanguage = displayedLanguage,
                        onBack = {
                            uiState = uiState.copy(route = MainRoute.Tabs)
                            biometricEnabled = preferencesManager.biometricEnabled
                        },
                        onPasswordSet = {
                            uiState = uiState.copy(route = MainRoute.Tabs)
                            biometricEnabled = preferencesManager.biometricEnabled
                        }
                    )
                }
            }

            MainRoute.RandomPasswordSettings -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Tabs) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
                    RandomPasswordSettingsScreen(
                        preferencesManager = preferencesManager,
                        currentLanguage = displayedLanguage,
                        onBack = { uiState = uiState.copy(route = MainRoute.Tabs) }
                    )
                }
            }

            MainRoute.WeakPasswords -> {
                WeakPasswordsScreen(
                    currentLanguage = displayedLanguage,
                    items = weakPasswords,
                    onBack = { uiState = uiState.copy(route = MainRoute.Tabs) },
                    onPasswordClick = { item ->
                        uiState = uiState.copy(
                            route = MainRoute.EditPassword(
                                passwordId = item.id,
                                initialPassword = item,
                                returnRoute = MainRoute.WeakPasswords
                            )
                        )
                    }
                )
            }

            MainRoute.ReusedPasswords -> {
                ReusedPasswordsScreen(
                    currentLanguage = displayedLanguage,
                    groups = reusedPasswordGroups,
                    onBack = { uiState = uiState.copy(route = MainRoute.Tabs) },
                    onPasswordClick = { item ->
                        uiState = uiState.copy(
                            route = MainRoute.EditPassword(
                                passwordId = item.id,
                                initialPassword = item,
                                returnRoute = MainRoute.ReusedPasswords
                            )
                        )
                    }
                )
            }

            MainRoute.Tabs -> {
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
                                onAddClick = { uiState = uiState.copy(route = MainRoute.EditPassword(null)) }
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
                                onSearchQueryChange = {
                                    uiState = uiState.copy(searchQuery = it)
                                    onHomeSearchQueryChange(it)
                                },
                                onNavigateToAllPasswords = { uiState = uiState.copy(route = MainRoute.AllPasswords) },
                                onPasswordClick = { id ->
                                    uiState = uiState.copy(
                                        route = MainRoute.EditPassword(
                                            passwordId = id,
                                            initialPassword = uiState.passwords.find { it.id == id }
                                        )
                                    )
                                },
                                onAddPassword = { uiState = uiState.copy(route = MainRoute.EditPassword(null)) }
                            )

                            TabItem.SECURITY -> SecurityContent(
                                currentLanguage = displayedLanguage,
                                stats = securityStats,
                                onOpenAllPasswords = { uiState = uiState.copy(route = MainRoute.AllPasswords) },
                                onOpenWeakPasswords = { uiState = uiState.copy(route = MainRoute.WeakPasswords) },
                                onOpenReusedPasswords = { uiState = uiState.copy(route = MainRoute.ReusedPasswords) }
                            )

                            TabItem.SETTINGS -> SettingsContent(
                                currentLanguage = displayedLanguage,
                                preferencesManager = preferencesManager,
                                biometricEnabled = biometricEnabled,
                                onBiometricEnabledChange = {
                                    biometricEnabled = it
                                    preferencesManager?.biometricEnabled = it
                                },
                                onNavigateToImport = {
                                    fileActions.launchImportPicker()
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
                                onNavigateToHelp = { uiState = uiState.copy(route = MainRoute.Help) },
                                onNavigateToPrivacy = { uiState = uiState.copy(route = MainRoute.Privacy) },
                                onNavigateToAbout = { uiState = uiState.copy(route = MainRoute.About) },
                                onNavigateToMasterPassword = { uiState = uiState.copy(route = MainRoute.MasterPasswordSetup) },
                                onNavigateToRandomPassword = { uiState = uiState.copy(route = MainRoute.RandomPasswordSettings) },
                                scrollState = settingsScrollState
                            )

                            TabItem.CLOUD -> CloudSyncContent(
                                currentLanguage = displayedLanguage,
                                themeColors = themeColors,
                                preferencesManager = preferencesManager,
                                passwords = uiState.passwords,
                                loadAllPasswords = loadAllPasswords,
                                replaceVaultPasswords = { restored ->
                                    onReplacePasswords?.invoke(restored)
                                    uiState = uiState.copy(passwords = restored)
                                }
                            )
                        }



                        MainOverlayLayer(
                            uiState = uiState,
                            currentLanguage = displayedLanguage,
                            currentTheme = currentTheme,
                            languageKey = languageKey,
                            themeOptions = themeOptions,
                            languageOptions = languageOptions,
                            onDismissTheme = { uiState = uiState.copy(showThemeDropdown = false) },
                            onThemeSelected = { option ->
                                preferencesManager?.theme = option.value
                                uiState = uiState.copy(showThemeDropdown = false)
                                onThemeChanged?.invoke()
                            },
                            onDismissLanguage = { uiState = uiState.copy(showLanguageDropdown = false) },
                            onLanguageSelected = { option ->
                                preferencesManager?.language = option.value
                                uiState = uiState.copy(showLanguageDropdown = false)
                                onThemeChanged?.invoke()
                            },
                            onExportFormatSelected = { format ->
                                uiState = uiState.copy(showExportFormatPicker = false)
                                fileActions.requestExport(format)
                            },
                            onDismissExportPicker = { uiState = uiState.copy(showExportFormatPicker = false) }
                        )
                    }
                }
            }
        }
}

