package com.example.passcard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.Radius18
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing16
import com.example.passcard.ui.theme.Spacing4
import com.example.passcard.ui.theme.rememberThemeColors

@Composable
fun CategoryTag(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    val background by animateColorAsState(
        targetValue = if (selected) themeColors.primary else themeColors.surface,
        animationSpec = tween(durationMillis = 150),
        label = "category_bg"
    )
    val content by animateColorAsState(
        targetValue = if (selected) Color.White else themeColors.onSurfaceVariant,
        animationSpec = tween(durationMillis = 150),
        label = "category_text"
    )

    PressableScale(onClick = onClick, modifier = modifier, pressScale = 0.96f) {
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(Radius18))
                .background(background)
                .padding(horizontal = Spacing16),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.W700 else FontWeight.W600
                ),
                color = content
            )
        }
    }
}

@Composable
fun CategoryTagRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = Spacing4),
        horizontalArrangement = Arrangement.spacedBy(Spacing12)
    ) {
        categories.forEach { category ->
            CategoryTag(
                label = category,
                selected = selectedCategory == category,
                onClick = { onCategorySelected(if (selectedCategory == category) null else category) }
            )
        }
    }
}
