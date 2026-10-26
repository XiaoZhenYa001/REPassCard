package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.screens.AppLanguage
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.ui.theme.ElevationLevel
import com.example.passcard.ui.theme.Radius10
import com.example.passcard.ui.theme.Radius12
import com.example.passcard.ui.theme.Radius16
import com.example.passcard.ui.theme.Radius18
import com.example.passcard.ui.theme.Radius24
import com.example.passcard.ui.theme.SuccessDark
import com.example.passcard.ui.theme.WarningDark
import com.example.passcard.ui.theme.ErrorDark
import com.example.passcard.ui.theme.ScoreCardBackground
import com.example.passcard.ui.theme.SecurityScoreStyle
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing14
import com.example.passcard.ui.theme.Spacing16
import com.example.passcard.ui.theme.Spacing20
import com.example.passcard.ui.theme.Spacing24
import com.example.passcard.ui.theme.Spacing4
import com.example.passcard.ui.theme.Spacing8
import com.example.passcard.ui.theme.StatValueStyle
import com.example.passcard.ui.theme.appleSurface
import com.example.passcard.ui.theme.rememberThemeColors
import com.example.passcard.ui.theme.softShadow

@Composable
fun SecurityScoreCard(
    score: Int,
    description: String,
    modifier: Modifier = Modifier,
    currentLanguage: AppLanguage = AppLanguage.CHINESE,
    hasPasswords: Boolean = true
) {
    val themeColors = rememberThemeColors()
    val safeScore = score.coerceIn(0, 100)
    val isZh = currentLanguage == AppLanguage.CHINESE
    val grade = when {
        !hasPasswords -> if (isZh) "暂无评分" else "No score yet"
        safeScore >= 85 -> if (isZh) "较高" else "Higher score"
        safeScore >= 70 -> if (isZh) "部分需要关注" else "Some items need attention"
        safeScore >= 50 -> if (isZh) "一般" else "Fair"
        else -> if (isZh) "需要改进" else "Needs Work"
    }
    val statusColor = when {
        !hasPasswords -> Color.White.copy(alpha = 0.66f)
        safeScore >= 85 -> SuccessDark
        safeScore >= 50 -> WarningDark
        else -> ErrorDark
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(
                colors = themeColors,
                shape = RoundedCornerShape(Radius24),
                level = ElevationLevel.Elevated
            )
            .clip(RoundedCornerShape(Radius24))
            .background(ScoreCardBackground)
            .padding(Spacing24),
        verticalArrangement = Arrangement.spacedBy(Spacing14)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isZh) "本地密码健康评分" else "Local password health score",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W700),
                color = Color.White.copy(alpha = 0.62f)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(Radius10))
                    .background(statusColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = if (hasPasswords) safeScore.toString() else "—", style = SecurityScoreStyle, color = Color.White)
            Spacer(modifier = Modifier.width(Spacing8))
            if (hasPasswords) Text(
                text = "/ 100",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W500),
                color = Color.White.copy(alpha = 0.56f),
                modifier = Modifier.padding(bottom = 9.dp)
            )
        }

        if (hasPasswords) LinearProgressIndicator(
            progress = { safeScore / 100f },
            color = statusColor,
            trackColor = Color.White.copy(alpha = 0.14f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Text(
            text = grade,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W700),
            color = statusColor
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.66f)
        )
    }
}

@Composable
fun SecurityStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    backgroundColor: Color,
    iconTint: Color,
    valueColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val themeColors = rememberThemeColors()
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .appleSurface(colors = themeColors, radius = Radius18)
                .padding(Spacing16),
            verticalArrangement = Arrangement.spacedBy(Spacing8)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(Radius10))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(18.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing4)) {
                Text(text = value, style = StatValueStyle, color = valueColor)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.W700),
                    color = labelColor
                )
            }
        }
    }

    if (onClick != null) {
        PressableScale(onClick = onClick, modifier = modifier.fillMaxWidth()) {
            content()
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun SecurityListItem(
    iconBackgroundColor: Color,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()

    PressableScale(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .appleSurface(colors = themeColors, radius = Radius18)
                .padding(Spacing16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing14)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Radius12))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing4)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
                    color = themeColors.onBackground
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = themeColors.muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SecuritySuggestionItem(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .appleSurface(colors = themeColors, radius = Radius18)
            .padding(Spacing16),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing12)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(Radius10))
                .background(themeColors.successContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = themeColors.success,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing4)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W700),
                color = themeColors.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SecurityOperationState(
    currentLanguage: AppLanguage,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    onRetry: () -> Unit = {}
) {
    val isZh = currentLanguage == AppLanguage.CHINESE
    val colors = LocalThemeColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing12)) {
        if (errorMessage == null) {
            LoadingIndicator(
                message = if (isZh) "正在检查密码健康…" else "Checking password health…",
                detail = if (isZh) "只在本机分析，完成后自动显示结果。" else "Analyzed on this device. Results appear when ready."
            )
        } else {
            Text(if (isZh) "暂时无法完成检查" else "Unable to finish the check", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            OutlinedButton(onClick = onRetry) { Text(if (isZh) "重试" else "Retry") }
        }
    }
}
