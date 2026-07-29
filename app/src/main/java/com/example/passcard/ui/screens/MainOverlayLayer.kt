package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
    val isImportBusy: Boolean
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

    if (state.isImportBusy) {
        ImportBusyOverlay(currentLanguage = currentLanguage)
    }
}

@Composable
private fun ImportBusyOverlay(currentLanguage: AppLanguage) {
    val themeColors = LocalThemeColors.current
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
                    text = if (currentLanguage == AppLanguage.CHINESE) "正在解析导入文件..." else "Parsing import file...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.onBackground
                )
            }
        }
    }
}
