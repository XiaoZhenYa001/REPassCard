package com.example.passcard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.*
import com.example.passcard.util.ImportIssue

@Composable
fun ImportPreviewScreen(
    entries: List<ImportEntry>,
    selectedIds: Set<String>,
    issues: List<ImportIssue>,
    receipt: ImportReceiptUi?,
    onToggleSelected: (id: String, selected: Boolean) -> Unit,
    onToggleSelectAll: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissReceipt: () -> Unit,
    onPrimaryReceiptAction: () -> Unit,
    onSecondaryReceiptAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var revealedIds by remember(entries) { mutableStateOf(emptySet<String>()) }
    val selectedCount = selectedIds.size
    val duplicateCount = entries.count { it.isDuplicate }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Status Bar Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(47.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "9:41",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.W600
                ),
                color = TextPrimary
            )
        }
        
        // Nav Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close",
                tint = TextPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onCancel() }
            )
            
            Text(
                text = "Import Preview",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.W700
                ),
                color = TextPrimary
            )
            
            Text(
                text = "Import",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.W600
                ),
                color = if (selectedCount > 0) Primary else OnSurfaceVariant,
                modifier = Modifier.clickable(enabled = selectedCount > 0) { onConfirm() }
            )
        }
        
        // Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatColumn(
                    value = entries.size.toString(),
                    label = "Total"
                )
                StatColumn(
                    value = selectedCount.toString(),
                    label = "Selected"
                )
                StatColumn(
                    value = issues.size.toString(),
                    label = "Issues"
                )
                StatColumn(
                    value = duplicateCount.toString(),
                    label = "Dup"
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Checkbox(
                    checked = entries.isNotEmpty() && selectedCount == entries.size,
                    onCheckedChange = { checked -> onToggleSelectAll(checked) }
                )
                Text(
                    text = "Select All",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "$selectedCount / ${entries.size}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
        
        // Entry List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 180.dp)
        ) {
            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "当前没有可导入记录，请重新选择 CSV 文件。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
            items(entries) { entry ->
                ImportEntryItem(
                    entry = entry,
                    selected = entry.id in selectedIds,
                    passwordVisible = entry.id in revealedIds,
                    onToggleSelected = { checked -> onToggleSelected(entry.id, checked) },
                    onTogglePassword = {
                        revealedIds = if (entry.id in revealedIds) {
                            revealedIds - entry.id
                        } else {
                            revealedIds + entry.id
                        }
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = receipt != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            receipt?.let {
                ImportReceiptCard(
                    receipt = it,
                    onDismiss = onDismissReceipt,
                    onPrimaryAction = onPrimaryReceiptAction,
                    onSecondaryAction = onSecondaryReceiptAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.W700
            ),
            color = Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun ImportEntryItem(
    entry: ImportEntry,
    selected: Boolean,
    passwordVisible: Boolean,
    onToggleSelected: (Boolean) -> Unit,
    onTogglePassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggleSelected(it) }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IconBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.service.take(1).uppercase().ifEmpty { "?" },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.W600
                ),
                color = TextPrimary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = entry.service,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.W600
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (entry.username.isNotBlank()) {
                    Text(
                        text = entry.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (entry.email.isNotBlank()) {
                    Text(
                        text = entry.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (passwordVisible) entry.password else "•".repeat(entry.password.length.coerceIn(6, 14)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = "Toggle password",
                    tint = OnSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onTogglePassword() }
                )
            }
            
            // Category badge
            if (entry.category.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Primary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = entry.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary
                    )
                }
            }

            if (entry.isDuplicate) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Warning.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "重复记录",
                        style = MaterialTheme.typography.labelSmall,
                        color = Warning
                    )
                }
            }
            Text(
                text = "Row ${entry.sourceRow}",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
            
            if (entry.note.isNotBlank()) {
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Check indicator
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = "Ready to import",
            tint = if (entry.isDuplicate) Warning else Success,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ImportReceiptCard(
    receipt: ImportReceiptUi,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelColor = when (receipt.level) {
        ImportReceiptLevel.SUCCESS -> Success
        ImportReceiptLevel.WARNING -> Warning
        ImportReceiptLevel.ERROR -> Error
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(levelColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (receipt.level) {
                            ImportReceiptLevel.SUCCESS -> "OK"
                            ImportReceiptLevel.WARNING -> "50%"
                            ImportReceiptLevel.ERROR -> "ERR"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = levelColor,
                        fontWeight = FontWeight.W700
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(levelColor.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = receipt.statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = levelColor,
                            fontWeight = FontWeight.W700
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = receipt.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.W700
                    )
                    Text(
                        text = receipt.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onDismiss() }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReceiptMetric(value = receipt.primaryValue, label = receipt.primaryLabel)
                ReceiptMetric(value = receipt.secondaryValue, label = receipt.secondaryLabel)
                ReceiptMetric(value = receipt.durationText, label = "耗时")
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                receipt.feedItems.take(3).forEach { item ->
                    val toneColor = when (item.tone) {
                        ImportReceiptFeedTone.SUCCESS -> Success
                        ImportReceiptFeedTone.WARNING -> Warning
                        ImportReceiptFeedTone.ERROR -> Error
                        ImportReceiptFeedTone.INFO -> Primary
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(toneColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.W600
                            )
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = item.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = toneColor,
                            fontWeight = FontWeight.W700
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSecondaryAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(receipt.secondaryActionText, color = TextPrimary)
                }
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(receipt.primaryActionText)
                }
            }
        }
    }
}

@Composable
private fun ReceiptMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.W700
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
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
    val isDuplicate: Boolean = false
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
