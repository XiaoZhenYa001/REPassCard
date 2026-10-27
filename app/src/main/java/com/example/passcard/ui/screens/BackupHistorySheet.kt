package com.example.passcard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.passcard.sync.CloudSyncRepository
import com.example.passcard.sync.VaultBackupSource
import com.example.passcard.sync.VaultBackupSummary
import com.example.passcard.ui.components.LoadingIndicator
import com.example.passcard.ui.theme.LocalThemeColors
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BackupHistorySheet(
    repository: CloudSyncRepository,
    cloudAvailable: Boolean,
    zh: Boolean,
    onDismiss: () -> Unit,
    onSelect: (VaultBackupSummary) -> Unit
) {
    val colors = LocalThemeColors.current
    var source by remember { mutableStateOf(if (cloudAvailable) VaultBackupSource.CLOUD else VaultBackupSource.LOCAL) }
    var reload by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf(emptyList<VaultBackupSummary>()) }
    LaunchedEffect(repository, source, reload) {
        loading = true
        error = null
        entries = emptyList()
        try {
            entries = (if (source == VaultBackupSource.CLOUD) repository.listCloudBackupHistory()
                else repository.listLocalBackupHistory()).getOrThrow()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: if (zh) "无法读取历史备份" else "Unable to read backup history"
        } finally { loading = false }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface,
        contentColor = colors.onSurface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(
            Modifier.fillMaxWidth().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            item(key = "header", contentType = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (zh) "历史备份" else "Backup history", style = MaterialTheme.typography.titleLarge)
                    Text(if (zh) "选择备份恢复到本机。恢复前会保存当前记录的加密副本，云端内容不会改变。"
                        else "Restore a backup on this device. A copy of your current vault is saved first; cloud data stays unchanged.",
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(source == VaultBackupSource.LOCAL, onClick = { source = VaultBackupSource.LOCAL },
                            label = { Text(if (zh) "本机副本" else "On this device") })
                        FilterChip(source == VaultBackupSource.CLOUD, onClick = { source = VaultBackupSource.CLOUD },
                            enabled = cloudAvailable, label = { Text(if (zh) "云端版本" else "Cloud versions") })
                    }
                    if (!cloudAvailable) Text(if (zh) "完成云端连接后可查看云端版本。本机副本可离线恢复。"
                        else "Connect your cloud to view its versions. Local copies can be restored offline.",
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            when {
                loading -> item(key = "loading") {
                    LoadingIndicator(if (zh) "正在读取备份列表…" else "Reading backup history…",
                        Modifier.fillMaxWidth().padding(vertical = 24.dp))
                }
                error != null -> item(key = "error") {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (zh) "暂时无法读取备份列表" else "Unable to read backup history", color = colors.error)
                        Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        TextButton(onClick = { reload++ }) { Text(if (zh) "重试" else "Try again") }
                    }
                }
                entries.isEmpty() -> item(key = "empty") {
                    Text(if (source == VaultBackupSource.LOCAL) {
                        if (zh) "还没有本机保护副本。每次恢复前都会自动保存当前保险库。"
                        else "No local copies yet. Your current vault is saved before each restore."
                    } else { if (zh) "云端尚无已提交备份" else "No committed cloud backups yet" },
                        style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp))
                }
                else -> items(entries, key = { "${it.source}:${it.id}" }, contentType = { "backup" }) { backup ->
                    Card(onClick = { onSelect(backup) }, colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(if (backup.source == VaultBackupSource.CLOUD) Icons.Outlined.Cloud else Icons.Outlined.Restore,
                                contentDescription = null, tint = colors.primary)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(backupDate(backup.updatedAt, zh), style = MaterialTheme.typography.titleSmall)
                                Text(buildList {
                                    add(backupSize(backup.sizeBytes))
                                    backup.itemCount?.let { add(if (zh) "$it 项" else "$it items") }
                                    backup.vaultRevision?.let { add(if (zh) "修订 $it" else "Revision $it") }
                                    if (backup.isCurrent) add(if (zh) "当前最新" else "Latest")
                                }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                            Text(if (zh) "选择" else "Select", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                        }
                    }
                }
            }
        }
    }
}

internal fun backupDate(timestamp: Long, zh: Boolean): String = if (timestamp <= 0) {
    if (zh) "日期未知" else "Date unavailable"
} else SimpleDateFormat(if (zh) "yyyy/MM/dd HH:mm" else "MMM d, yyyy · HH:mm", if (zh) Locale.CHINA else Locale.US).format(Date(timestamp))

internal fun backupSize(bytes: Long): String = when {
    bytes < 0 -> "—"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.ROOT, "%.1f KiB", bytes / 1024.0)
    else -> String.format(Locale.ROOT, "%.1f MiB", bytes / (1024.0 * 1024))
}
