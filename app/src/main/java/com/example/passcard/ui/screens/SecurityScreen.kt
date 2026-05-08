package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.components.*
import com.example.passcard.ui.theme.*

@Composable
fun SecurityScreen(
    onBack: () -> Unit,
    onNavigateToCompromised: () -> Unit,
    onNavigateToWeak: () -> Unit,
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
                .height(47.dp)
        )
        
        // Nav Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "安全中心",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }
        
        // Scroll Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Security Score Card
            SecurityScoreCard(
                score = 85,
                description = "您的密码健康状况良好，但有几项需要修复。"
            )
            
            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecurityStatCard(
                    icon = Icons.Outlined.Storage,
                    value = "142",
                    label = "密码总数",
                    backgroundColor = Surface,
                    iconTint = OnSurfaceVariant,
                    valueColor = TextPrimary,
                    labelColor = OnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                
                SecurityStatCard(
                    icon = Icons.Outlined.Warning,
                    value = "3",
                    label = "弱密码",
                    backgroundColor = ErrorLight,
                    iconTint = Error,
                    valueColor = Error,
                    labelColor = Error,
                    modifier = Modifier.weight(1f)
                )
                
                SecurityStatCard(
                    icon = Icons.Outlined.Refresh,
                    value = "12",
                    label = "重复使用",
                    backgroundColor = WarningContainer,
                    iconTint = Warning,
                    valueColor = Warning,
                    labelColor = Warning,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Attention Needed Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "需要注意",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                
                SecurityListItem(
                    iconBackgroundColor = ErrorContainer,
                    icon = Icons.Outlined.LockOpen,
                    iconTint = Error,
                    title = "泄露密码",
                    description = "1 个账户在数据泄露中发现",
                    onClick = onNavigateToCompromised
                )
                
                SecurityListItem(
                    iconBackgroundColor = WarningLight,
                    icon = Icons.Outlined.Warning,
                    iconTint = Warning,
                    title = "弱密码",
                    description = "3 个账户需要更强的密码",
                    onClick = onNavigateToWeak
                )
            }
            
            // Security Suggestions Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "安全建议",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                
                SecuritySuggestionItem(
                    title = "启用两步验证",
                    description = "为您的主密码库账户添加额外的安全保护。"
                )
            }
        }
    }
}
