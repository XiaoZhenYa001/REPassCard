package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.LocalThemeColors

@Composable
fun VaultFlowScreen(
    currentLanguage: AppLanguage,
    isExport: Boolean,
    onBack: () -> Unit,
    onStartExport: () -> Unit,
    onStartImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = themeColors.onBackground)
        }
        Text(
            text = if (isZh) "加密保险库（Vault）" else "Encrypted Vault",
            color = themeColors.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isZh) {
                if (isExport) {
                    "将使用 24 词 BIP39 词表生成恢复密钥，并导出加密 JSON。请按步骤生成助记词并再次完整确认。"
                } else {
                    "从加密 JSON 文件恢复密码库。需要输入与备份时相同的 24 词助记词。"
                }
            } else {
                if (isExport) {
                    "A 24-word recovery phrase (BIP39 wordlist) will be generated. Export is an encrypted JSON file."
                } else {
                    "Restore from an encrypted vault file. You need the same 24-word phrase used when exporting."
                }
            },
            color = themeColors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Button(
            onClick = if (isExport) onStartExport else onStartImport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isZh) {
                    if (isExport) "下一步：生成助记词" else "下一步：输入助记词并选择文件"
                } else {
                    if (isExport) "Next: generate phrase" else "Next: enter phrase & pick file"
                }
            )
        }
    }
}
