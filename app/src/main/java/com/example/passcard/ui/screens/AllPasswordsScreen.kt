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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.components.PasswordListItem
import com.example.passcard.ui.theme.*

/**
 * 所有密码列表数据模型
 */
data class PasswordItem(
    val id: String,
    val name: String,
    val username: String,
    val phone: String = "",
    val email: String = "",
    val password: String,
    val category: String = "",
    val note: String = ""
)

@Composable
fun AllPasswordsScreen(
    onBack: () -> Unit,
    passwords: List<PasswordItem> = emptyList(),
    onPasswordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 如果没有传入数据，使用示例数据
    val samplePasswords = remember {
        listOf(
            PasswordItem(
                id = "1",
                name = "Google Account",
                username = "alex@gmail.com",
                email = "alex@gmail.com",
                password = "MySecretPassword123",
                category = "Social Media",
                note = "Main Google account"
            ),
            PasswordItem(
                id = "2",
                name = "Netflix",
                username = "alex@gmail.com",
                email = "alex@gmail.com",
                password = "NetflixPass456",
                category = "Entertainment",
                note = ""
            ),
            PasswordItem(
                id = "3",
                name = "Facebook",
                username = "alex.morgan",
                email = "alex@design.com",
                password = "FacebookPass789",
                category = "Social Media",
                note = ""
            ),
            PasswordItem(
                id = "4",
                name = "Twitter",
                username = "alex_twitter",
                email = "",
                password = "TwitterPass000",
                category = "",
                note = ""
            ),
            PasswordItem(
                id = "5",
                name = "Amazon",
                username = "alex@amazon.com",
                email = "alex@amazon.com",
                password = "AmazonPass111",
                category = "Shopping",
                note = "Prime membership"
            )
        )
    }
    
    val displayPasswords = if (passwords.isEmpty()) samplePasswords else passwords
    
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
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "All Passwords",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Search Bar
        var searchQuery by remember { mutableStateOf("") }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Muted
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search passwords...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Muted
                        )
                    }
                }
                
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 44.dp, end = 16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    singleLine = true
                )
            }
        }
        
        // Stats Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${displayPasswords.size} items",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
        
        // Password List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            items(displayPasswords) { item ->
                PasswordListItem(
                    name = item.name,
                    email = item.email.ifEmpty { item.username },
                    password = item.password,
                    iconText = item.name.take(1).uppercase(),
                    iconBackgroundColor = getIconBackgroundColor(item.name),
                    iconTextColor = getIconTextColor(item.name),
                    onClick = { onPasswordClick(item.id) }
                )
            }
        }
    }
}

private fun getIconBackgroundColor(name: String): Color {
    return when (name.lowercase()) {
        "google" -> IconBackground
        "netflix" -> Color.Black
        "facebook" -> Color(0xFF1877F2)
        "twitter" -> Color.Black
        "amazon" -> Color(0xFFFF9900)
        else -> IconBackground
    }
}

private fun getIconTextColor(name: String): Color {
    return when (name.lowercase()) {
        "netflix" -> Color(0xFFE50914)
        "facebook" -> Color.White
        "twitter" -> Color.White
        "amazon" -> Color.White
        else -> TextPrimary
    }
}
