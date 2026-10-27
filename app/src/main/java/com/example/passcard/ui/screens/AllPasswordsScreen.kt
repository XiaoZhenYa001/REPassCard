package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Add
import com.example.passcard.ui.components.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.passcard.ui.components.PasswordListItem
import com.example.passcard.ui.components.SearchBar
import com.example.passcard.ui.components.CategoryTagRow
import com.example.passcard.ui.theme.rememberThemeColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun AllPasswordsScreen(
    onBack: () -> Unit,
    onPasswordClick: (PasswordItem) -> Unit,
    modifier: Modifier = Modifier,
    pagedPasswords: Flow<PagingData<PasswordItem>> = flowOf(PagingData.empty()),
    passwordCount: Int = 0,
    categories: List<String> = emptyList(),
    selectedCategory: String? = null,
    onCategorySelected: (String?) -> Unit = {},
    onAddPassword: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    currentLanguage: AppLanguage = AppLanguage.CHINESE,
    listState: LazyListState = rememberLazyListState()
) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE
    val lazyPasswords = pagedPasswords.collectAsLazyPagingItems()
    val isInitialLoading = lazyPasswords.loadState.refresh is LoadState.Loading
    val isAppendLoading = lazyPasswords.loadState.append is LoadState.Loading
    val refreshError = lazyPasswords.loadState.refresh as? LoadState.Error
    val appendError = lazyPasswords.loadState.append as? LoadState.Error

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = if (isZh) "返回" else "Back",
                tint = themeColors.onBackground,
                modifier = Modifier.size(22.dp)
            ) }
            Text(
                text = AppStrings.allPasswords(currentLanguage),
                style = MaterialTheme.typography.titleLarge,
                color = themeColors.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onAddPassword) { Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = if (isZh) "添加密码" else "Add password",
                tint = themeColors.onBackground,
                modifier = Modifier.size(24.dp)
            ) }
        }

        // Search and feedback scroll with the results when the IME leaves little height.
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item(key = "all_passwords:search", contentType = "search") {
                SearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    currentLanguage = currentLanguage
                )
            }
            if (categories.isNotEmpty() || selectedCategory != null) {
                item(key = "all_passwords:categories", contentType = "categories") {
                    CategoryTagRow(categories, selectedCategory, onCategorySelected,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp), isChinese = isZh)
                }
            }
            item(key = "all_passwords:count", contentType = "count") {
                Text(
                    text = if (isInitialLoading) {
                        if (isZh) "正在加载..." else "Loading..."
                    } else {
                        if (searchQuery.isBlank() && selectedCategory == null) AppStrings.itemsCount(passwordCount, currentLanguage)
                        else if (isZh) "筛选结果" else "Filtered results"
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant
                )
            }
            when {
                isInitialLoading -> item(key = "all_passwords:loading", contentType = "loading") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(if (isZh) "正在读取保险库…" else "Reading your vault…")
                    }
                }
                refreshError != null -> item(key = "all_passwords:error", contentType = "error") {
                    ErrorState(
                        title = if (isZh) "加载失败" else "Failed to load",
                        description = refreshError.error.message ?: if (isZh) "请稍后重试。" else "Please try again later.",
                        onRetry = { lazyPasswords.retry() },
                        currentLanguage = currentLanguage
                    )
                }
                lazyPasswords.itemCount == 0 -> item(key = "all_passwords:empty", contentType = "empty") {
                    EmptyState(
                        hasQuery = searchQuery.isNotBlank() || selectedCategory != null,
                        currentLanguage = currentLanguage,
                        onAddPassword = onAddPassword,
                        onClearFilters = { onSearchQueryChange(""); onCategorySelected(null) }
                    )
                }
                else -> {
                    items(
                        count = lazyPasswords.itemCount,
                        key = lazyPasswords.itemKey { "password:${it.id}" },
                        contentType = { "password" }
                    ) { index ->
                        val item = lazyPasswords[index]
                        if (item != null) {
                            PasswordListItem(
                                name = item.name.ifBlank { if (isZh) "未命名记录" else "Untitled item" },
                                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
                                email = item.accountLabel(),
                                password = item.password,
                                iconText = item.name.take(1).uppercase(),
                                iconType = item.iconType,
                                iconValue = item.iconValue,
                                copyContentDescription = if (isZh) "复制密码" else "Copy password",
                                copiedToastMessage = if (isZh) "已复制密码" else "Password copied",
                                onClick = { onPasswordClick(item) }
                            )
                        }
                    }
                    if (isAppendLoading) {
                        item(key = "all_passwords:append_loading", contentType = "loading") {
                            LoadingMoreRow(currentLanguage = currentLanguage)
                        }
                    }
                    if (appendError != null) {
                        item(key = "all_passwords:append_error", contentType = "error") {
                            TextButton(
                                onClick = { lazyPasswords.retry() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isZh) "加载更多失败，点击重试" else "Load more failed. Tap to retry.")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    currentLanguage: AppLanguage
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)
    ) {
        SearchBar(
            value = value,
            onValueChange = onValueChange,
            placeholder = AppStrings.searchPasswords(currentLanguage),
            clearContentDescription = if (currentLanguage == AppLanguage.CHINESE) "清空搜索" else "Clear search"
        )
    }
}

@Composable
private fun EmptyState(
    hasQuery: Boolean,
    currentLanguage: AppLanguage,
    onAddPassword: () -> Unit,
    onClearFilters: () -> Unit
) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(themeColors.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = themeColors.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = if (hasQuery) {
                    if (isZh) "没有匹配的密码" else "No matching passwords"
                } else {
                    if (isZh) "还没有保存密码" else "No passwords saved yet"
                },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = themeColors.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (hasQuery) {
                    if (isZh) "请尝试其他关键词。" else "Try another keyword."
                } else {
                    if (isZh) "添加第一条记录，开始管理你的密码。" else "Add your first item to start managing passwords."
                },
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = if (hasQuery) onClearFilters else onAddPassword) {
                Text(if (hasQuery) { if (isZh) "清除筛选" else "Clear filters" }
                    else { if (isZh) "添加密码" else "Add password" })
            }
        }
    }
}

@Composable
private fun LoadingMoreRow(currentLanguage: AppLanguage) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        LoadingIndicator(if (currentLanguage == AppLanguage.CHINESE) "正在加载更多…" else "Loading more…", compact = true)
    }
}

@Composable
private fun ErrorState(
    title: String,
    description: String,
    onRetry: () -> Unit,
    currentLanguage: AppLanguage
) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = themeColors.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text(if (isZh) "重试" else "Retry")
        }
    }
}
