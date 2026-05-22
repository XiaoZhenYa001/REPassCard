package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.passcard.data.PasswordSecurityStats
import com.example.passcard.ui.components.SecurityListItem
import com.example.passcard.ui.components.SecurityScoreCard
import com.example.passcard.ui.components.SecurityStatCard
import com.example.passcard.ui.components.SecuritySuggestionItem
import com.example.passcard.ui.theme.LocalThemeColors

@Composable
fun SecurityContent(
    currentLanguage: AppLanguage,
    stats: PasswordSecurityStats
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE

    val scoreDesc = when {
        stats.totalCount == 0 -> if (isZh) "保险库为空，添加密码后即可生成安全概览。" else "Your vault is empty. Add passwords to see a security overview."
        stats.weakCount == 0 && stats.reusedCount == 0 -> if (isZh) "当前密码健康状况良好，请继续保持。" else "Your password health looks good."
        else -> if (isZh) "您的密码健康状况良好，但有几项需要修复。" else "Your password health is good, but some items need attention."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isZh) "安全中心" else "Security Center",
                style = MaterialTheme.typography.titleLarge,
                color = themeColors.onBackground
            )
        }
        SecurityScoreCard(score = stats.score, description = scoreDesc)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecurityStatCard(
                icon = Icons.Outlined.Storage,
                value = stats.totalCount.toString(),
                label = if (isZh) "密码总数" else "Total Passwords",
                backgroundColor = themeColors.surfaceVariant,
                iconTint = themeColors.onSurfaceVariant,
                valueColor = themeColors.onBackground,
                labelColor = themeColors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            SecurityStatCard(
                icon = Icons.Outlined.Warning,
                value = stats.weakCount.toString(),
                label = if (isZh) "弱密码" else "Weak Passwords",
                backgroundColor = themeColors.errorContainer,
                iconTint = themeColors.error,
                valueColor = themeColors.error,
                labelColor = themeColors.error,
                modifier = Modifier.weight(1f)
            )
            SecurityStatCard(
                icon = Icons.Outlined.Refresh,
                value = stats.reusedCount.toString(),
                label = if (isZh) "重复使用" else "Reused",
                backgroundColor = themeColors.warningContainer,
                iconTint = themeColors.warning,
                valueColor = themeColors.warning,
                labelColor = themeColors.warning,
                modifier = Modifier.weight(1f)
            )
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = if (isZh) "需要注意" else "Attention Needed",
                style = MaterialTheme.typography.titleMedium,
                color = themeColors.onBackground
            )
            SecurityListItem(
                iconBackgroundColor = themeColors.errorContainer,
                icon = Icons.Outlined.LockOpen,
                iconTint = themeColors.error,
                title = if (isZh) "弱密码" else "Weak Passwords",
                description = if (isZh) "${stats.weakCount} 个账户需要更强的密码" else "${stats.weakCount} accounts need stronger passwords",
                onClick = { }
            )
            SecurityListItem(
                iconBackgroundColor = themeColors.warningContainer,
                icon = Icons.Outlined.Warning,
                iconTint = themeColors.warning,
                title = if (isZh) "重复使用" else "Reused Passwords",
                description = if (isZh) "${stats.reusedCount} 个账户存在重复密码" else "${stats.reusedCount} accounts reuse passwords",
                onClick = { }
            )
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = if (isZh) "安全建议" else "Security Suggestions",
                style = MaterialTheme.typography.titleMedium,
                color = themeColors.onBackground
            )
            SecuritySuggestionItem(
                title = if (isZh) "优先修复弱密码和重复密码" else "Fix weak and reused passwords first",
                description = if (isZh) {
                    "统计数据直接来自本地数据库聚合查询。建议为重要账户使用更长且唯一的密码，并开启两步验证。"
                } else {
                    "Stats come from local database aggregate queries. Use longer unique passwords for important accounts and enable 2FA."
                }
            )
        }
    }
}
