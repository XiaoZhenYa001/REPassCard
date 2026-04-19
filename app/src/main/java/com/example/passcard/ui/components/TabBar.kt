package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.theme.*

enum class TabItem { HOME, SECURITY, CLOUD, SETTINGS }

@Composable
fun TabBar(
    selectedTab: TabItem,
    onTabSelected: (TabItem) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    val bgColor = if (themeColors.isDark) Color(0xFF1A1A1A) else Color.White
    
    Row(
        modifier = modifier
            .width(345.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabButton(
            selected = selectedTab == TabItem.HOME,
            onClick = { onTabSelected(TabItem.HOME) },
            icon = if (selectedTab == TabItem.HOME) Icons.Filled.Home else Icons.Outlined.Home,
            label = "首页",
            colors = themeColors,
            modifier = Modifier.weight(1f)
        )
        TabButton(
            selected = selectedTab == TabItem.SECURITY,
            onClick = { onTabSelected(TabItem.SECURITY) },
            icon = if (selectedTab == TabItem.SECURITY) Icons.Filled.Shield else Icons.Outlined.Shield,
            label = "安全",
            colors = themeColors,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier.width(64.dp).height(54.dp).clip(RoundedCornerShape(27.dp))
                .background(Color.Black).clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(28.dp))
        }
        TabButton(
            selected = selectedTab == TabItem.CLOUD,
            onClick = { onTabSelected(TabItem.CLOUD) },
            icon = if (selectedTab == TabItem.CLOUD) Icons.Filled.Cloud else Icons.Outlined.Cloud,
            label = "加密",
            colors = themeColors,
            modifier = Modifier.weight(1f)
        )
        TabButton(
            selected = selectedTab == TabItem.SETTINGS,
            onClick = { onTabSelected(TabItem.SETTINGS) },
            icon = if (selectedTab == TabItem.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
            label = "设置",
            colors = themeColors,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    colors: ThemeColors,
    modifier: Modifier = Modifier
) {
    val labelColor = if (selected) colors.onBackground else colors.tabInactive
    
    Column(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = labelColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            color = labelColor
        )
    }
}
