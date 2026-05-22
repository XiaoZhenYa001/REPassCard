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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.components.DropdownOption
import com.example.passcard.ui.components.DropdownSelectMenu
import com.example.passcard.ui.components.ExportFormat
import com.example.passcard.ui.components.FormatPickerSheet
import com.example.passcard.ui.theme.LocalThemeColors

@Composable
fun MainOverlayLayer(
    uiState: MainUiState,
    currentLanguage: AppLanguage,
    currentTheme: String,
    languageKey: String,
    themeOptions: List<DropdownOption>,
    languageOptions: List<DropdownOption>,
    onDismissTheme: () -> Unit,
    onThemeSelected: (DropdownOption) -> Unit,
    onDismissLanguage: () -> Unit,
    onLanguageSelected: (DropdownOption) -> Unit,
    onExportFormatSelected: (ExportFormat) -> Unit,
    onDismissExportPicker: () -> Unit
) {
    if (uiState.showThemeDropdown) {
        DropdownSelectMenu(
            expanded = true,
            onDismissRequest = onDismissTheme,
            options = themeOptions,
            selectedValue = currentTheme,
            onOptionSelected = onThemeSelected,
            offset = uiState.themeDropdownOffset,
            itemWidth = uiState.themeDropdownSize.width,
            itemHeight = uiState.themeDropdownSize.height
        )
    }

    if (uiState.showLanguageDropdown) {
        DropdownSelectMenu(
            expanded = true,
            onDismissRequest = onDismissLanguage,
            options = languageOptions,
            selectedValue = languageKey,
            onOptionSelected = onLanguageSelected,
            offset = uiState.languageDropdownOffset,
            itemWidth = uiState.languageDropdownSize.width,
            itemHeight = uiState.languageDropdownSize.height
        )
    }

    FormatPickerSheet(
        visible = uiState.showExportFormatPicker,
        currentLanguage = currentLanguage,
        onFormatSelected = onExportFormatSelected,
        onDismiss = onDismissExportPicker
    )

    if (uiState.isImportBusy) {
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
