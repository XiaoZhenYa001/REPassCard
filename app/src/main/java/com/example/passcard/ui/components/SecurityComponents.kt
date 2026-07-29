package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.screens.AppLanguage
import com.example.passcard.ui.theme.ElevationLevel
import com.example.passcard.ui.theme.Radius10
import com.example.passcard.ui.theme.Radius12
import com.example.passcard.ui.theme.Radius16
import com.example.passcard.ui.theme.Radius18
import com.example.passcard.ui.theme.Radius24
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
    currentLanguage: AppLanguage = AppLanguage.CHINESE
) {
    val themeColors = rememberThemeColors()
    val safeScore = score.coerceIn(0, 100)
    val isZh = currentLanguage == AppLanguage.CHINESE
    val grade = when {
        safeScore >= 85 -> if (isZh) "优秀" else "Excellent"
        safeScore >= 70 -> if (isZh) "良好" else "Good"
        safeScore >= 50 -> if (isZh) "一般" else "Fair"
        else -> if (isZh) "需要改进" else "Needs Work"
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
                text = if (isZh) "安全评分" else "Security Score",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W700),
                color = Color.White.copy(alpha = 0.62f)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(Radius10))
                    .background(themeColors.success.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = themeColors.success,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = safeScore.toString(), style = SecurityScoreStyle, color = Color.White)
            Spacer(modifier = Modifier.width(Spacing8))
            Text(
                text = "/ 100",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W500),
                color = Color.White.copy(alpha = 0.56f),
                modifier = Modifier.padding(bottom = 9.dp)
            )
        }

        LinearProgressIndicator(
            progress = { safeScore / 100f },
            color = themeColors.success,
            trackColor = Color.White.copy(alpha = 0.14f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Text(
            text = grade,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W700),
            color = themeColors.success
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
                .height(108.dp)
                .appleSurface(colors = themeColors, radius = Radius18)
                .padding(Spacing16),
            verticalArrangement = Arrangement.SpaceBetween
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
