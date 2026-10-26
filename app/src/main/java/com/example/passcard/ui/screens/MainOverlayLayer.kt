package com.example.passcard.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.passcard.ui.components.LoadingIndicator
import com.example.passcard.ui.components.DropdownOption
import com.example.passcard.ui.components.DropdownSelectMenu
import com.example.passcard.ui.components.ExportFormat
import com.example.passcard.ui.components.FormatPickerSheet
import com.example.passcard.ui.theme.LocalThemeColors

@Immutable
data class MainOverlayUiState(
    val showThemeDropdown: Boolean,
    val themeDropdownOffset: IntOffset,
    val themeDropdownSize: IntSize,
    val showLanguageDropdown: Boolean,
    val languageDropdownOffset: IntOffset,
    val languageDropdownSize: IntSize,
    val showExportFormatPicker: Boolean,
    val isImportBusy: Boolean,
    val isExportBusy: Boolean,
    val fileOperationStage: FileOperationStage?
)

@Immutable
data class DropdownOptionSet(
    val items: List<DropdownOption>
)

@Composable
fun MainOverlayLayer(
    state: MainOverlayUiState,
    currentLanguage: AppLanguage,
    currentTheme: String,
    languageKey: String,
    themeOptions: DropdownOptionSet,
    languageOptions: DropdownOptionSet,
    onDismissTheme: () -> Unit,
    onThemeSelected: (DropdownOption) -> Unit,
    onDismissLanguage: () -> Unit,
    onLanguageSelected: (DropdownOption) -> Unit,
    onExportFormatSelected: (ExportFormat) -> Unit,
    onDismissExportPicker: () -> Unit
) {
    if (state.showThemeDropdown) {
        DropdownSelectMenu(
            expanded = true,
            onDismissRequest = onDismissTheme,
            options = themeOptions.items,
            selectedValue = currentTheme,
            onOptionSelected = onThemeSelected,
            offset = state.themeDropdownOffset,
            itemWidth = state.themeDropdownSize.width,
            itemHeight = state.themeDropdownSize.height
        )
    }

    if (state.showLanguageDropdown) {
        DropdownSelectMenu(
            expanded = true,
            onDismissRequest = onDismissLanguage,
            options = languageOptions.items,
            selectedValue = languageKey,
            onOptionSelected = onLanguageSelected,
            offset = state.languageDropdownOffset,
            itemWidth = state.languageDropdownSize.width,
            itemHeight = state.languageDropdownSize.height
        )
    }

    FormatPickerSheet(
        visible = state.showExportFormatPicker,
        currentLanguage = currentLanguage,
        onFormatSelected = onExportFormatSelected,
        onDismiss = onDismissExportPicker
    )

    if (state.isImportBusy || state.isExportBusy) {
        BackHandler {}
        FileBusyOverlay(currentLanguage, state.fileOperationStage)
    }
}

@Composable
private fun FileBusyOverlay(currentLanguage: AppLanguage, stage: FileOperationStage?) {
    val colors = LocalThemeColors.current
    val zh = currentLanguage == AppLanguage.CHINESE
    val message = when (stage) {
        FileOperationStage.IMPORTING -> if (zh) "正在保存所选记录…" else "Saving selected records…"
        FileOperationStage.EXPORTING -> if (zh) "正在准备导出文件…" else "Preparing your export…"
        else -> if (zh) "正在读取并检查文件…" else "Reading and checking the file…"
    }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = colors.surface), shape = RoundedCornerShape(20.dp)) {
            LoadingIndicator(message, Modifier.padding(horizontal = 24.dp, vertical = 20.dp), compact = true)
        }
    }
}
