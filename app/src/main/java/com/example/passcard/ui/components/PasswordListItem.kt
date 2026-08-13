package com.example.passcard.ui.components

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.Radius13
import com.example.passcard.ui.theme.Radius18
import com.example.passcard.ui.theme.MotionDuration
import com.example.passcard.ui.theme.MotionScale
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing14
import com.example.passcard.ui.theme.ThemeColors
import com.example.passcard.ui.theme.appleSurface
import com.example.passcard.ui.theme.rememberThemeColors
import com.example.passcard.util.ClipboardHelper
import com.example.passcard.util.PasswordIconType
import kotlinx.coroutines.delay

@Composable
fun PasswordListItem(
    name: String,
    email: String,
    password: String,
    iconText: String,
    modifier: Modifier = Modifier,
    iconType: String = PasswordIconType.GENERATED,
    iconValue: String = "",
    copyContentDescription: String = "Copy password",
    copiedToastMessage: String = "Password copied",
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeColors = rememberThemeColors()
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .appleSurface(colors = themeColors, radius = Radius18)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
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

        Spacer(modifier = Modifier.width(Spacing12))

        CopyPasswordButton(
            themeColors = themeColors,
            contentDescription = copyContentDescription,
            copiedContentDescription = copiedToastMessage,
            onClick = { copyToClipboard(context, password) }
        )
    }
}

@Composable
private fun CopyPasswordButton(
    themeColors: ThemeColors,
    contentDescription: String,
    copiedContentDescription: String,
    onClick: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPY_FEEDBACK_DURATION_MS)
            copied = false
        }
    }

    PressableScale(
        onClick = {
            onClick()
            copied = true
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        },
        pressScale = MotionScale.StrongPressed
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius13))
                .background(themeColors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = copied,
                animationSpec = tween(MotionDuration.Standard),
                label = "copy_feedback"
            ) { isCopied ->
                Icon(
                    imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    contentDescription = if (isCopied) copiedContentDescription else contentDescription,
                    tint = if (isCopied) themeColors.success else themeColors.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    ClipboardHelper.copyToClipboard(context, text, label = "Password", showToast = false)
}

private const val COPY_FEEDBACK_DURATION_MS = 1_200L

@Composable
fun SimplePasswordListItem(
    name: String,
    email: String,
    iconText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconType: String = PasswordIconType.GENERATED,
    iconValue: String = ""
) {
    val themeColors = rememberThemeColors()
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .appleSurface(colors = themeColors, radius = Radius18)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
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
