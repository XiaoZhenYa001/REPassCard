package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.components.CategoryTagRow
import com.example.passcard.ui.components.PasswordListItem
import com.example.passcard.ui.components.SearchBar
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.ui.theme.OnPrimary
import com.example.passcard.ui.theme.Primary

@Composable
fun HomeContent(
    passwords: List<PasswordItem>,
    selectedCategory: String?,
    searchQuery: String,
    currentLanguage: AppLanguage,
    welcomeText: String,
    searchPlaceholder: String,
    pwdCountText: String,
    secScoreText: String,
    recentText: String,
    viewAllText: String,
    onCategorySelected: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToAllPasswords: () -> Unit,
    onPasswordClick: (String) -> Unit,
    onAddPassword: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    val allCategory = if (isZh) "全部" else "All"
    val defaultCategories = if (isZh) {
        listOf("社交媒体", "工作", "金融", "购物", "娱乐", "AI")
    } else {
        listOf("Social Media", "Work", "Finance", "Shopping", "Entertainment", "AI")
    }
    val categories = remember(passwords, currentLanguage) {
        listOf(allCategory) + (defaultCategories + passwords.map { it.category }.filter { it.isNotBlank() })
            .distinct()
    }
    val isSearching = searchQuery.isNotBlank()
    val isFieldSearch = searchQuery.trim().startsWith("/t", ignoreCase = true)
    val filteredPasswords = remember(passwords, selectedCategory, searchQuery) {
        passwords.filter { item ->
            val matchCategory = searchQuery.isNotBlank() ||
                selectedCategory == null ||
                selectedCategory == allCategory ||
                item.category == selectedCategory
            val matchSearch = searchQuery.isBlank() ||
                isFieldSearch ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.username.contains(searchQuery, ignoreCase = true) ||
                item.email.contains(searchQuery, ignoreCase = true) ||
                item.phone.contains(searchQuery, ignoreCase = true) ||
                item.password.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.note.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }.take(if (searchQuery.isBlank()) 5 else 20)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = welcomeText, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
            Text(
                text = if (isZh) "我的保险库" else "My Vault",
                style = MaterialTheme.typography.headlineMedium,
                color = themeColors.onBackground
            )
        }

        SearchBar(value = searchQuery, onValueChange = onSearchQueryChange, placeholder = searchPlaceholder)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(themeColors.surfaceVariant)
                    .clickable { onNavigateToAllPasswords() },
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(themeColors.iconBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.VpnKey, contentDescription = null, tint = themeColors.onBackground)
                    }
                    Text(
                        text = pwdCountText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                        color = themeColors.onBackground
                    )
                }
            }
            Box(
                modifier = Modifier.weight(1f).height(140.dp).clip(RoundedCornerShape(16.dp)).background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(OnPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Shield, contentDescription = null, tint = OnPrimary)
                    }
                    Text(
                        text = secScoreText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                        color = OnPrimary
                    )
                }
            }
        }

        CategoryTagRow(categories = categories, selectedCategory = selectedCategory, onCategorySelected = onCategorySelected)

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isSearching) {
                        if (isZh) "搜索结果" else "Search Results"
                    } else {
                        recentText
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = themeColors.onBackground
                )
                Text(
                    text = viewAllText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W500),
                    color = Primary,
                    modifier = Modifier.clickable { onNavigateToAllPasswords() }
                )
            }
            if (filteredPasswords.isEmpty()) {
                HomeEmptyState(
                    isSearching = searchQuery.isNotBlank() || (selectedCategory != null && selectedCategory != allCategory),
                    isZh = isZh,
                    onAddPassword = onAddPassword
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredPasswords.forEach { password ->
                        PasswordListItem(
                            name = password.name,
                            email = password.email.ifEmpty { password.username },
                            password = password.password,
                            iconText = password.name.take(1).uppercase(),
                            iconType = password.iconType,
                            iconValue = password.iconValue,
                            onClick = { onPasswordClick(password.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyState(
    isSearching: Boolean,
    isZh: Boolean,
    onAddPassword: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(themeColors.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(themeColors.iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = themeColors.onBackground)
        }
        Text(
            text = if (isSearching) {
                if (isZh) "没有匹配的密码" else "No matching passwords"
            } else {
                if (isZh) "还没有保存密码" else "No passwords saved yet"
            },
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = themeColors.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isSearching) {
                if (isZh) "试试其他关键词或分类。" else "Try another keyword or category."
            } else {
                if (isZh) "点击下方添加按钮创建第一条记录。" else "Tap add below to create your first item."
            },
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!isSearching) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isZh) "添加密码" else "Add Password",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Primary,
                modifier = Modifier.clickable { onAddPassword() }
            )
        }
    }
}
