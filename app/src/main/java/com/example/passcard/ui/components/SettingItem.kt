package com.example.passcard.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.IconBgPurple
import com.example.passcard.ui.theme.ProfileIconSize
import com.example.passcard.ui.theme.Radius10
import com.example.passcard.ui.theme.Radius14
import com.example.passcard.ui.theme.Radius16
import com.example.passcard.ui.theme.Radius18
import com.example.passcard.ui.theme.SettingIconSize
import com.example.passcard.ui.theme.SettingItemHeight
import com.example.passcard.ui.theme.Spacing4
import com.example.passcard.ui.theme.Spacing8
import com.example.passcard.ui.theme.Spacing10
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing14
import com.example.passcard.ui.theme.Spacing16
import com.example.passcard.ui.theme.Spacing20
import com.example.passcard.ui.theme.ThemeColors
import com.example.passcard.ui.theme.appleSurface
import com.example.passcard.ui.theme.rememberThemeColors

@Composable
fun SettingItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    showChevron: Boolean = true,
    iconBackgroundColor: Color = IconBgPurple,
    onPositioned: ((offset: IntOffset, size: IntSize) -> Unit)? = null,
    colors: ThemeColors = rememberThemeColors()
) {
    val positionedModifier = if (onPositioned != null) {
        Modifier.onGloballyPositioned { coordinates ->
            val position = coordinates.positionInWindow()
            onPositioned(
                IntOffset(position.x.toInt(), position.y.toInt()),
                IntSize(coordinates.size.width, coordinates.size.height)
            )
        }
    } else {
        Modifier
    }

    PressableScale(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(positionedModifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SettingItemHeight)
                .appleSurface(colors = colors, radius = Radius16)
                .padding(horizontal = Spacing16, vertical = Spacing10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing14)
        ) {
            SettingIconBox(icon = icon, label = label, backgroundColor = iconBackgroundColor)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W500),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SettingToggleItem(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = IconBgPurple,
    colors: ThemeColors = rememberThemeColors()
) {
    PressableScale(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SettingItemHeight)
                .appleSurface(colors = colors, radius = Radius16)
                .padding(horizontal = Spacing16, vertical = Spacing10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing14)
        ) {
            SettingIconBox(icon = icon, label = label, backgroundColor = iconBackgroundColor)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IosToggle(checked = checked, colors = colors)
        }
    }
}

@Composable
fun ProfileCard(
    userName: String,
    userEmail: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    @DrawableRes avatarResId: Int? = null,
    colors: ThemeColors = rememberThemeColors()
) {
    if (onClick == null) {
        ProfileCardContent(
            userName = userName,
            userEmail = userEmail,
            avatarResId = avatarResId,
            showChevron = false,
            colors = colors,
            modifier = modifier
        )
    } else {
        PressableScale(onClick = onClick, modifier = modifier.fillMaxWidth()) {
            ProfileCardContent(
                userName = userName,
                userEmail = userEmail,
                avatarResId = avatarResId,
                showChevron = true,
                colors = colors
            )
        }
    }
}

@Composable
private fun ProfileCardContent(
    userName: String,
    userEmail: String,
    @DrawableRes avatarResId: Int?,
    showChevron: Boolean,
    colors: ThemeColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .appleSurface(colors = colors, radius = Radius18)
            .padding(Spacing16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing16)
    ) {
        Box(
            modifier = Modifier
                .size(ProfileIconSize)
                .clip(RoundedCornerShape(Radius14))
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (avatarResId != null) {
                Image(
                    painter = painterResource(id = avatarResId),
                    contentDescription = userName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing4)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing4)
        ) {
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    colors: ThemeColors = rememberThemeColors()
) {
    val displayTitle = if (title.any { it in 'A'..'Z' || it in 'a'..'z' }) {
        title.uppercase()
    } else {
        title
    }
    Text(
        text = displayTitle,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.W700),
        color = colors.muted,
        modifier = modifier.padding(start = Spacing4)
    )
}

@Composable
private fun SettingIconBox(
    icon: ImageVector,
    label: String,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .size(SettingIconSize)
            .clip(RoundedCornerShape(Radius10))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun IosToggle(
    checked: Boolean,
    colors: ThemeColors
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.success else colors.surfaceVariant,
        animationSpec = tween(durationMillis = 160),
        label = "toggle_track"
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = tween(durationMillis = 180),
        label = "toggle_offset"
    )

    Box(
        modifier = Modifier
            .width(50.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset, y = 2.dp)
                .size(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White)
        )
    }
}
