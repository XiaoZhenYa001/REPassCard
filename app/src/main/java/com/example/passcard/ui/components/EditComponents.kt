package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.*

@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    isMultiline: Boolean = false,
    trailingIcons: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
        
        var passwordVisible by remember { mutableStateOf(false) }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isMultiline) Modifier.height(120.dp) else Modifier.height(56.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .then(
                    if (isMultiline) Modifier.padding(16.dp) else Modifier.padding(horizontal = 16.dp)
                ),
            contentAlignment = if (isMultiline) Alignment.TopStart else Alignment.CenterStart
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Normal
                    ),
                    color = Muted,
                    modifier = if (isMultiline) Modifier.padding(top = 0.dp) else Modifier
                )
            }
            
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (isMultiline) Modifier.padding(top = 0.dp) else Modifier),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.W500,
                        color = TextPrimary
                    ),
                    visualTransformation = if (isPassword && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    singleLine = !isMultiline,
                    maxLines = if (isMultiline) 4 else 1
                )
                
                // Trailing icons
                if (isPassword || trailingIcons != {}) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPassword) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy",
                                tint = TextPrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { /* TODO: Copy */ }
                            )
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Hide" else "Show",
                                tint = TextPrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { passwordVisible = !passwordVisible }
                            )
                        }
                        trailingIcons()
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelector(
    selectedCategory: String?,
    categories: List<String>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCategory != null) {
                // Category Pill
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(OnSurfaceVariant.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = selectedCategory,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.W600
                        ),
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove",
                        tint = OnSurfaceVariant,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onCategorySelected("") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Select",
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LogoSelector(
    currentIcon: String,
    onIconChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Logo Container
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Surface),
            contentAlignment = Alignment.Center
        ) {
            // TODO: Replace with actual icon based on currentIcon
            Icon(
                imageVector = Icons.Outlined.Key,
                contentDescription = "Icon",
                tint = Primary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Text(
            text = "Change Icon",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.W600
            ),
            color = TextPrimary,
            modifier = Modifier.clickable { onIconChange() }
        )
    }
}

@Composable
fun DeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ErrorContainer)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = "Delete",
            tint = Error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Delete Password",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.W600
            ),
            color = Error
        )
    }
}
