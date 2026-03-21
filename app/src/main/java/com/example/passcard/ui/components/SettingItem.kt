package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.theme.*

@Composable
fun SettingItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    showChevron: Boolean = true,
    onPositioned: ((offset: IntOffset, size: IntSize) -> Unit)? = null
) {
    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .then(
                if (onPositioned != null) {
                    Modifier.onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInRoot()
                        val size = coordinates.size
                        onPositioned(
                            IntOffset(position.x.toInt(), position.y.toInt()),
                            IntSize(size.width, size.height)
                        )
                    }
                } else Modifier
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.W600
            ),
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.W500
                ),
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        if (showChevron) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = "More",
                tint = TabInactive,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingToggleItem(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.W600
            ),
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        
        // Toggle Switch
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) Color.Black else OnSurfaceVariant.copy(alpha = 0.3f))
                .padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun ProfileCard(
    userName: String,
    userEmail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(OnSurfaceVariant.copy(alpha = 0.2f))
        ) {
            // TODO: Add user avatar image
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.W600
                ),
                color = TextPrimary
            )
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = "More",
            tint = TabInactive,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 24.sp,
            fontWeight = FontWeight.W800,
            letterSpacing = (-0.5).sp
        ),
        color = TextPrimary,
        modifier = modifier
    )
}
