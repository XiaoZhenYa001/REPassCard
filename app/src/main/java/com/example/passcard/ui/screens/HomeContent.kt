package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.passcard.data.PasswordSearch
import com.example.passcard.data.PasswordSearchField
import com.example.passcard.data.PasswordSearchSyntax
import com.example.passcard.ui.components.CategoryTagRow
import com.example.passcard.ui.components.PasswordListItem
import com.example.passcard.ui.components.PressableScale
import com.example.passcard.ui.components.SearchBar
import com.example.passcard.ui.theme.ElevationLevel
import com.example.passcard.ui.theme.IconBgBlue
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.ui.theme.Radius10
import com.example.passcard.ui.theme.Radius18
import com.example.passcard.ui.theme.Radius20
import com.example.passcard.ui.theme.Spacing10
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing16
import com.example.passcard.ui.theme.Spacing20
import com.example.passcard.ui.theme.Spacing24
import com.example.passcard.ui.theme.Spacing4
import com.example.passcard.ui.theme.Spacing8
import com.example.passcard.ui.theme.appleSurface
import com.example.passcard.ui.theme.softShadow

@Composable
fun HomeContent(
    passwords: PasswordListSnapshot,
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
    onAddPassword: () -> Unit,
    scrollState: ScrollState = rememberScrollState()
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    val passwordItems = passwords.items
    val allCategory = if (isZh) "全部" else "All"
    val defaultCategories = if (isZh) {
        listOf("社交媒体", "工作", "金融", "购物", "娱乐", "AI")
    } else {
        listOf("Social Media", "Work", "Finance", "Shopping", "Entertainment", "AI")
    }
    val categories = remember(passwordItems, currentLanguage) {
        listOf(allCategory) + (defaultCategories + passwordItems.map { it.category }.filter { it.isNotBlank() })
            .distinct()
    }
    val isSearching = searchQuery.isNotBlank()
    val parsedSearch = remember(searchQuery) { PasswordSearchSyntax.parse(searchQuery) }
    val filteredPasswords = remember(passwordItems, selectedCategory, searchQuery, parsedSearch) {
        passwordItems.filter { item ->
            val matchCategory = searchQuery.isNotBlank() ||
                selectedCategory == null ||
                selectedCategory == allCategory ||
                item.category == selectedCategory
            val matchSearch = searchQuery.isBlank() || item.matchesPasswordSearch(parsedSearch)
            matchCategory && matchSearch
        }.take(if (searchQuery.isBlank()) 5 else 20)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing20, vertical = Spacing20),
        verticalArrangement = Arrangement.spacedBy(Spacing24)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing4)) {
            Text(text = welcomeText, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
            Text(
                text = if (isZh) "我的保险库" else "My Vault",
                style = MaterialTheme.typography.headlineLarge,
                color = themeColors.onBackground
            )
        }

        SearchBar(value = searchQuery, onValueChange = onSearchQueryChange, placeholder = searchPlaceholder)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing12)) {
            HomeMetricCard(
                text = pwdCountText,
                icon = Icons.Outlined.VpnKey,
                iconColor = IconBgBlue,
                emphasized = false,
                onClick = onNavigateToAllPasswords,
                modifier = Modifier.weight(1f)
            )
            HomeMetricCard(
                text = secScoreText,
                icon = Icons.Outlined.Shield,
                iconColor = Color.White.copy(alpha = 0.20f),
                emphasized = true,
                modifier = Modifier.weight(1f)
            )
        }

        CategoryTagRow(categories = categories, selectedCategory = selectedCategory, onCategorySelected = onCategorySelected)

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing12)) {
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
                PressableScale(onClick = onNavigateToAllPasswords) {
                    Text(
                        text = viewAllText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W700),
                        color = themeColors.primary
                    )
                }
            }
            if (filteredPasswords.isEmpty()) {
                HomeEmptyState(
                    isSearching = searchQuery.isNotBlank() || (selectedCategory != null && selectedCategory != allCategory),
                    isZh = isZh,
                    onAddPassword = onAddPassword
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing10)) {
                    filteredPasswords.forEach { password ->
                        PasswordListItem(
                            name = password.name,
                            email = password.email.ifEmpty { password.username },
                            password = password.password,
                            iconText = password.name.take(1).uppercase(),
                            iconType = password.iconType,
                            iconValue = password.iconValue,
                            copyContentDescription = if (isZh) "复制密码" else "Copy password",
                            copiedToastMessage = if (isZh) "已复制密码" else "Password copied",
                            onClick = { onPasswordClick(password.id) }
                        )
                    }
                }
            }
        }
    }
}

internal fun PasswordItem.matchesPasswordSearch(search: PasswordSearch): Boolean {
    val keyword = search.keyword
    if (keyword.isBlank()) return false

    return when (search.field) {
        PasswordSearchField.NAME -> name.contains(keyword, ignoreCase = true)
        PasswordSearchField.USERNAME -> username.contains(keyword, ignoreCase = true)
        PasswordSearchField.PHONE -> phone.contains(keyword, ignoreCase = true)
        PasswordSearchField.EMAIL -> email.contains(keyword, ignoreCase = true)
        PasswordSearchField.PASSWORD -> password.contains(keyword, ignoreCase = true)
        PasswordSearchField.CATEGORY -> category.contains(keyword, ignoreCase = true)
        PasswordSearchField.NOTE -> note.contains(keyword, ignoreCase = true)
        null -> name.contains(keyword, ignoreCase = true) ||
            username.contains(keyword, ignoreCase = true) ||
            phone.contains(keyword, ignoreCase = true) ||
            email.contains(keyword, ignoreCase = true) ||
            password.contains(keyword, ignoreCase = true) ||
            category.contains(keyword, ignoreCase = true) ||
            note.contains(keyword, ignoreCase = true)
    }
}

@Composable
private fun HomeMetricCard(
    text: String,
    icon: ImageVector,
    iconColor: Color,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val themeColors = LocalThemeColors.current
    val shape = RoundedCornerShape(Radius20)
    val cardModifier = Modifier
        .fillMaxWidth()
        .height(138.dp)
        .let { base ->
            if (emphasized) {
                base
                    .softShadow(colors = themeColors, shape = shape, level = ElevationLevel.Floating)
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(themeColors.primaryGradientStart, themeColors.primaryGradientEnd)
                        )
                    )
            } else {
                base.appleSurface(colors = themeColors, radius = Radius20)
            }
        }
        .padding(Spacing16)

    val content: @Composable () -> Unit = {
        Column(
            modifier = cardModifier,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Radius10))
                    .background(if (emphasized) iconColor else iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (emphasized) Color.White else iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
                color = if (emphasized) Color.White else themeColors.onBackground
            )
        }
    }

    if (onClick != null) {
        PressableScale(onClick = onClick, modifier = modifier) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
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
            .appleSurface(colors = themeColors, radius = Radius18)
            .padding(Spacing24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing10)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(Radius18))
                .background(themeColors.primaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = themeColors.primary)
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
            Spacer(modifier = Modifier.height(Spacing4))
            PressableScale(onClick = onAddPassword) {
                Text(
                    text = if (isZh) "添加密码" else "Add Password",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.W700),
                    color = themeColors.primary
                )
            }
        }
    }
}
