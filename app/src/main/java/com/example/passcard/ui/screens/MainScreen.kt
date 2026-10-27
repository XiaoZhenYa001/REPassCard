package com.example.passcard.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passcard.data.PasswordHealthSnapshot
import com.example.passcard.data.PasswordSecurityStats
import com.example.passcard.data.ReusedPasswordGroup
import com.example.passcard.ui.components.*
import com.example.passcard.ui.theme.*
import com.example.passcard.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

private val EmptyWeakPasswordsFlow: StateFlow<List<PasswordItem>> = MutableStateFlow(emptyList())
private val EmptyReusedPasswordGroupsFlow: StateFlow<List<ReusedPasswordGroup>> = MutableStateFlow(emptyList())

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    navigationState: MainNavigationState? = null,
    preferencesManager: PreferencesManager? = null,
    onThemeChanged: (() -> Unit)? = null,
    currentTheme: String = "LIGHT",
    languageKey: String = "CHINESE",
    passwords: List<PasswordItem> = emptyList(),
    passwordCount: Int = passwords.size,
    isHomeLoading: Boolean = false,
    homeErrorMessage: String? = null,
    healthSnapshot: PasswordHealthSnapshot? = null,
    isHealthLoading: Boolean = false,
    healthErrorMessage: String? = null,
    onRetryData: () -> Unit = {},
    pagedPasswords: Flow<PagingData<PasswordItem>> = flowOf(PagingData.empty()),
    categories: List<String> = emptyList(),
    securityStats: PasswordSecurityStats = PasswordSecurityStats(),
    weakPasswordsFlow: StateFlow<List<PasswordItem>> = EmptyWeakPasswordsFlow,
    reusedPasswordGroupsFlow: StateFlow<List<ReusedPasswordGroup>> = EmptyReusedPasswordGroupsFlow,
    loadAllPasswords: suspend () -> List<PasswordItem> = { passwords },
    onHomeSearchQueryChange: (String) -> Unit = {},
    onHomeCategoryChange: (String?) -> Unit = {},
    onAllPasswordsSearchQueryChange: (String) -> Unit = {},
    onAllPasswordsCategoryChange: (String?) -> Unit = {},
    onSavePassword: (suspend (PasswordItem) -> Unit)? = null,
    onImportPasswords: (suspend (List<PasswordItem>) -> Unit)? = null,
    onReplacePasswords: (suspend (List<PasswordItem>) -> Unit)? = null,
    onDeletePassword: (suspend (String) -> Unit)? = null
) {
    MainContainer(
        navigationState = navigationState,
        preferencesManager = preferencesManager,
        onThemeChanged = onThemeChanged,
        currentTheme = currentTheme,
        languageKey = languageKey,
        passwords = passwords,
        passwordCount = passwordCount,
        isHomeLoading = isHomeLoading,
        homeErrorMessage = homeErrorMessage,
        healthSnapshot = healthSnapshot,
        isHealthLoading = isHealthLoading,
        healthErrorMessage = healthErrorMessage,
        onRetryData = onRetryData,
        pagedPasswords = pagedPasswords,
        categories = categories,
        securityStats = securityStats,
        weakPasswordsFlow = weakPasswordsFlow,
        reusedPasswordGroupsFlow = reusedPasswordGroupsFlow,
        loadAllPasswords = loadAllPasswords,
        onHomeSearchQueryChange = onHomeSearchQueryChange,
        onHomeCategoryChange = onHomeCategoryChange,
        onAllPasswordsSearchQueryChange = onAllPasswordsSearchQueryChange,
        onAllPasswordsCategoryChange = onAllPasswordsCategoryChange,
        onSavePassword = onSavePassword,
        onImportPasswords = onImportPasswords,
        onReplacePasswords = onReplacePasswords,
        onDeletePassword = onDeletePassword,
        modifier = modifier
    )
}

@Composable
fun MainContainer(
    modifier: Modifier = Modifier,
    navigationState: MainNavigationState? = null,
    preferencesManager: PreferencesManager? = null,
    onThemeChanged: (() -> Unit)? = null,
    currentTheme: String = "LIGHT",
    languageKey: String = "CHINESE",
    passwords: List<PasswordItem> = emptyList(),
    passwordCount: Int = passwords.size,
    isHomeLoading: Boolean = false,
    homeErrorMessage: String? = null,
    healthSnapshot: PasswordHealthSnapshot? = null,
    isHealthLoading: Boolean = false,
    healthErrorMessage: String? = null,
    onRetryData: () -> Unit = {},
    pagedPasswords: Flow<PagingData<PasswordItem>> = flowOf(PagingData.empty()),
    categories: List<String> = emptyList(),
    securityStats: PasswordSecurityStats = PasswordSecurityStats(),
    weakPasswordsFlow: StateFlow<List<PasswordItem>> = EmptyWeakPasswordsFlow,
    reusedPasswordGroupsFlow: StateFlow<List<ReusedPasswordGroup>> = EmptyReusedPasswordGroupsFlow,
    loadAllPasswords: suspend () -> List<PasswordItem> = { passwords },
    onHomeSearchQueryChange: (String) -> Unit = {},
    onHomeCategoryChange: (String?) -> Unit = {},
    onAllPasswordsSearchQueryChange: (String) -> Unit = {},
    onAllPasswordsCategoryChange: (String?) -> Unit = {},
    onSavePassword: (suspend (PasswordItem) -> Unit)? = null,
    onImportPasswords: (suspend (List<PasswordItem>) -> Unit)? = null,
    onReplacePasswords: (suspend (List<PasswordItem>) -> Unit)? = null,
    onDeletePassword: (suspend (String) -> Unit)? = null
) {
    val context = LocalContext.current
    val appearanceTransition = LocalAppearanceTransitionController.current
    val tabTransition = rememberAppearanceTransitionController()
    val touchInput = LocalInputModeManager.current.inputMode == InputMode.Touch
    val layoutDirection = LocalLayoutDirection.current
    val displayedLanguage = remember(languageKey) {
        if (languageKey == "ENGLISH") AppLanguage.ENGLISH else AppLanguage.CHINESE
    }

    val themeColors = LocalThemeColors.current
    val homeScrollState = rememberLazyListState()
    val securityScrollState = rememberScrollState()
    val cloudScrollState = rememberLazyListState()
    val allPasswordsListState = rememberLazyListState()
    val settingsScrollState = rememberScrollState()
    val helpScrollState = rememberScrollState()
    val stateHolder = navigationState ?: remember { MainNavigationState() }
    var allPasswordsSearchQuery by stateHolder.allPasswordsSearchQuery
    var allPasswordsCategory by stateHolder.allPasswordsCategory
    
    var uiState by stateHolder.uiState
    SideEffect {
        if (uiState.route != MainRoute.Tabs) {
            appearanceTransition?.clear()
            tabTransition.clear()
        }
    }
    DisposableEffect(appearanceTransition, tabTransition) {
        onDispose {
            appearanceTransition?.clear()
            tabTransition.clear()
        }
    }
    var biometricEnabled by remember { mutableStateOf(preferencesManager?.biometricEnabled ?: false) }
    val homePasswordSnapshot = remember(passwords) { PasswordListSnapshot(passwords) }
        // 计算字符串（非 Composable）
        val welcomeText = if (displayedLanguage == AppLanguage.CHINESE) "欢迎回来" else "Welcome Back"
        val searchPlaceholder = if (displayedLanguage == AppLanguage.CHINESE) "搜索密码..." else "Search passwords..."
        val recentText = if (displayedLanguage == AppLanguage.CHINESE) "最近更新" else "Recently updated"
        val viewAllText = if (displayedLanguage == AppLanguage.CHINESE) "查看全部" else "View All"

        val themeOptions = remember(displayedLanguage) {
            listOf(
                DropdownOption(if (displayedLanguage == AppLanguage.CHINESE) "浅色" else "Light", "LIGHT"),
                DropdownOption(if (displayedLanguage == AppLanguage.CHINESE) "深色" else "Dark", "DARK"),
                DropdownOption(if (displayedLanguage == AppLanguage.CHINESE) "跟随系统" else "System", "SYSTEM")
            )
        }
        val languageOptions = remember {
            listOf(
                DropdownOption("中文", "CHINESE"),
                DropdownOption("English", "ENGLISH")
            )
        }
        val themeOptionSet = remember(themeOptions) { DropdownOptionSet(themeOptions) }
        val languageOptionSet = remember(languageOptions) { DropdownOptionSet(languageOptions) }
        val overlayState = remember(
            uiState.showThemeDropdown,
            uiState.themeDropdownOffset,
            uiState.themeDropdownSize,
            uiState.showLanguageDropdown,
            uiState.languageDropdownOffset,
            uiState.languageDropdownSize,
            uiState.showExportFormatPicker,
            uiState.isImportBusy,
            uiState.isExportBusy,
            uiState.fileOperationStage
        ) {
            MainOverlayUiState(
                showThemeDropdown = uiState.showThemeDropdown,
                themeDropdownOffset = uiState.themeDropdownOffset,
                themeDropdownSize = uiState.themeDropdownSize,
                showLanguageDropdown = uiState.showLanguageDropdown,
                languageDropdownOffset = uiState.languageDropdownOffset,
                languageDropdownSize = uiState.languageDropdownSize,
                showExportFormatPicker = uiState.showExportFormatPicker,
                isImportBusy = uiState.isImportBusy,
                isExportBusy = uiState.isExportBusy,
                fileOperationStage = uiState.fileOperationStage
            )
        }

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

    fun openAllPasswords(query: String = "", category: String? = null) {
        allPasswordsSearchQuery = query
        allPasswordsCategory = category
        onAllPasswordsSearchQueryChange(query)
        onAllPasswordsCategoryChange(category)
        uiState = uiState.copy(route = MainRoute.AllPasswords)
    }

    fun selectTab(tab: TabItem) {
        if (tab == uiState.selectedTab) return
        appearanceTransition?.clear()
        val updateTab = {
            uiState = uiState.copy(selectedTab = tab, showThemeDropdown = false, showLanguageDropdown = false)
        }
        if (touchInput) {
            val logicalDirection = if (tab.ordinal > uiState.selectedTab.ordinal) 1 else -1
            val direction = if (layoutDirection == LayoutDirection.Rtl) -logicalDirection else logicalDirection
            tabTransition.requestNavigation(direction, updateTab)
        } else {
            tabTransition.clear()
            updateTab()
        }
    }

    Box(modifier.fillMaxSize()) {
    AnimatedContent(
        targetState = uiState.route,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            // Every route fills the viewport; no container size interpolation is needed.
            mainRouteTransition(initialState, targetState).using(null)
        },
        label = "main_route_content"
    ) routeContent@{ route ->
        when (route) {
            is MainRoute.EditPassword -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    val currentPassword = route.initialPassword ?: passwords.find { it.id == route.passwordId }
                    EditScreen(
                        password = currentPassword,
                        currentLanguage = displayedLanguage,
                        randomPasswordSpec = preferencesManager?.randomPasswordSpec,
                        availableCategories = categories,
                        loadAllPasswords = loadAllPasswords,
                        onBack = { uiState = uiState.copy(route = route.returnRoute) },
                        onSave = { updatedPassword ->
                            onSavePassword?.invoke(updatedPassword)
                        },
                        onDelete = {
                            route.passwordId?.let { onDeletePassword?.invoke(it) }
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
                    isBusy = uiState.isImportBusy,
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
                Box(modifier = Modifier.fillMaxSize()) {
                    AllPasswordsScreen(
                        currentLanguage = displayedLanguage,
                        onBack = { uiState = uiState.copy(route = MainRoute.Tabs) },
                        pagedPasswords = pagedPasswords,
                        passwordCount = passwordCount,
                        categories = categories,
                        selectedCategory = allPasswordsCategory,
                        onCategorySelected = { category ->
                            allPasswordsCategory = category
                            onAllPasswordsCategoryChange(category)
                        },
                        onAddPassword = { uiState = uiState.copy(route = MainRoute.EditPassword(null, returnRoute = MainRoute.AllPasswords)) },
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
                Box(modifier = Modifier.fillMaxSize()) {
                    HelpContent(
                        currentLanguage = displayedLanguage,
                        onBack = { uiState = uiState.copy(route = MainRoute.Tabs) },
                        onNavigateToSearchHelp = { uiState = uiState.copy(route = MainRoute.SearchHelp) },
                        onNavigateToCloudBackupHelp = { uiState = uiState.copy(route = MainRoute.CloudBackupHelp) },
                        scrollState = helpScrollState
                    )
                }
            }

            MainRoute.SearchHelp -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Help) }
                Box(modifier = Modifier.fillMaxSize()) {
                    SearchHelpContent(
                        currentLanguage = displayedLanguage,
                        onBack = { uiState = uiState.copy(route = MainRoute.Help) }
                    )
                }
            }

            MainRoute.CloudBackupHelp -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Help) }
                Box(modifier = Modifier.fillMaxSize()) {
                    CloudBackupHelpContent(
                        currentLanguage = displayedLanguage,
                        onBack = { uiState = uiState.copy(route = MainRoute.Help) }
                    )
                }
            }

            MainRoute.Privacy -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Tabs) }
                Box(modifier = Modifier.fillMaxSize()) {
                    PrivacyContent(currentLanguage = displayedLanguage, onBack = { uiState = uiState.copy(route = MainRoute.Tabs) })
                }
            }

            MainRoute.About -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Tabs) }
                Box(modifier = Modifier.fillMaxSize()) {
                    AboutContent(currentLanguage = displayedLanguage, onBack = { uiState = uiState.copy(route = MainRoute.Tabs) })
                }
            }

            MainRoute.MasterPasswordSetup -> {
                BackHandler { uiState = uiState.copy(route = MainRoute.Tabs) }
                Box(modifier = Modifier.fillMaxSize()) {
                    SetupMasterPasswordScreen(
                        preferencesManager = preferencesManager ?: return@routeContent,
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
                Box(modifier = Modifier.fillMaxSize()) {
                    RandomPasswordSettingsScreen(
                        preferencesManager = preferencesManager,
                        currentLanguage = displayedLanguage,
                        onBack = { uiState = uiState.copy(route = MainRoute.Tabs) }
                    )
                }
            }

            MainRoute.WeakPasswords -> {
                val weakPasswords by weakPasswordsFlow.collectAsStateWithLifecycle()
                WeakPasswordsScreen(
                    currentLanguage = displayedLanguage,
                    items = healthSnapshot?.weakPasswords ?: weakPasswords,
                    isLoading = isHealthLoading,
                    errorMessage = healthErrorMessage,
                    onRetry = onRetryData,
                    assessments = healthSnapshot?.assessmentsById.orEmpty(),
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
                val reusedPasswordGroups by reusedPasswordGroupsFlow.collectAsStateWithLifecycle()
                ReusedPasswordsScreen(
                    currentLanguage = displayedLanguage,
                    groups = healthSnapshot?.reusedPasswordGroups ?: reusedPasswordGroups,
                    isLoading = isHealthLoading,
                    errorMessage = healthErrorMessage,
                    onRetry = onRetryData,
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
                    modifier = Modifier.fillMaxSize(),
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
                                isChinese = displayedLanguage == AppLanguage.CHINESE,
                                onTabSelected = ::selectTab,
                                onAddClick = { uiState = uiState.copy(route = MainRoute.EditPassword(null)) }
                            )
                        }
                    }
                ) { innerPadding ->
                    AppearanceTransitionHost(
                        controller = tabTransition,
                        provideController = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(themeColors.background)
                    ) {
                            when (uiState.selectedTab) {
                            TabItem.HOME -> HomeContent(
                                passwords = homePasswordSnapshot,
                                selectedCategory = uiState.selectedCategory,
                                searchQuery = uiState.searchQuery,
                                currentLanguage = displayedLanguage,
                                welcomeText = welcomeText,
                                searchPlaceholder = searchPlaceholder,
                                passwordCount = passwordCount,
                                securityStats = securityStats,
                                availableCategories = categories,
                                recentText = recentText,
                                viewAllText = viewAllText,
                                onCategorySelected = {
                                    uiState = uiState.copy(selectedCategory = it)
                                    onHomeCategoryChange(it)
                                },
                                onSearchQueryChange = {
                                    uiState = uiState.copy(searchQuery = it)
                                    onHomeSearchQueryChange(it)
                                },
                                onNavigateToAllPasswords = { openAllPasswords(uiState.searchQuery, uiState.selectedCategory) },
                                onOpenAllPasswords = { openAllPasswords() },
                                onNavigateToSecurity = { selectTab(TabItem.SECURITY) },
                                onPasswordClick = { id ->
                                    uiState = uiState.copy(
                                        route = MainRoute.EditPassword(
                                            passwordId = id,
                                            initialPassword = passwords.find { it.id == id }
                                        )
                                    )
                                },
                                onAddPassword = { uiState = uiState.copy(route = MainRoute.EditPassword(null)) },
                                isLoading = isHomeLoading,
                                errorMessage = homeErrorMessage,
                                onRetry = onRetryData,
                                isHealthLoading = isHealthLoading,
                                healthErrorMessage = healthErrorMessage,
                                scrollState = homeScrollState
                            )

                            TabItem.SECURITY -> SecurityContent(
                                currentLanguage = displayedLanguage,
                                stats = securityStats,
                                isLoading = isHealthLoading,
                                errorMessage = healthErrorMessage,
                                onRetry = onRetryData,
                                onOpenAllPasswords = { openAllPasswords() },
                                onOpenWeakPasswords = { uiState = uiState.copy(route = MainRoute.WeakPasswords) },
                                onOpenReusedPasswords = { uiState = uiState.copy(route = MainRoute.ReusedPasswords) },
                                scrollState = securityScrollState
                            )

                            TabItem.SETTINGS -> SettingsContent(
                                currentLanguage = displayedLanguage,
                                preferencesManager = preferencesManager,
                                biometricEnabled = biometricEnabled,
                                onBiometricEnabledChange = { enabled ->
                                    val unavailableMessage = if (enabled) {
                                        biometricUnavailableMessage(context, displayedLanguage)
                                    } else {
                                        null
                                    }
                                    if (unavailableMessage == null) {
                                        biometricEnabled = enabled
                                        preferencesManager?.biometricEnabled = enabled
                                    } else {
                                        Toast.makeText(context, unavailableMessage, Toast.LENGTH_LONG).show()
                                    }
                                },
                                onNavigateToImport = {
                                    fileActions.launchImportPicker()
                                },
                                onNavigateToExport = {
                                    uiState = uiState.copy(showExportFormatPicker = true)
                                },
                                onThemeDropdownToggle = { offset, size ->
                                    uiState = uiState.copy(
                                        showThemeDropdown = !uiState.showThemeDropdown,
                                        themeDropdownOffset = offset,
                                        themeDropdownSize = size,
                                        showLanguageDropdown = false
                                    )
                                },
                                currentThemeLabel = currentThemeLabel,
                                onLanguageDropdownToggle = { offset, size ->
                                    uiState = uiState.copy(
                                        showLanguageDropdown = !uiState.showLanguageDropdown,
                                        languageDropdownOffset = offset,
                                        languageDropdownSize = size,
                                        showThemeDropdown = false
                                    )
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
                                localItemCount = passwordCount,
                                loadAllPasswords = loadAllPasswords,
                                replaceVaultPasswords = { restored ->
                                    onReplacePasswords?.invoke(restored)
                                },
                                scrollState = cloudScrollState
                            )
                        }


                    }
                }
            }
        }
    }
        MainOverlayLayer(
            state = overlayState,
            currentLanguage = displayedLanguage,
            currentTheme = currentTheme,
            languageKey = languageKey,
            themeOptions = themeOptionSet,
            languageOptions = languageOptionSet,
            onDismissTheme = { uiState = uiState.copy(showThemeDropdown = false) },
            onThemeSelected = { option, origin ->
                uiState = uiState.copy(showThemeDropdown = false)
                if (option.value != currentTheme) {
                    val applyTheme = {
                        preferencesManager?.theme = option.value
                        onThemeChanged?.invoke()
                        Unit
                    }
                    val nextDark = when (option.value) {
                        "DARK" -> true
                        "SYSTEM" -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                        else -> false
                    }
                    tabTransition.clear()
                    if (appearanceTransition != null && nextDark != themeColors.isDark && touchInput) {
                        appearanceTransition.requestTheme(origin, applyTheme)
                    } else {
                        appearanceTransition?.clear()
                        applyTheme()
                    }
                }
            },
            onDismissLanguage = { uiState = uiState.copy(showLanguageDropdown = false) },
            onLanguageSelected = { option ->
                uiState = uiState.copy(showLanguageDropdown = false)
                if (option.value != languageKey) {
                    val applyLanguage = {
                        preferencesManager?.language = option.value
                        onThemeChanged?.invoke()
                        Unit
                    }
                    tabTransition.clear()
                    if (appearanceTransition != null && touchInput) {
                        appearanceTransition.requestLanguage(applyLanguage)
                    } else {
                        appearanceTransition?.clear()
                        applyLanguage()
                    }
                }
            },
            onExportFormatSelected = { format ->
                uiState = uiState.copy(showExportFormatPicker = false)
                fileActions.requestExport(format)
            },
            onDismissExportPicker = { uiState = uiState.copy(showExportFormatPicker = false) }
        )
    }
}

private fun biometricUnavailableMessage(
    context: Context,
    language: AppLanguage
): String? {
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    return when (BiometricManager.from(context).canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> null
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> if (language == AppLanguage.CHINESE) {
            "请先在系统设置中录入指纹"
        } else {
            "Enroll a fingerprint in system settings first."
        }
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> if (language == AppLanguage.CHINESE) {
            "此设备不支持指纹解锁"
        } else {
            "This device does not support fingerprint unlock."
        }
        else -> if (language == AppLanguage.CHINESE) {
            "指纹功能暂时不可用，请稍后重试"
        } else {
            "Fingerprint is temporarily unavailable. Try again later."
        }
    }
}

private fun mainRouteTransition(
    initialRoute: MainRoute,
    targetRoute: MainRoute
): ContentTransform {
    val transition = when (mainNavigationDirection(initialRoute, targetRoute)) {
        MainNavigationDirection.FORWARD -> {
            slideInHorizontally(
                initialOffsetX = { width -> width / 12 },
                animationSpec = tween(MotionDuration.Emphasized, easing = MotionEasing.EnterExit)
            ) + fadeIn(tween(MotionDuration.Standard, easing = MotionEasing.EnterExit)) togetherWith slideOutHorizontally(
                targetOffsetX = { width -> -width / 24 },
                animationSpec = tween(MotionDuration.Exit, easing = MotionEasing.EnterExit)
            ) + fadeOut(tween(MotionDuration.Exit, easing = MotionEasing.EnterExit))
        }

        MainNavigationDirection.BACKWARD -> {
            slideInHorizontally(
                initialOffsetX = { width -> -width / 24 },
                animationSpec = tween(MotionDuration.Emphasized, easing = MotionEasing.EnterExit)
            ) + fadeIn(tween(MotionDuration.Standard, easing = MotionEasing.EnterExit)) togetherWith slideOutHorizontally(
                targetOffsetX = { width -> width / 12 },
                animationSpec = tween(MotionDuration.Exit, easing = MotionEasing.EnterExit)
            ) + fadeOut(tween(MotionDuration.Exit, easing = MotionEasing.EnterExit))
        }

        MainNavigationDirection.REPLACE -> {
            fadeIn(tween(MotionDuration.Standard, easing = MotionEasing.EnterExit)) togetherWith
                fadeOut(tween(MotionDuration.Fast, easing = MotionEasing.EnterExit))
        }
    }
    return transition
}

