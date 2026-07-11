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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.passcard.ui.components.PasswordListItem
import com.example.passcard.ui.components.SearchBar
import com.example.passcard.ui.theme.rememberThemeColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun AllPasswordsScreen(
    onBack: () -> Unit,
    pagedPasswords: Flow<PagingData<PasswordItem>> = flowOf(PagingData.empty()),
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onPasswordClick: (PasswordItem) -> Unit,
    currentLanguage: AppLanguage = AppLanguage.CHINESE,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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

        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            currentLanguage = currentLanguage
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isInitialLoading) {
                    if (isZh) "正在加载..." else "Loading..."
                } else {
                    AppStrings.itemsCount(lazyPasswords.itemCount, currentLanguage)
                },
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
        }

        when {
            isInitialLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            refreshError != null -> {
                ErrorState(
                    title = if (isZh) "加载失败" else "Failed to load",
                    description = refreshError.error.message ?: if (isZh) "请稍后重试。" else "Please try again later.",
                    onRetry = { lazyPasswords.retry() },
                    currentLanguage = currentLanguage
                )
            }
            lazyPasswords.itemCount == 0 -> {
                EmptyState(
                    hasQuery = searchQuery.isNotBlank(),
                    currentLanguage = currentLanguage
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    items(
                        count = lazyPasswords.itemCount,
                        key = lazyPasswords.itemKey { it.id }
                    ) { index ->
                        val item = lazyPasswords[index]
                        if (item != null) {
                            PasswordListItem(
                                name = item.name,
                                email = item.email.ifEmpty { item.username },
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
                        item { LoadingMoreRow(currentLanguage = currentLanguage) }
                    }
                    if (appendError != null) {
                        item {
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp)
    ) {
        SearchBar(
            value = value,
            onValueChange = onValueChange,
            placeholder = AppStrings.searchPasswords(currentLanguage)
        )
    }
}

@Composable
private fun EmptyState(
    hasQuery: Boolean,
    currentLanguage: AppLanguage
) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
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
                    if (isZh) "回到首页点击 + 添加第一条记录。" else "Go back home and tap + to add your first item."
                },
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingMoreRow(currentLanguage: AppLanguage) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (isZh) "继续加载中..." else "Loading more...",
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.onSurfaceVariant
        )
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
