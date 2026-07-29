package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
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
import com.example.passcard.ui.components.SectionTitle
import com.example.passcard.ui.components.SecurityListItem
import com.example.passcard.ui.components.SecurityScoreCard
import com.example.passcard.ui.components.SecurityStatCard
import com.example.passcard.ui.components.SecuritySuggestionItem
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.ui.theme.Spacing10
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing20
import com.example.passcard.ui.theme.Spacing24

@Composable
fun SecurityContent(
    currentLanguage: AppLanguage,
    stats: PasswordSecurityStats,
    onOpenAllPasswords: () -> Unit,
    onOpenWeakPasswords: () -> Unit,
    onOpenReusedPasswords: () -> Unit,
    scrollState: ScrollState = rememberScrollState()
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE

    val scoreDesc = when {
        stats.totalCount == 0 -> if (isZh) "保险库为空，添加密码后即可生成安全评分。" else "Your vault is empty. Add passwords to generate a score."
        stats.weakCount == 0 && stats.reusedCount == 0 -> if (isZh) "当前密码健康状况良好，请继续保持。" else "Your password health looks good."
        else -> if (isZh) "评分基于弱密码和重复密码占比，优先处理下方项目。" else "The score is based on weak and reused password ratios. Fix the items below first."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing20, vertical = Spacing20),
        verticalArrangement = Arrangement.spacedBy(Spacing24)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isZh) "安全中心" else "Security Center",
                style = MaterialTheme.typography.displayLarge,
                color = themeColors.onBackground
            )
        }
        SecurityScoreCard(
            score = stats.score,
            description = scoreDesc,
            currentLanguage = currentLanguage
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing12)) {
            SecurityStatCard(
                icon = Icons.Outlined.Storage,
                value = stats.totalCount.toString(),
                label = if (isZh) "密码总数" else "Total",
                backgroundColor = themeColors.surfaceVariant,
                iconTint = themeColors.onSurfaceVariant,
                valueColor = themeColors.onBackground,
                labelColor = themeColors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                onClick = onOpenAllPasswords
            )
            SecurityStatCard(
                icon = Icons.Outlined.Warning,
                value = stats.weakCount.toString(),
                label = if (isZh) "弱密码" else "Weak",
                backgroundColor = themeColors.errorContainer,
                iconTint = themeColors.error,
                valueColor = themeColors.error,
                labelColor = themeColors.error,
                modifier = Modifier.weight(1f),
                onClick = onOpenWeakPasswords
            )
            SecurityStatCard(
                icon = Icons.Outlined.Refresh,
                value = stats.reusedCount.toString(),
                label = if (isZh) "重复使用" else "Reused",
                backgroundColor = themeColors.warningContainer,
                iconTint = themeColors.warning,
                valueColor = themeColors.warning,
                labelColor = themeColors.warning,
                modifier = Modifier.weight(1f),
                onClick = onOpenReusedPasswords
            )
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing10)) {
            SectionTitle(title = if (isZh) "需要关注" else "Attention Needed", colors = themeColors)
            SecurityListItem(
                iconBackgroundColor = themeColors.errorContainer,
                icon = Icons.Outlined.LockOpen,
                iconTint = themeColors.error,
                title = if (isZh) "弱密码" else "Weak Passwords",
                description = if (isZh) "${stats.weakCount} 个账户需要更强的密码" else "${stats.weakCount} accounts need stronger passwords",
                onClick = onOpenWeakPasswords
            )
            SecurityListItem(
                iconBackgroundColor = themeColors.warningContainer,
                icon = Icons.Outlined.Warning,
                iconTint = themeColors.warning,
                title = if (isZh) "重复使用" else "Reused Passwords",
                description = if (isZh) "${stats.reusedCount} 个账户正在共用密码" else "${stats.reusedCount} accounts reuse passwords",
                onClick = onOpenReusedPasswords
            )
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing10)) {
            SectionTitle(title = if (isZh) "安全建议" else "Security Suggestions", colors = themeColors)
            SecuritySuggestionItem(
                title = if (isZh) "优先修复弱密码和重复密码" else "Fix weak and reused passwords first",
                description = if (isZh) {
                    "统计和列表都来自本地数据库查询。重要账户建议使用更长且唯一的随机密码，并开启两步验证。"
                } else {
                    "Stats and lists come from local database queries. Use longer unique random passwords for important accounts and enable 2FA."
                }
            )
        }
    }
}
