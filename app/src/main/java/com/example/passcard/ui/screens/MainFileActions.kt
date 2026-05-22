package com.example.passcard.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.passcard.ui.components.ExportFormat
import com.example.passcard.util.CsvExporter
import com.example.passcard.util.CsvImporter
import com.example.passcard.util.ExportPasswordEntry
import com.example.passcard.util.FileFormatDetector
import com.example.passcard.util.ImportIssue
import com.example.passcard.util.JsonExporter
import com.example.passcard.util.JsonImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFileActions(
    val launchImportPicker: () -> Unit,
    val requestExport: (ExportFormat) -> Unit,
    val closeImportPreview: () -> Unit,
    val commitSelectedImports: (Boolean) -> Unit,
    val handleReceiptAction: (ImportReceiptActionType?) -> Unit
)

@Composable
fun rememberMainFileActions(
    uiState: MainUiState,
    currentLanguage: AppLanguage,
    updateUiState: ((MainUiState) -> MainUiState) -> Unit,
    loadAllPasswords: suspend () -> List<PasswordItem>,
    onImportPasswords: ((List<PasswordItem>) -> Unit)?,
    onSavePassword: ((PasswordItem) -> Unit)?
): MainFileActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestState by rememberUpdatedState(uiState)
    val latestLanguage by rememberUpdatedState(currentLanguage)
    val latestLoadAllPasswords by rememberUpdatedState(loadAllPasswords)
    val latestOnImportPasswords by rememberUpdatedState(onImportPasswords)
    val latestOnSavePassword by rememberUpdatedState(onSavePassword)
    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }
    var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }

    fun closeImportPreview() {
        updateUiState {
            it.copy(
                route = MainRoute.Tabs,
                importEntries = emptyList(),
                importSelectedIds = emptySet(),
                importIssues = emptyList(),
                importReceipt = null,
                showImportReceipt = false,
                isImportBusy = false
            )
        }
    }

    fun commitSelectedImports(closeAfterSuccess: Boolean) {
        val state = latestState
        val language = latestLanguage
        val startedAt = System.currentTimeMillis()
        val selectedEntries = state.importEntries.filter { it.id in state.importSelectedIds }
        if (selectedEntries.isEmpty()) {
            updateUiState {
                it.copy(
                    showImportReceipt = true,
                    importReceipt = buildNoSelectionReceipt(language)
                )
            }
            return
        }

        val existingKeys = state.passwords
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

        val importPasswords = latestOnImportPasswords
        if (importPasswords != null) {
            importPasswords(toInsert)
        } else {
            toInsert.forEach { latestOnSavePassword?.invoke(it) }
        }

        val receipt = buildImportDoneReceipt(
            importedCount = toInsert.size,
            duplicateSkipped = duplicateSkipped,
            parseIssueCount = state.importIssues.size,
            selectedCount = selectedEntries.size,
            durationMillis = System.currentTimeMillis() - startedAt,
            language = language
        )

        if (toInsert.isNotEmpty()) {
            val successMessage = if (language == AppLanguage.CHINESE) {
                "导入成功：${toInsert.size} 条"
            } else {
                "Import successful: ${toInsert.size} items"
            }
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
        }

        if (toInsert.isNotEmpty() && closeAfterSuccess) {
            closeImportPreview()
        } else {
            updateUiState {
                it.copy(
                    importReceipt = receipt,
                    showImportReceipt = true
                )
            }
        }
    }

    fun exportPasswords(format: ExportFormat) {
        val language = latestLanguage
        val exportData = latestState.passwords.map { p ->
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
            val successMessage = if (language == AppLanguage.CHINESE) {
                "导出成功，已保存到 Documents/PassCard"
            } else {
                "Export saved to Documents/PassCard."
            }
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
            val shareIntent = when (format) {
                ExportFormat.CSV -> CsvExporter.createShareIntent(uri)
                ExportFormat.JSON -> JsonExporter.createShareIntent(uri)
            }
            shareLauncher.launch(Intent.createChooser(shareIntent, "Export Passwords"))
        }.onFailure {
            val failureMessage = if (language == AppLanguage.CHINESE) {
                "导出失败，无法写入 Documents/PassCard"
            } else {
                "Export failed. Unable to write to Documents/PassCard."
            }
            Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
        }
    }

    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val language = latestLanguage
        scope.launch {
            val startedAt = System.currentTimeMillis()
            updateUiState { it.copy(isImportBusy = true) }
            val passwordSnapshot = withContext(Dispatchers.IO) {
                latestLoadAllPasswords()
            }

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
                updateUiState {
                    it.copy(
                        isImportBusy = false,
                        route = MainRoute.ImportPreview,
                        importEntries = entries,
                        importSelectedIds = selectedIds,
                        importIssues = parsed.issues,
                        importReceipt = buildParseReceipt(
                            parseResult = parsed,
                            duplicateCount = entries.count { entry -> entry.isDuplicate },
                            fileName = fileNameDisplay,
                            durationMillis = System.currentTimeMillis() - startedAt,
                            language = language
                        ),
                        showImportReceipt = true
                    )
                }
            }.onFailure { error ->
                updateUiState {
                    it.copy(
                        isImportBusy = false,
                        route = MainRoute.ImportPreview,
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
                            language = language
                        ),
                        showImportReceipt = true
                    )
                }
            }
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val format = pendingExportFormat
        pendingExportFormat = null
        if (granted && format != null) {
            exportPasswords(format)
        } else {
            val message = if (latestLanguage == AppLanguage.CHINESE) {
                "需要存储权限才能导出到 PassCard 文件夹"
            } else {
                "Storage permission is required to export to the PassCard folder."
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    return remember(importFilePickerLauncher, storagePermissionLauncher) {
        MainFileActions(
            launchImportPicker = {
                importFilePickerLauncher.launch(IMPORT_MIME_TYPES)
            },
            requestExport = { format ->
                if (
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    pendingExportFormat = format
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    exportPasswords(format)
                }
            },
            closeImportPreview = ::closeImportPreview,
            commitSelectedImports = ::commitSelectedImports,
            handleReceiptAction = { action ->
                when (action) {
                    ImportReceiptActionType.START_IMPORT -> commitSelectedImports(true)
                    ImportReceiptActionType.PICK_FILE -> {
                        closeImportPreview()
                        importFilePickerLauncher.launch(IMPORT_MIME_TYPES)
                    }
                    ImportReceiptActionType.SHOW_ISSUES -> {
                        updateUiState { it.copy(showImportReceipt = false) }
                    }
                    ImportReceiptActionType.CLOSE_PREVIEW -> {
                        closeImportPreview()
                    }
                    null -> Unit
                }
            }
        )
    }
}

private val IMPORT_MIME_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "application/csv",
    "text/tab-separated-values",
    "application/json",
    "text/json",
    "*/*"
)
