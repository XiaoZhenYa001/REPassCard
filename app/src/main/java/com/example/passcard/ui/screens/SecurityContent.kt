package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.passcard.data.PasswordSecurityStats
import com.example.passcard.ui.components.SectionTitle
import com.example.passcard.ui.components.SecurityListItem
import com.example.passcard.ui.components.SecurityScoreCard
import com.example.passcard.ui.components.SecurityOperationState
import com.example.passcard.ui.components.SecurityStatCard
import com.example.passcard.ui.components.SecuritySuggestionItem
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.ui.theme.Spacing10
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing20
import com.example.passcard.ui.theme.Spacing24

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SecurityContent(
    currentLanguage: AppLanguage,
    stats: PasswordSecurityStats,
    onOpenAllPasswords: () -> Unit,
    onOpenWeakPasswords: () -> Unit,
    onOpenReusedPasswords: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {}
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE

    val scoreDesc = when {
        stats.totalCount == 0 -> if (isZh) "保险库为空，添加密码后即可生成安全评分。" else "Your vault is empty. Add passwords to generate a score."
        !stats.hasScore -> if (isZh) "当前记录未保存可评估的密码，暂无评分。" else "These records have no password to assess. No score is available."
        stats.weakCount == 0 && stats.reusedCount == 0 -> if (isZh) "本地检查暂未发现明显的弱密码或重复使用。" else "Local checks found no obvious weak or reused passwords."
        else -> if (isZh) "评分基于密码长度、常见模式和重复使用，请优先处理下方项目。" else "The score uses length, predictable patterns and reuse. Review the items below first."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing20, vertical = Spacing20),
        verticalArrangement = Arrangement.spacedBy(Spacing24)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isZh) "安全中心" else "Security Center",
                style = MaterialTheme.typography.headlineLarge,
                color = themeColors.onBackground
            )
        }
        if (isLoading || errorMessage != null) {
            SecurityOperationState(
                currentLanguage = currentLanguage,
                errorMessage = errorMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
            )
            return@Column
        }
        SecurityScoreCard(
            score = stats.score,
            description = scoreDesc,
            currentLanguage = currentLanguage,
            hasPasswords = stats.hasScore
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val singleColumn = maxWidth < 240.dp * LocalDensity.current.fontScale
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing12),
                verticalArrangement = Arrangement.spacedBy(Spacing12),
                maxItemsInEachRow = if (singleColumn) 1 else 3
            ) {
                SecurityStatCard(
                    icon = Icons.Outlined.Storage,
                    value = stats.totalCount.toString(),
                    label = if (isZh) "记录总数" else "Records",
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
                    backgroundColor = if (stats.weakCount > 0) themeColors.errorContainer else themeColors.surfaceVariant,
                    iconTint = if (stats.weakCount > 0) themeColors.error else themeColors.onSurfaceVariant,
                    valueColor = if (stats.weakCount > 0) themeColors.error else themeColors.onBackground,
                    labelColor = themeColors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenWeakPasswords
                )
                SecurityStatCard(
                    icon = Icons.Outlined.Refresh,
                    value = stats.reusedCount.toString(),
                    label = if (isZh) "重复使用" else "Reused",
                    backgroundColor = if (stats.reusedCount > 0) themeColors.warningContainer else themeColors.surfaceVariant,
                    iconTint = if (stats.reusedCount > 0) themeColors.warning else themeColors.onSurfaceVariant,
                    valueColor = if (stats.reusedCount > 0) themeColors.warning else themeColors.onBackground,
                    labelColor = themeColors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenReusedPasswords
                )
            }
        }
        Text(
            text = if (isZh) {
                "仅在本机进行启发式检查，未查询密码泄露。" +
                    if (stats.passwordlessCount > 0) " ${stats.passwordlessCount} 条未保存密码的记录不参与评分。" else ""
            } else {
                "Local heuristics only; breaches are not checked." +
                    if (stats.passwordlessCount > 0) " ${stats.passwordlessCount} records without a password are excluded." else ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.onSurfaceVariant
        )
        if (stats.weakCount > 0 || stats.reusedCount > 0) Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing10)) {
            SectionTitle(title = if (isZh) "需要关注" else "Attention Needed", colors = themeColors)
            if (stats.weakCount > 0) SecurityListItem(
                iconBackgroundColor = themeColors.errorContainer,
                icon = Icons.Outlined.LockOpen,
                iconTint = themeColors.error,
                title = if (isZh) "弱密码" else "Weak Passwords",
                description = if (isZh) "${stats.weakCount} 个账户需要更强的密码" else "${stats.weakCount} accounts need stronger passwords",
                onClick = onOpenWeakPasswords
            )
            if (stats.reusedCount > 0) SecurityListItem(
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
                title = if (isZh) "为每个账户使用独立密码" else "Use a unique password for each account",
                description = if (isZh) {
                    "建议使用至少 15 个字符的独立随机密码，或由不相关词语组成的长口令。修改记录前，先在对应网站或应用中更换密码；重要账户建议开启两步验证。"
                } else {
                    "Use a unique random password of at least 15 characters, or a long passphrase made of unrelated words. Change it on the service before updating the saved item, and enable 2FA for important accounts."
                }
            )
        }
    }
}
