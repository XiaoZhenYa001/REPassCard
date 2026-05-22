package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.crypto.RecoveryPhraseManager
import com.example.passcard.ui.theme.LocalThemeColors
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
fun RecoveryPhraseScreen(
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    onGenerated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    var phrase by remember { mutableStateOf(RecoveryPhraseManager.generatePhrase()) }
    var hiddenOnce by remember { mutableStateOf(false) }
    var word3 by remember { mutableStateOf("") }
    var word8 by remember { mutableStateOf("") }
    var word17 by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    val checkPositions = remember { intArrayOf(3, 8, 17) }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = themeColors.onBackground,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = if (isZh) "生成恢复助记词" else "Generate Recovery Phrase",
                style = MaterialTheme.typography.titleLarge,
                color = themeColors.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Security, contentDescription = null, tint = themeColors.primary)
                    Text(
                        text = if (isZh) "请抄写并妥善保存，丢失后无法恢复云端数据。" else "Write it down and keep it safe. Loss means cloud data cannot be recovered.",
                        color = themeColors.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(text = if (hiddenOnce) "•••••• •••••• •••••• ..." else phrase, color = themeColors.onBackground)
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(phrase)); hiddenOnce = true }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(if (isZh) "复制助记词" else "Copy Phrase")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isZh) "确认你已保存" else "Confirm you saved it",
                    color = themeColors.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isZh) "请输入第 3、8、17 个词以确认已抄写正确。" else "Enter the 3rd, 8th, and 17th words to confirm you saved the phrase.",
                    color = themeColors.onSurfaceVariant
                )
                val expectedWords = remember(phrase) {
                    RecoveryPhraseManager.wordsAtOneBasedPositions(phrase, checkPositions)
                }
                OutlinedTextField(
                    value = word3,
                    onValueChange = { word3 = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isZh) "第 3 个词" else "Word #3") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = word8,
                    onValueChange = { word8 = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isZh) "第 8 个词" else "Word #8") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = word17,
                    onValueChange = { word17 = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isZh) "第 17 个词" else "Word #17") },
                    singleLine = true
                )
                val canContinue = RecoveryPhraseManager.normalize(word3) == expectedWords[0] &&
                    RecoveryPhraseManager.normalize(word8) == expectedWords[1] &&
                    RecoveryPhraseManager.normalize(word17) == expectedWords[2]
                Button(
                    onClick = {
                        confirmed = true
                        onGenerated(phrase)
                        hiddenOnce = true
                    },
                    enabled = canContinue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(if (isZh) "确认并返回" else "Confirm and Return")
                }
                if (confirmed) {
                    Text(
                        text = if (isZh) "已确认保存。" else "Saved confirmation accepted.",
                        color = themeColors.success,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Button(
            onClick = {
                phrase = RecoveryPhraseManager.generatePhrase()
                word3 = ""
                word8 = ""
                word17 = ""
                confirmed = false
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
        ) {
            Text(if (isZh) "重新生成" else "Regenerate")
        }
    }
}
