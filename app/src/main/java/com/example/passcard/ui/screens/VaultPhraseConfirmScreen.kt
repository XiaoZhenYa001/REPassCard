package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.crypto.RecoveryPhraseManager
import com.example.passcard.ui.theme.LocalThemeColors

@Composable
fun VaultPhraseConfirmScreen(
    currentLanguage: AppLanguage,
    phrase: String,
    onBack: () -> Unit,
    onConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    var input by remember { mutableStateOf("") }
    val expected = RecoveryPhraseManager.normalize(phrase)
    val canContinue = RecoveryPhraseManager.isValidWords(input) && RecoveryPhraseManager.normalize(input) == expected

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = themeColors.onBackground)
            Spacer(modifier = Modifier.padding(6.dp))
            Text(
                text = if (isZh) "确认恢复助记词" else "Confirm Recovery Phrase",
                color = themeColors.onBackground,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isZh) "请重新输入 24 词助记词。" else "Please re-enter the 24-word phrase.",
                    color = themeColors.onBackground
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isZh) "助记词" else "Recovery Phrase") },
                    minLines = 4
                )
                Text(
                    text = if (isZh) "仅当完全匹配时才能继续。" else "You can continue only when it matches exactly.",
                    color = themeColors.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = { onConfirmed(expected) },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Check, null)
            Spacer(modifier = Modifier.padding(6.dp))
            Text(if (isZh) "确认" else "Confirm")
        }

        TextButton(onClick = onBack) {
            Text(if (isZh) "返回" else "Back")
        }
    }
}
