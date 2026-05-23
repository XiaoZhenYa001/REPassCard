package com.example.passcard.ui.screens

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.passcard.ui.components.TabItem
import com.example.passcard.util.ImportIssue

data class MainUiState(
    val selectedTab: TabItem = TabItem.HOME,
    val route: MainRoute = MainRoute.Tabs,
    val importEntries: List<ImportEntry> = emptyList(),
    val importSelectedIds: Set<String> = emptySet(),
    val importIssues: List<ImportIssue> = emptyList(),
    val importReceipt: ImportReceiptUi? = null,
    val showImportReceipt: Boolean = false,
    val isImportBusy: Boolean = false,
    val passwords: List<PasswordItem> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val showThemeDropdown: Boolean = false,
    val themeDropdownOffset: IntOffset = IntOffset.Zero,
    val themeDropdownSize: IntSize = IntSize.Zero,
    val showLanguageDropdown: Boolean = false,
    val languageDropdownOffset: IntOffset = IntOffset.Zero,
    val languageDropdownSize: IntSize = IntSize.Zero,
    val showExportFormatPicker: Boolean = false
)

sealed interface MainRoute {
    data object Tabs : MainRoute
    data class EditPassword(
        val passwordId: String?,
        val initialPassword: PasswordItem? = null,
        val returnRoute: MainRoute = Tabs
    ) : MainRoute
    data object ImportPreview : MainRoute
    data object AllPasswords : MainRoute
    data object Help : MainRoute
    data object CloudBackupHelp : MainRoute
    data object Privacy : MainRoute
    data object About : MainRoute
    data object MasterPasswordSetup : MainRoute
    data object RandomPasswordSettings : MainRoute
    data object WeakPasswords : MainRoute
    data object ReusedPasswords : MainRoute
}
