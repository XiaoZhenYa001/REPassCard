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
                text = "Security Center",
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
                description = "Your password health is looking good, but there are a few items to fix."
            )
            
            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecurityStatCard(
                    icon = Icons.Outlined.Storage,
                    value = "142",
                    label = "Total Passwords",
                    backgroundColor = Surface,
                    iconTint = OnSurfaceVariant,
                    valueColor = TextPrimary,
                    labelColor = OnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                
                SecurityStatCard(
                    icon = Icons.Outlined.Warning,
                    value = "3",
                    label = "Weak Passwords",
                    backgroundColor = ErrorLight,
                    iconTint = Error,
                    valueColor = Error,
                    labelColor = Error,
                    modifier = Modifier.weight(1f)
                )
                
                SecurityStatCard(
                    icon = Icons.Outlined.Refresh,
                    value = "12",
                    label = "Reused",
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
                    text = "Attention Needed",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                
                SecurityListItem(
                    iconBackgroundColor = ErrorContainer,
                    icon = Icons.Outlined.LockOpen,
                    iconTint = Error,
                    title = "Compromised Passwords",
                    description = "1 account found in data breaches",
                    onClick = onNavigateToCompromised
                )
                
                SecurityListItem(
                    iconBackgroundColor = WarningLight,
                    icon = Icons.Outlined.Warning,
                    iconTint = Warning,
                    title = "Weak Passwords",
                    description = "3 accounts need stronger passwords",
                    onClick = onNavigateToWeak
                )
            }
            
            // Security Suggestions Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Security Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                
                SecuritySuggestionItem(
                    title = "Enable 2-Factor Auth",
                    description = "Add an extra layer of security to your main vault account."
                )
            }
        }
    }
}
