package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.components.PasswordListItem
import com.example.passcard.ui.theme.*

@Composable
fun AllPasswordsScreen(
    onBack: () -> Unit,
    passwords: List<PasswordItem> = emptyList(),
    onPasswordClick: (String) -> Unit,
    currentLanguage: AppLanguage = AppLanguage.CHINESE,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    
    val samplePasswords = remember(currentLanguage) {
        listOf(
            PasswordItem(id = "1", name = "Google Account", username = "alex@gmail.com", email = "alex@gmail.com",
                password = "MySecretPassword123", category = if (currentLanguage == AppLanguage.CHINESE) "社交媒体" else "Social Media", note = ""),
            PasswordItem(id = "2", name = "Netflix", username = "alex@gmail.com", email = "alex@gmail.com",
                password = "NetflixPass456", category = if (currentLanguage == AppLanguage.CHINESE) "娱乐" else "Entertainment", note = ""),
            PasswordItem(id = "3", name = "Facebook", username = "alex.morgan", email = "alex@design.com",
                password = "FacebookPass789", category = if (currentLanguage == AppLanguage.CHINESE) "社交媒体" else "Social Media", note = ""),
            PasswordItem(id = "4", name = "Twitter", username = "alex_twitter", email = "",
                password = "TwitterPass000", category = "", note = ""),
            PasswordItem(id = "5", name = "Amazon", username = "alex@amazon.com", email = "alex@amazon.com",
                password = "AmazonPass111", category = if (currentLanguage == AppLanguage.CHINESE) "购物" else "Shopping", note = "")
        )
    }
    
    val displayPasswords = if (passwords.isEmpty()) samplePasswords else passwords
    
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(47.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "9:41",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                color = themeColors.onBackground
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = themeColors.onBackground,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = AppStrings.allPasswords(currentLanguage),
                style = MaterialTheme.typography.titleLarge,
                color = themeColors.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = themeColors.onBackground,
                modifier = Modifier.size(24.dp)
            )
        }
        
        var searchQuery by remember { mutableStateOf("") }
        
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(themeColors.surfaceVariant),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔍", style = MaterialTheme.typography.bodyLarge, color = themeColors.muted)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                val textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = themeColors.onBackground)
                
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxSize().padding(start = 44.dp, end = 16.dp),
                    textStyle = textStyle,
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(text = AppStrings.searchPasswords(currentLanguage), style = textStyle.copy(color = themeColors.muted))
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = AppStrings.itemsCount(displayPasswords.size, currentLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            items(displayPasswords) { item ->
                PasswordListItem(
                    name = item.name,
                    email = item.email.ifEmpty { item.username },
                    password = item.password,
                    iconText = item.name.take(1).uppercase(),
                    iconBackgroundColor = themeColors.iconBackground,
                    iconTextColor = themeColors.onBackground,
                    onClick = { onPasswordClick(item.id) }
                )
            }
        }
    }
}
