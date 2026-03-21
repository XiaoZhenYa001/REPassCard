package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.*

data class StatCardData(
    val emoji: String,
    val value: String,
    val isPrimary: Boolean = false
)

@Composable
fun StatCard(
    emoji: String,
    value: String,
    backgroundColor: Color,
    contentColor: Color,
    iconBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        // Value Text
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.W600
            ),
            color = contentColor
        )
    }
}

@Composable
fun StatCardPair(
    card1Data: StatCardData,
    card2Data: StatCardData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            emoji = card1Data.emoji,
            value = card1Data.value,
            backgroundColor = CardGray,
            contentColor = TextPrimary,
            iconBackgroundColor = Color.White,
            modifier = Modifier.weight(1f)
        )
        
        StatCard(
            emoji = card2Data.emoji,
            value = card2Data.value,
            backgroundColor = Primary,
            contentColor = Color.White,
            iconBackgroundColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.weight(1f)
        )
    }
}
