package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passcard.data.PasswordSearch
import com.example.passcard.data.PasswordSearchField
import com.example.passcard.data.PasswordSearchSyntax
import com.example.passcard.data.PasswordCategories
import com.example.passcard.data.PasswordSecurityStats
import com.example.passcard.ui.components.LoadingIndicator
import com.example.passcard.ui.components.CategoryTagRow
import com.example.passcard.ui.components.PasswordListItem
import com.example.passcard.ui.components.PressableScale
import com.example.passcard.ui.components.SearchBar
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
import com.example.passcard.ui.theme.StatValueStyle
import com.example.passcard.ui.theme.appleSurface

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun HomeContent(
    passwords: PasswordListSnapshot,
    selectedCategory: String?,
    searchQuery: String,
    currentLanguage: AppLanguage,
    welcomeText: String,
    searchPlaceholder: String,
    passwordCount: Int,
    securityStats: PasswordSecurityStats,
    availableCategories: List<String>,
    recentText: String,
    viewAllText: String,
    onCategorySelected: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToAllPasswords: () -> Unit,
    onOpenAllPasswords: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onPasswordClick: (String) -> Unit,
    onAddPassword: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    isHealthLoading: Boolean = false,
    healthErrorMessage: String? = null,
    scrollState: LazyListState = rememberLazyListState()
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    val passwordItems = passwords.items
    val isSearching = searchQuery.isNotBlank()
    val isFiltering = isSearching || selectedCategory != null
    val parsedSearch = remember(searchQuery) { PasswordSearchSyntax.parse(searchQuery) }
    val filteredPasswords = remember(passwordItems, selectedCategory, searchQuery, parsedSearch) {
        passwordItems.asSequence().filter { item ->
            val matchCategory = selectedCategory == null || PasswordCategories.matches(item.category, selectedCategory)
            val matchSearch = searchQuery.isBlank() || item.matchesPasswordSearch(parsedSearch)
            matchCategory && matchSearch
        }.take(if (isFiltering) 20 else 5).toList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        state = scrollState,
        contentPadding = PaddingValues(horizontal = Spacing20, vertical = Spacing20)
    ) {
        item(key = "home_header", contentType = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing4)) {
                Text(text = welcomeText, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
                Text(
                    text = if (isZh) "我的保险库" else "My Vault",
                    style = MaterialTheme.typography.headlineLarge,
                    color = themeColors.onBackground
                )
            }
        }

        item(key = "header_search_spacing", contentType = "spacing") { Spacer(Modifier.height(Spacing24)) }
        item(key = "home_search", contentType = "search") {
            SearchBar(value = searchQuery, onValueChange = onSearchQueryChange, placeholder = searchPlaceholder,
                clearContentDescription = if (isZh) "清空搜索" else "Clear search")
        }

        item(key = "search_metrics_spacing", contentType = "spacing") { Spacer(Modifier.height(Spacing24)) }
        item(key = "home_metrics", contentType = "metrics") {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val singleColumn = maxWidth < 280.dp * LocalDensity.current.fontScale
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing12),
                    verticalArrangement = Arrangement.spacedBy(Spacing12),
                    maxItemsInEachRow = if (singleColumn) 1 else 2
                ) {
                    HomeMetricCard(
                        value = if (isLoading && passwordCount == 0) "—" else passwordCount.toString(),
                        label = if (isZh) "所有密码" else "All passwords",
                        icon = Icons.Outlined.VpnKey,
                        iconColor = IconBgBlue,
                        onClick = onOpenAllPasswords,
                        modifier = Modifier.weight(1f)
                    )
                    HomeMetricCard(
                        value = if (!securityStats.hasScore || isHealthLoading || healthErrorMessage != null) "—" else securityStats.score.toString(),
                        valueSuffix = if (securityStats.hasScore && !isHealthLoading && healthErrorMessage == null) "/ 100" else null,
                        label = if (isZh) "密码健康" else "Password health",
                        icon = Icons.Outlined.Shield,
                        iconColor = themeColors.primary,
                        onClick = onNavigateToSecurity,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (availableCategories.isNotEmpty() || selectedCategory != null) {
            item(key = "metrics_categories_spacing", contentType = "spacing") { Spacer(Modifier.height(Spacing16)) }
            item(key = "home_categories", contentType = "categories") {
                CategoryTagRow(categories = availableCategories, selectedCategory = selectedCategory,
                    onCategorySelected = onCategorySelected, isChinese = isZh)
            }
        }

        item(key = "categories_results_spacing", contentType = "spacing") { Spacer(Modifier.height(Spacing24)) }
        item(key = "home_results_header", contentType = "results_header") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing12), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isSearching) {
                        if (isZh) "搜索结果" else "Search Results"
                    } else if (selectedCategory != null) {
                        PasswordCategories.label(selectedCategory, isZh)
                    } else {
                        recentText
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = themeColors.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onNavigateToAllPasswords) {
                    Text(
                        text = viewAllText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W700),
                        color = themeColors.primary
                    )
                }
            }
        }

        if (errorMessage != null) {
            item(key = "home_read_error", contentType = "error") {
                Card(colors = CardDefaults.cardColors(containerColor = themeColors.errorContainer)) {
                    Column(Modifier.fillMaxWidth().padding(Spacing16)) {
                        Text(if (isZh) "暂时无法读取当前结果" else "Unable to read these results",
                            style = MaterialTheme.typography.bodyMedium, color = themeColors.error)
                        Text(if (isZh) "请重试；已有内容不表示本次读取成功。" else "Try again; any existing results may be out of date.",
                            style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
                        TextButton(onClick = onRetry) { Text(if (isZh) "重新加载" else "Try again") }
                    }
                }
            }
        }
        if (isLoading) {
            item(key = "home_loading", contentType = "loading") {
                LoadingIndicator(if (isZh) "正在读取保险库…" else "Reading your vault…",
                    Modifier.fillMaxWidth().padding(vertical = Spacing16), compact = true)
            }
        }
        if (filteredPasswords.isEmpty() && !isLoading && errorMessage == null) {
            item(key = "results_empty_spacing", contentType = "spacing") { Spacer(Modifier.height(Spacing12)) }
            item(key = "home_empty", contentType = "empty") {
                HomeEmptyState(
                    isSearching = isFiltering,
                    isZh = isZh,
                    onAddPassword = onAddPassword,
                    onClearFilters = { onSearchQueryChange(""); onCategorySelected(null) }
                )
            }
        } else {
            itemsIndexed(
                items = filteredPasswords,
                key = { _, password -> password.id },
                contentType = { _, _ -> "password" }
            ) { index, password ->
                PasswordListItem(
                    name = password.name.ifBlank { if (isZh) "未命名记录" else "Untitled item" },
                    email = password.accountLabel(),
                    password = password.password,
                    iconText = password.name.take(1).uppercase(),
                    iconType = password.iconType,
                    iconValue = password.iconValue,
                    copyContentDescription = if (isZh) "复制密码" else "Copy password",
                    copiedToastMessage = if (isZh) "已复制密码" else "Password copied",
                    onClick = { onPasswordClick(password.id) },
                    modifier = Modifier.padding(top = if (index == 0) Spacing12 else Spacing10)
                )
            }
        }
    }
}

internal fun PasswordItem.matchesPasswordSearch(search: PasswordSearch): Boolean {
    val keyword = search.keyword
    if (keyword.isBlank()) return false
    val matchesCategory = category.contains(keyword, ignoreCase = true) ||
        PasswordCategories.searchAliases(keyword).any { PasswordCategories.matches(category, it) }

    return when (search.field) {
        PasswordSearchField.NAME -> name.contains(keyword, ignoreCase = true)
        PasswordSearchField.USERNAME -> username.contains(keyword, ignoreCase = true)
        PasswordSearchField.PHONE -> phone.contains(keyword, ignoreCase = true)
        PasswordSearchField.EMAIL -> email.contains(keyword, ignoreCase = true)
        PasswordSearchField.PASSWORD -> password.contains(keyword, ignoreCase = true)
        PasswordSearchField.CATEGORY -> matchesCategory
        PasswordSearchField.NOTE -> note.contains(keyword, ignoreCase = true)
        null -> name.contains(keyword, ignoreCase = true) ||
            username.contains(keyword, ignoreCase = true) ||
            phone.contains(keyword, ignoreCase = true) ||
            email.contains(keyword, ignoreCase = true) ||
            password.contains(keyword, ignoreCase = true) ||
            matchesCategory ||
            note.contains(keyword, ignoreCase = true)
    }
}

@Composable
private fun HomeMetricCard(
    value: String,
    label: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    valueSuffix: String? = null,
    onClick: (() -> Unit)? = null
) {
    val themeColors = LocalThemeColors.current
    val cardModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 116.dp)
        .appleSurface(colors = themeColors, radius = Radius20)
        .padding(Spacing16)

    val content: @Composable () -> Unit = {
        Column(
            modifier = cardModifier,
            verticalArrangement = Arrangement.spacedBy(Spacing8)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(Radius10))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing4)) {
                Text(
                    text = value,
                    style = StatValueStyle,
                    color = themeColors.onBackground,
                    modifier = Modifier.alignByBaseline()
                )
                if (valueSuffix != null) {
                    Text(
                        text = valueSuffix,
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
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
    onAddPassword: () -> Unit,
    onClearFilters: () -> Unit
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
            Icon(if (isSearching) Icons.Outlined.Search else Icons.Outlined.Add, contentDescription = null, tint = themeColors.primary)
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
        TextButton(onClick = if (isSearching) onClearFilters else onAddPassword) {
            Text(
                text = if (isSearching) { if (isZh) "清除筛选" else "Clear filters" }
                    else { if (isZh) "添加密码" else "Add password" },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.W700),
                color = themeColors.primary
            )
        }
    }
}
