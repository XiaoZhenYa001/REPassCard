package com.example.passcard.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.passcard.ui.components.PasswordIcon
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.util.ImportIssue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    entries: List<ImportEntry>,
    selectedIds: Set<String>,
    issues: List<ImportIssue>,
    receipt: ImportReceiptUi?,
    currentLanguage: AppLanguage,
    onToggleSelected: (id: String, selected: Boolean) -> Unit,
    onToggleSelectAll: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissReceipt: () -> Unit,
    onPrimaryReceiptAction: () -> Unit,
    onSecondaryReceiptAction: () -> Unit,
    modifier: Modifier = Modifier,
    isBusy: Boolean = false
) {
    val colors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    val selectedCount = selectedIds.size
    val duplicateCount = remember(entries) { entries.count { it.isDuplicate } }
    val allSelected = entries.isNotEmpty() && selectedCount == entries.size
    val selectionState = when {
        allSelected -> ToggleableState.On
        selectedCount == 0 -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }
    var revealedIds by remember(entries) { mutableStateOf(emptySet<String>()) }
    var showIssues by remember(issues) { mutableStateOf(entries.isEmpty() && issues.isNotEmpty()) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, entries) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) revealedIds = emptySet()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = receipt == null || isBusy) {
        if (!isBusy) onCancel()
    }

    fun revealIssues() {
        if (issues.isEmpty()) return
        showIssues = true
        scope.launch { listState.animateScrollToItem(if (entries.isEmpty()) 1 else 2) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onCancel, enabled = !isBusy, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = if (isZh) "关闭导入预览" else "Close import preview",
                    tint = colors.onBackground
                )
            }
            Text(
                text = if (isZh) "导入预览" else "Import preview",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onConfirm,
                enabled = selectedCount > 0 && !isBusy,
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (isZh) "导入 ($selectedCount)" else "Import ($selectedCount)")
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
        ) {
            item(key = "summary", contentType = "summary") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            StatColumn(entries.size.toString(), if (isZh) "总数" else "Total", Modifier.weight(1f))
                            StatColumn(selectedCount.toString(), if (isZh) "已选" else "Selected", Modifier.weight(1f))
                            StatColumn(issues.size.toString(), if (isZh) "异常" else "Issues", Modifier.weight(1f))
                            StatColumn(duplicateCount.toString(), if (isZh) "重复" else "Duplicates", Modifier.weight(1f))
                        }
                        Text(
                            text = if (duplicateCount > 0) {
                                if (isZh) "仅导入已勾选记录，完全相同的重复记录会自动跳过。"
                                else "Only selected records will be imported. Exact duplicates are skipped."
                            } else {
                                if (isZh) "核对记录后确认导入，密码默认隐藏。"
                                else "Review the records before importing. Passwords are hidden by default."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
            if (entries.isNotEmpty()) {
                item(key = "selection", contentType = "selection") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .triStateToggleable(
                                state = selectionState,
                                enabled = !isBusy,
                                role = Role.Checkbox,
                                onClick = { onToggleSelectAll(!allSelected) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TriStateCheckbox(state = selectionState, onClick = null, enabled = !isBusy)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (allSelected) {
                                if (isZh) "取消全选" else "Deselect all"
                            } else {
                                if (isZh) "全选" else "Select all"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$selectedCount / ${entries.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }
            }
            if (issues.isNotEmpty()) {
                item(key = "issues_toggle", contentType = "issues_toggle") {
                    OutlinedButton(
                        onClick = { showIssues = !showIssues },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (showIssues) {
                                if (isZh) "收起异常记录" else "Hide issues"
                            } else {
                                if (isZh) "查看 ${issues.size} 条异常" else "View ${issues.size} issues"
                            }
                        )
                    }
                }
                if (showIssues) {
                    item(key = "issues_explanation", contentType = "issues_explanation") {
                        Text(
                            text = if (isZh) "异常行不会导入。请根据原因检查原文件后重新导入。"
                            else "Rows with issues are excluded. Check the source file and import it again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    itemsIndexed(
                        items = issues,
                        key = { index, issue -> "issue_${issue.rowNumber}_$index" },
                        contentType = { _, _ -> "issue" }
                    ) { _, issue ->
                        ImportIssueItem(issue, currentLanguage)
                    }
                }
            }
            if (entries.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    Card(colors = CardDefaults.cardColors(containerColor = colors.surface)) {
                        Text(
                            text = if (isZh) "没有可导入记录，请检查异常原因或重新选择 CSV / JSON 文件。"
                            else "No importable records. Review the issues or select another CSV / JSON file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(20.dp)
                        )
                    }
                }
            }
            items(items = entries, key = { it.id }, contentType = { "entry" }) { entry ->
                ImportEntryItem(
                    entry = entry,
                    selected = entry.id in selectedIds,
                    passwordVisible = entry.id in revealedIds,
                    currentLanguage = currentLanguage,
                    enabled = !isBusy,
                    onToggleSelected = { checked -> onToggleSelected(entry.id, checked) },
                    onTogglePassword = {
                        revealedIds = if (entry.id in revealedIds) revealedIds - entry.id else revealedIds + entry.id
                    }
                )
            }
        }
    }

    if (receipt != null && !isBusy) {
        ModalBottomSheet(
            onDismissRequest = onDismissReceipt,
            sheetState = sheetState,
            containerColor = colors.surface,
            contentColor = colors.onSurface
        ) {
            ImportReceiptCard(
                receipt = receipt,
                currentLanguage = currentLanguage,
                onDismiss = onDismissReceipt,
                onPrimaryAction = {
                    if (receipt.primaryAction == ImportReceiptActionType.SHOW_ISSUES) revealIssues()
                    onPrimaryReceiptAction()
                },
                onSecondaryAction = {
                    if (receipt.secondaryAction == ImportReceiptActionType.SHOW_ISSUES) revealIssues()
                    onSecondaryReceiptAction()
                },
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)
            )
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalThemeColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun ImportIssueItem(issue: ImportIssue, currentLanguage: AppLanguage) {
    val colors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.warningContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (issue.rowNumber > 0) {
                    if (isZh) "第 ${issue.rowNumber} 行" else "Row ${issue.rowNumber}"
                } else {
                    if (isZh) "文件无法解析" else "Unable to parse file"
                },
                style = MaterialTheme.typography.labelMedium,
                color = colors.warning,
                fontWeight = FontWeight.SemiBold
            )
            // The original row may contain a password, so only the reason is displayed.
            Text(issue.reason, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
        }
    }
}

@Composable
private fun ImportEntryItem(
    entry: ImportEntry,
    selected: Boolean,
    passwordVisible: Boolean,
    currentLanguage: AppLanguage,
    enabled: Boolean,
    onToggleSelected: (Boolean) -> Unit,
    onTogglePassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.primary.copy(alpha = 0.08f).compositeOver(colors.surface) else colors.surface,
        animationSpec = tween(140),
        label = "import_entry_selection"
    )
    val accountText = remember(entry.username, entry.phone, entry.email) {
        listOf(entry.username, entry.phone, entry.email).filter { it.isNotBlank() }.joinToString(" · ")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, if (selected) colors.primary.copy(alpha = 0.35f) else colors.border),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = onToggleSelected)
                .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(checked = selected, onCheckedChange = null, enabled = enabled, modifier = Modifier.size(48.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PasswordIcon(
                        label = entry.service,
                        iconType = entry.iconType,
                        iconValue = entry.iconValue,
                        size = 36.dp,
                        cornerRadius = 10.dp
                    )
                    Text(
                        text = entry.service,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (accountText.isNotEmpty()) {
                    Text(accountText, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (passwordVisible) entry.password else "••••••••",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onTogglePassword, enabled = enabled, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) {
                                if (isZh) "隐藏 ${entry.service} 的密码" else "Hide password for ${entry.service}"
                            } else {
                                if (isZh) "显示 ${entry.service} 的密码" else "Show password for ${entry.service}"
                            },
                            tint = colors.onSurfaceVariant
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entry.category.isNotBlank()) {
                        Text(entry.category, style = MaterialTheme.typography.labelSmall, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Text(
                        text = if (isZh) "第 ${entry.sourceRow} 行" else "Row ${entry.sourceRow}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                if (entry.isDuplicate) {
                    Text(if (isZh) "重复记录 · 导入时跳过" else "Duplicate · skipped during import", style = MaterialTheme.typography.labelSmall, color = colors.warning)
                }
                if (entry.note.isNotBlank()) {
                    Text(entry.note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ImportReceiptCard(
    receipt: ImportReceiptUi,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalThemeColors.current
    val levelColor = when (receipt.level) {
        ImportReceiptLevel.SUCCESS -> colors.success
        ImportReceiptLevel.WARNING -> colors.warning
        ImportReceiptLevel.ERROR -> colors.error
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(levelColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (receipt.level) {
                        ImportReceiptLevel.SUCCESS -> Icons.Outlined.CheckCircle
                        ImportReceiptLevel.WARNING -> Icons.Outlined.Info
                        ImportReceiptLevel.ERROR -> Icons.Outlined.ErrorOutline
                    },
                    contentDescription = null,
                    tint = levelColor
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(receipt.statusLabel, style = MaterialTheme.typography.labelMedium, color = levelColor)
                Text(receipt.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                Text(receipt.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = if (currentLanguage == AppLanguage.CHINESE) "关闭导入结果" else "Dismiss import result", tint = colors.onSurfaceVariant)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.background).padding(14.dp)
        ) {
            ReceiptMetric(receipt.primaryValue, receipt.primaryLabel, Modifier.weight(1f))
            ReceiptMetric(receipt.secondaryValue, receipt.secondaryLabel, Modifier.weight(1f))
            ReceiptMetric(receipt.durationText, if (currentLanguage == AppLanguage.CHINESE) "耗时" else "Duration", Modifier.weight(1f))
        }
        receipt.feedItems.take(3).forEach { item ->
            val toneColor = when (item.tone) {
                ImportReceiptFeedTone.SUCCESS -> colors.success
                ImportReceiptFeedTone.WARNING -> colors.warning
                ImportReceiptFeedTone.ERROR -> colors.error
                ImportReceiptFeedTone.INFO -> colors.primary
            }
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.padding(top = 5.dp).size(6.dp).clip(RoundedCornerShape(3.dp)).background(toneColor))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title, style = MaterialTheme.typography.labelMedium, color = colors.onSurface, fontWeight = FontWeight.Medium)
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Text(item.tag, style = MaterialTheme.typography.labelSmall, color = toneColor)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSecondaryAction, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(receipt.secondaryActionText, textAlign = TextAlign.Center)
            }
            Button(onClick = onPrimaryAction, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(receipt.primaryActionText, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ReceiptMetric(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalThemeColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/**
 * 导入条目数据模型
 */
data class ImportEntry(
    val id: String,
    val service: String,
    val username: String,
    val phone: String,
    val email: String,
    val password: String,
    val note: String,
    val category: String = "",
    val sourceRow: Int = 0,
    val isDuplicate: Boolean = false,
    val iconType: String = "generated",
    val iconValue: String = ""
)

enum class ImportReceiptLevel {
    SUCCESS,
    WARNING,
    ERROR
}

enum class ImportReceiptFeedTone {
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}

enum class ImportReceiptActionType {
    START_IMPORT,
    SHOW_ISSUES,
    PICK_FILE,
    CLOSE_PREVIEW
}

data class ImportReceiptFeedItem(
    val title: String,
    val description: String,
    val tag: String,
    val tone: ImportReceiptFeedTone
)

data class ImportReceiptUi(
    val level: ImportReceiptLevel,
    val statusLabel: String,
    val title: String,
    val description: String,
    val primaryValue: String,
    val primaryLabel: String,
    val secondaryValue: String,
    val secondaryLabel: String,
    val durationText: String,
    val primaryActionText: String,
    val secondaryActionText: String,
    val primaryAction: ImportReceiptActionType,
    val secondaryAction: ImportReceiptActionType,
    val feedItems: List<ImportReceiptFeedItem>
)
