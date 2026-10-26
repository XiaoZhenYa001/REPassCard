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
import com.example.passcard.util.ImportDeduplication
import com.example.passcard.util.ImportRecordContent
import com.example.passcard.util.PasswordEntryCsv
import com.example.passcard.util.JsonExporter
import com.example.passcard.util.JsonImporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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
    onImportPasswords: (suspend (List<PasswordItem>) -> Unit)?,
    onSavePassword: (suspend (PasswordItem) -> Unit)?
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
    var isImportCommitRunning by remember { mutableStateOf(false) }
    var isFilePreparationRunning by remember { mutableStateOf(false) }
    var isExportRunning by remember { mutableStateOf(false) }

    fun isFileOperationRunning(): Boolean = isImportCommitRunning || isFilePreparationRunning ||
        isExportRunning || latestState.isImportBusy || latestState.isExportBusy

    fun closeImportPreview() {
        updateUiState {
            it.copy(
                route = MainRoute.Tabs,
                importEntries = emptyList(),
                importSelectedIds = emptySet(),
                importIssues = emptyList(),
                importReceipt = null,
                showImportReceipt = false,
                isImportBusy = false,
                fileOperationStage = null
            )
        }
    }

    fun commitSelectedImports(closeAfterSuccess: Boolean) {
        if (isFileOperationRunning()) return
        val state = latestState
        val language = latestLanguage
        if (state.importSelectedIds.isEmpty()) {
            updateUiState {
                it.copy(showImportReceipt = true, importReceipt = buildNoSelectionReceipt(language))
            }
            return
        }

        isImportCommitRunning = true
        updateUiState { it.copy(isImportBusy = true, fileOperationStage = FileOperationStage.IMPORTING) }
        scope.launch {
            val startedAt = System.currentTimeMillis()
            try {
                val passwordSnapshot = withContext(Dispatchers.IO) { latestLoadAllPasswords() }
                val selectedEntries = withContext(Dispatchers.Default) {
                    state.importEntries.filter { it.id in state.importSelectedIds }
                }
                if (selectedEntries.isEmpty()) {
                    updateUiState {
                        it.copy(showImportReceipt = true, importReceipt = buildNoSelectionReceipt(language))
                    }
                    return@launch
                }
                val (toInsert, duplicateSkipped) = withContext(Dispatchers.Default) {
                    val flags = ImportDeduplication.duplicateFlags(
                        passwordSnapshot.map { it.importContent() }, selectedEntries.map { it.importContent() }
                    )
                    val newPasswords = selectedEntries.filterIndexed { index, _ -> !flags[index] }.map { entry ->
                        PasswordItem(
                            id = "import_${UUID.randomUUID()}",
                            name = entry.service,
                            username = entry.username,
                            phone = entry.phone,
                            email = entry.email,
                            password = entry.password,
                            category = entry.category,
                            note = entry.note,
                            iconType = entry.iconType,
                            iconValue = entry.iconValue
                        )
                    }
                    newPasswords to flags.count { it }
                }

                if (toInsert.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val importPasswords = latestOnImportPasswords
                        if (importPasswords != null) {
                            importPasswords(toInsert)
                        } else {
                            val savePassword = latestOnSavePassword
                                ?: error("Vault is not available for import")
                            toInsert.forEach { savePassword(it) }
                        }
                    }
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
                    updateUiState { it.copy(importReceipt = receipt, showImportReceipt = true) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                val message = if (language == AppLanguage.CHINESE) {
                    "导入未完成，请重试。"
                } else {
                    "Import did not complete. Please try again."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } finally {
                isImportCommitRunning = false
                updateUiState { it.copy(isImportBusy = false, fileOperationStage = null) }
            }
        }
    }

    fun exportPasswords(format: ExportFormat) {
        if (isFileOperationRunning()) return
        val language = latestLanguage
        isExportRunning = true
        updateUiState { it.copy(isExportBusy = true, fileOperationStage = FileOperationStage.EXPORTING) }
        scope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    val exportData = latestLoadAllPasswords().map { password ->
                        ExportPasswordEntry(
                            service = password.name,
                            username = password.username,
                            phone = password.phone,
                            email = password.email,
                            password = password.password,
                            note = password.note,
                            category = password.category,
                            iconType = password.iconType,
                            iconValue = password.iconValue
                        )
                    }
                    when (format) {
                        ExportFormat.CSV -> CsvExporter.exportToCsv(context, exportData).getOrThrow()
                        ExportFormat.JSON -> JsonExporter.exportToJson(context, exportData).getOrThrow()
                    }
                }
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
                val chooserTitle = if (language == AppLanguage.CHINESE) "分享导出文件" else "Share export"
                shareLauncher.launch(Intent.createChooser(shareIntent, chooserTitle))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                val failureMessage = if (language == AppLanguage.CHINESE) {
                    "导出失败，无法读取保险库或写入 Documents/PassCard"
                } else {
                    "Export failed. Unable to read the vault or write to Documents/PassCard."
                }
                Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
            } finally {
                isExportRunning = false
                updateUiState { it.copy(isExportBusy = false, fileOperationStage = null) }
            }
        }
    }

    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null || isFileOperationRunning()) return@rememberLauncherForActivityResult
        val language = latestLanguage
        isFilePreparationRunning = true
        updateUiState { it.copy(isImportBusy = true, fileOperationStage = FileOperationStage.PARSING_IMPORT) }
        scope.launch {
            val startedAt = System.currentTimeMillis()
            try {
                val prepared = withContext(Dispatchers.IO) {
                    val passwordSnapshot = latestLoadAllPasswords()
                    val (format, content) = FileFormatDetector.detectFromUri(context, uri)
                    val parsed = when (format) {
                        FileFormatDetector.FileFormat.JSON -> JsonImporter.parseJsonContent(content)
                        FileFormatDetector.FileFormat.CSV -> CsvImporter.parseCsvContent(content)
                    }
                    val flags = ImportDeduplication.duplicateFlags(
                        passwordSnapshot.map { it.importContent() }, parsed.entries.map { it.importContent() }
                    )
                    val batchId = UUID.randomUUID().toString()
                    val entries = parsed.entries.mapIndexed { index, entry ->
                        ImportEntry(
                            id = "preview_${batchId}_$index",
                            service = entry.service,
                            username = entry.username,
                            phone = entry.phone,
                            email = entry.email,
                            password = entry.password,
                            note = entry.note,
                            category = entry.category,
                            sourceRow = entry.sourceRow,
                            isDuplicate = flags[index],
                            iconType = entry.iconType,
                            iconValue = entry.iconValue
                        )
                    }
                    val selectedIds = entries.filterNot { it.isDuplicate }.map { it.id }.toSet()
                    val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "file"
                    PreparedImport(
                        entries = entries,
                        selectedIds = selectedIds,
                        issues = parsed.issues,
                        receipt = buildParseReceipt(parsed, flags.count { it }, fileName,
                            System.currentTimeMillis() - startedAt, language)
                    )
                }
                updateUiState {
                    it.copy(
                        route = MainRoute.ImportPreview,
                        importEntries = prepared.entries,
                        importSelectedIds = prepared.selectedIds,
                        importIssues = prepared.issues,
                        importReceipt = prepared.receipt,
                        showImportReceipt = true
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val reason = error.message ?: if (language == AppLanguage.CHINESE) "无法解析文件" else "Unable to parse file"
                updateUiState {
                    it.copy(
                        route = MainRoute.ImportPreview,
                        importEntries = emptyList(),
                        importSelectedIds = emptySet(),
                        importIssues = listOf(ImportIssue(rowNumber = 0, reason = reason, rawRow = "")),
                        importReceipt = buildParseFailureReceipt(reason, language),
                        showImportReceipt = true
                    )
                }
            } finally {
                isFilePreparationRunning = false
                updateUiState { it.copy(isImportBusy = false, fileOperationStage = null) }
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
                if (!isFileOperationRunning()) importFilePickerLauncher.launch(IMPORT_MIME_TYPES)
            },
            requestExport = requestExport@ { format ->
                if (isFileOperationRunning()) return@requestExport
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
            closeImportPreview = { if (!isFileOperationRunning()) closeImportPreview() },
            commitSelectedImports = ::commitSelectedImports,
            handleReceiptAction = handleReceiptAction@ { action ->
                if (isFileOperationRunning()) return@handleReceiptAction
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

private data class PreparedImport(
    val entries: List<ImportEntry>,
    val selectedIds: Set<String>,
    val issues: List<ImportIssue>,
    val receipt: ImportReceiptUi
)

private fun PasswordItem.importContent() = ImportRecordContent(
    name, username, phone, email, password, note, category, iconType, iconValue
)

private fun PasswordEntryCsv.importContent() = ImportRecordContent(
    service, username, phone, email, password, note, category, iconType, iconValue
)

private fun ImportEntry.importContent() = ImportRecordContent(
    service, username, phone, email, password, note, category, iconType, iconValue
)
