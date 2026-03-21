package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.*

enum class TabItem {
    HOME, SECURITY, PLACEHOLDER, SETTINGS
}

@Composable
fun TabBar(
    selectedTab: TabItem,
    onTabSelected: (TabItem) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(345.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home Tab
        TabItem(
            selected = selectedTab == TabItem.HOME,
            onClick = { onTabSelected(TabItem.HOME) },
            icon = if (selectedTab == TabItem.HOME) Icons.Filled.Home else Icons.Outlined.Home,
            label = "首页",
            modifier = Modifier.weight(1f)
        )
        
        // Security Tab
        TabItem(
            selected = selectedTab == TabItem.SECURITY,
            onClick = { onTabSelected(TabItem.SECURITY) },
            icon = if (selectedTab == TabItem.SECURITY) Icons.Filled.Shield else Icons.Outlined.Shield,
            label = "安全",
            modifier = Modifier.weight(1f)
        )
        
        // Add Button
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(Color.Black)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Placeholder Tab
        TabItem(
            selected = selectedTab == TabItem.PLACEHOLDER,
            onClick = { onTabSelected(TabItem.PLACEHOLDER) },
            icon = Icons.Outlined.Shield,
            label = "暂缺",
            modifier = Modifier.weight(1f)
        )
        
        // Settings Tab
        TabItem(
            selected = selectedTab == TabItem.SETTINGS,
            onClick = { onTabSelected(TabItem.SETTINGS) },
            icon = if (selectedTab == TabItem.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
            label = "设置",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) TextPrimary else TabInactive,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TextPrimary else TabInactive
        )
    }
}
