package com.example.passcard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.Radius18
import com.example.passcard.ui.theme.MotionDuration
import com.example.passcard.ui.theme.MotionEasing
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing16
import com.example.passcard.ui.theme.Spacing4
import com.example.passcard.ui.theme.rememberThemeColors
import com.example.passcard.data.PasswordCategories

@Composable
fun CategoryTag(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val themeColors = rememberThemeColors()
    val background by animateColorAsState(
        targetValue = if (selected) themeColors.primary else themeColors.surface,
        animationSpec = tween(durationMillis = MotionDuration.Fast, easing = MotionEasing.EnterExit),
        label = "category_bg"
    )
    val content by animateColorAsState(
        targetValue = if (selected) themeColors.onPrimary else themeColors.onSurfaceVariant,
        animationSpec = tween(durationMillis = MotionDuration.Fast, easing = MotionEasing.EnterExit),
        label = "category_text"
    )

    PressableScale(onClick = onClick, enabled = enabled, modifier = modifier.semantics { this.selected = selected }) {
        Box(
            modifier = Modifier.heightIn(min = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.heightIn(min = 34.dp)
                .clip(RoundedCornerShape(Radius18))
                .background(background)
                .padding(horizontal = Spacing16, vertical = 6.dp),
                contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = content
                )
            }
        }
    }
}

@Composable
fun CategoryTagRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    isChinese: Boolean = true
) {
    val categoryKeys = remember(categories, selectedCategory) {
        listOf<String?>(null) + PasswordCategories.availableKeys(categories + listOfNotNull(selectedCategory))
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing4),
        horizontalArrangement = Arrangement.spacedBy(Spacing12)
    ) {
        items(
            items = categoryKeys,
            key = { category -> if (category == null) "all" else "category:$category" },
            contentType = { "category" }
        ) { category ->
            CategoryTag(
                label = category?.let { PasswordCategories.label(it, isChinese) } ?: if (isChinese) "全部" else "All",
                selected = if (category == null) selectedCategory == null else selectedCategory != null && PasswordCategories.matches(selectedCategory, category),
                onClick = { onCategorySelected(category) }
            )
        }
    }
}
