package com.example.passcard.ui.screens

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

@Composable
fun ImportPreviewScreen(
    entries: List<ImportEntry>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                color = Primary,
                modifier = Modifier.clickable { onConfirm() }
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
                    value = entries.count { it.email.isNotBlank() }.toString(),
                    label = "With Email"
                )
                StatColumn(
                    value = entries.count { it.phone.isNotBlank() }.toString(),
                    label = "With Phone"
                )
                StatColumn(
                    value = entries.count { it.category.isNotBlank() }.toString(),
                    label = "With Category"
                )
            }
        }
        
        // Entry List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            items(entries) { entry ->
                ImportEntryItem(entry = entry)
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
            tint = Success,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 导入条目数据模型
 */
data class ImportEntry(
    val service: String,
    val username: String,
    val phone: String,
    val email: String,
    val password: String,
    val note: String,
    val category: String = ""
)
