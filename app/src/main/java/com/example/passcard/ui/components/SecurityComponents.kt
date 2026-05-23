package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.screens.AppLanguage
import com.example.passcard.ui.theme.PrimaryDark
import com.example.passcard.ui.theme.rememberThemeColors

@Composable
fun SecurityScoreCard(
    score: Int,
    description: String,
    currentLanguage: AppLanguage = AppLanguage.CHINESE,
    modifier: Modifier = Modifier
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
            .clip(RoundedCornerShape(24.dp))
            .background(if (themeColors.isDark) Color(0xFF1F1F23) else PrimaryDark)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isZh) "安全评分" else "Security Score",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                color = themeColors.tabInactive
            )
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = themeColors.success,
                modifier = Modifier.size(24.dp)
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = safeScore.toString(),
                style = TextStyle(
                    fontWeight = FontWeight.W800,
                    fontSize = 64.sp,
                    lineHeight = 64.sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "/ 100",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W500),
                color = themeColors.tabInactive,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        LinearProgressIndicator(
            progress = { safeScore / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Text(
            text = grade,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = themeColors.success
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.tabInactive,
            lineHeight = 20.sp
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                style = TextStyle(fontWeight = FontWeight.W700, fontSize = 24.sp),
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.W500),
                color = labelColor
            )
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(themeColors.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
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
            tint = themeColors.tabInactive,
            modifier = Modifier.size(20.dp)
        )
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
            .clip(RoundedCornerShape(16.dp))
            .background(themeColors.successContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = themeColors.success,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                color = if (themeColors.isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (themeColors.isDark) Color(0xFFBBF7D0) else Color(0xFF166534),
                lineHeight = 17.sp
            )
        }
    }
}
