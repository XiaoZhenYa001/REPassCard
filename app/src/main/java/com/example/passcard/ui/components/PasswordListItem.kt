package com.example.passcard.ui.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.Radius13
import com.example.passcard.ui.theme.Radius18
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing14
import com.example.passcard.ui.theme.Spacing16
import com.example.passcard.ui.theme.appleSurface
import com.example.passcard.ui.theme.rememberThemeColors
import com.example.passcard.util.PasswordIconType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordListItem(
    name: String,
    email: String,
    password: String,
    iconText: String,
    iconType: String = PasswordIconType.GENERATED,
    iconValue: String = "",
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeColors = rememberThemeColors()
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .appleSurface(colors = themeColors, radius = Radius18)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {},
                onDoubleClick = { copyToClipboard(context, password) }
            )
            .padding(horizontal = Spacing14),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PasswordIcon(
            label = name.ifBlank { iconText },
            iconType = iconType,
            iconValue = iconValue,
            size = 42.dp,
            cornerRadius = Radius13
        )

        Spacer(modifier = Modifier.width(Spacing12))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                color = themeColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

private fun copyToClipboard(context: Context, text: String) {
    ClipboardHelper.copyToClipboard(context, text, label = "Password", showToast = true)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimplePasswordListItem(
    name: String,
    email: String,
    iconText: String,
    iconType: String = PasswordIconType.GENERATED,
    iconValue: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .appleSurface(colors = themeColors, radius = Radius18)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {}
            )
            .padding(horizontal = Spacing14),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PasswordIcon(
            label = name.ifBlank { iconText },
            iconType = iconType,
            iconValue = iconValue,
            size = 42.dp,
            cornerRadius = Radius13
        )

        Spacer(modifier = Modifier.width(Spacing12))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                color = themeColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
