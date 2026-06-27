package com.example.passcard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.Radius14
import com.example.passcard.ui.theme.SearchBarHeight
import com.example.passcard.ui.theme.Spacing14
import com.example.passcard.ui.theme.Spacing8
import com.example.passcard.ui.theme.appleSurface
import com.example.passcard.ui.theme.rememberThemeColors

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "搜索密码...",
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(Radius14)
    val borderColor by animateColorAsState(
        targetValue = if (focused) themeColors.primary else themeColors.border.copy(alpha = 0f),
        animationSpec = tween(durationMillis = 150),
        label = "search_border"
    )
    val iconColor by animateColorAsState(
        targetValue = if (focused) themeColors.primary else themeColors.muted,
        animationSpec = tween(durationMillis = 150),
        label = "search_icon"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SearchBarHeight)
            .appleSurface(colors = themeColors, radius = Radius14)
            .border(width = if (focused) 1.dp else 0.dp, color = borderColor, shape = shape),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(themeColors.surface)
                .padding(horizontal = Spacing14),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing8))

            val textStyle = MaterialTheme.typography.bodyMedium.copy(color = themeColors.onBackground)

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onFocusChanged { focused = it.isFocused },
                textStyle = textStyle,
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = textStyle.copy(color = themeColors.muted)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
