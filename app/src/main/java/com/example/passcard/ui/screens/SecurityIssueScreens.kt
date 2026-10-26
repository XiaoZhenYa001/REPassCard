package com.example.passcard.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passcard.data.ReusedPasswordGroup
import com.example.passcard.data.PasswordHealthAnalyzer
import com.example.passcard.data.PasswordAssessment
import com.example.passcard.data.PasswordWeakReason
import com.example.passcard.ui.components.SecurityOperationState
import com.example.passcard.ui.theme.LocalThemeColors

@Composable
fun WeakPasswordsScreen(
    currentLanguage: AppLanguage,
    items: List<PasswordItem>,
    onBack: () -> Unit,
    onPasswordClick: (PasswordItem) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    assessments: Map<String, PasswordAssessment> = emptyMap()
) {
    val isZh = currentLanguage == AppLanguage.CHINESE
    BackHandler(onBack = onBack)
    SecurityListScaffold(
        title = if (isZh) "弱密码" else "Weak Passwords",
        subtitle = if (isLoading) (if (isZh) "正在本机检查…" else "Checking locally…") else if (errorMessage != null) (if (isZh) "检查未完成" else "Check incomplete") else if (isZh) "${items.size} 个账户需要关注" else "${items.size} accounts need attention",
        onBack = onBack,
        modifier = modifier
    ) {
        if (isLoading || errorMessage != null) {
            item { SecurityOperationState(currentLanguage, modifier = Modifier.fillMaxWidth().padding(top = 64.dp), errorMessage = errorMessage, onRetry = onRetry) }
        } else if (items.isEmpty()) {
            item { EmptySecurityState(if (isZh) "本地检查未发现明显弱密码" else "No obvious weak passwords found locally") }
        } else {
            items(items, key = { it.id }) { item ->
                WeakPasswordRow(
                    item = item,
                    currentLanguage = currentLanguage,
                    assessment = assessments[item.id],
                    onClick = { onPasswordClick(item) }
                )
            }
        }
    }
}

@Composable
fun ReusedPasswordsScreen(
    currentLanguage: AppLanguage,
    groups: List<ReusedPasswordGroup>,
    onBack: () -> Unit,
    onPasswordClick: (PasswordItem) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {}
) {
    val isZh = currentLanguage == AppLanguage.CHINESE
    BackHandler(onBack = onBack)
    val accountCount = remember(groups) { groups.sumOf { it.count } }
    SecurityListScaffold(
        title = if (isZh) "重复使用" else "Reused Passwords",
        subtitle = if (isLoading) (if (isZh) "正在本机检查…" else "Checking locally…") else if (errorMessage != null) (if (isZh) "检查未完成" else "Check incomplete") else if (isZh) "${groups.size} 组，共 $accountCount 个账户" else "${groups.size} groups, $accountCount accounts",
        onBack = onBack,
        modifier = modifier
    ) {
        if (isLoading || errorMessage != null) {
            item { SecurityOperationState(currentLanguage, modifier = Modifier.fillMaxWidth().padding(top = 64.dp), errorMessage = errorMessage, onRetry = onRetry) }
        } else if (groups.isEmpty()) {
            item { EmptySecurityState(if (isZh) "没有发现重复使用的密码" else "No reused passwords") }
        } else {
            items(groups, key = { it.items.minOf { item -> item.id } }) { group ->
                ReusedPasswordGroupCard(
                    group = group,
                    currentLanguage = currentLanguage,
                    onPasswordClick = onPasswordClick
                )
            }
        }
    }
}

@Composable
private fun SecurityListScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    val themeColors = LocalThemeColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = themeColors.onBackground,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = themeColors.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun WeakPasswordRow(
    item: PasswordItem,
    currentLanguage: AppLanguage,
    assessment: PasswordAssessment?,
    onClick: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    val reasons = remember(assessment, currentLanguage) {
        assessment?.reasons?.map { it.healthDescription(isZh) }.orEmpty()
            .ifEmpty { listOf(if (isZh) "本地检查建议改进" else "Local checks suggest an improvement") }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(themeColors.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(themeColors.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.LockOpen, contentDescription = null, tint = themeColors.error)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.name.ifBlank { if (isZh) "未命名" else "Untitled" },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = themeColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.accountLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = reasons.joinToString(if (isZh) " · " else " / "),
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.error
            )
            Text(
                text = if (isZh) "建议使用独立的长随机密码或不相关词语组成的口令；先在对应服务更换，再更新此记录。" else "Use a unique long random password or a passphrase of unrelated words. Change it on the service, then update this record.",
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReusedPasswordGroupCard(
    group: ReusedPasswordGroup,
    currentLanguage: AppLanguage,
    onPasswordClick: (PasswordItem) -> Unit
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(themeColors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(themeColors.warningContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Warning, contentDescription = null, tint = themeColors.warning)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isZh) "同一密码被 ${group.count} 个账户使用" else "Same password used by ${group.count} accounts",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = themeColors.onBackground
                )
                Text(
                    text = if (isZh) "密码内容已隐藏；为每个账户设置独立密码。" else "Password hidden. Give each account a unique password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant
                )
            }
        }
        group.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPasswordClick(item) }
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name.ifBlank { if (isZh) "未命名" else "Untitled" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = themeColors.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.accountLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptySecurityState(text: String) {
    val themeColors = LocalThemeColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = themeColors.onSurfaceVariant)
    }
}

internal fun PasswordWeakReason.healthDescription(isZh: Boolean): String = when (this) {
    PasswordWeakReason.TOO_SHORT -> if (isZh) "少于 ${PasswordHealthAnalyzer.RECOMMENDED_LENGTH} 个字符" else "Under ${PasswordHealthAnalyzer.RECOMMENDED_LENGTH} characters"
    PasswordWeakReason.COMMON_PASSWORD -> if (isZh) "常见密码或简单变体" else "Common password or simple variation"
    PasswordWeakReason.REPEATED_PATTERN -> if (isZh) "重复字符或重复片段" else "Repeated characters or patterns"
    PasswordWeakReason.PREDICTABLE_SEQUENCE -> if (isZh) "连续字符或键盘顺序" else "Character or keyboard sequence"
}
