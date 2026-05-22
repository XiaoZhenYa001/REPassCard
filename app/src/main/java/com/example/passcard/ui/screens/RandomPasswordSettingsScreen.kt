package com.example.passcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.util.PreferencesManager
import com.example.passcard.util.RandomPasswordGenerator
import com.example.passcard.util.RandomPasswordSpec

@Composable
fun RandomPasswordSettingsScreen(
    preferencesManager: PreferencesManager?,
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    var spec by remember(preferencesManager) {
        mutableStateOf(preferencesManager?.randomPasswordSpec ?: RandomPasswordSpec())
    }
    var preview by remember(spec) { mutableStateOf(RandomPasswordGenerator.generate(spec)) }
    val strength = remember(spec) { RandomPasswordGenerator.strength(spec) }

    fun updateSpec(next: RandomPasswordSpec) {
        spec = next.normalized()
        preferencesManager?.randomPasswordSpec = spec
        preview = RandomPasswordGenerator.generate(spec)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = themeColors.onBackground,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (isZh) "随机密码" else "Random Password",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = themeColors.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PreviewCard(
                preview = preview,
                strengthScore = strength.score,
                strengthLabel = if (isZh) strength.labelZh else strength.labelEn,
                strengthHint = if (isZh) strength.hintZh else strength.hintEn,
                onRefresh = { preview = RandomPasswordGenerator.generate(spec) }
            )

            SettingBlock {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isZh) "密码长度" else "Password Length",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = themeColors.onBackground
                    )
                    Text(
                        text = spec.length.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = themeColors.primary
                    )
                }
                Slider(
                    value = spec.length.toFloat(),
                    onValueChange = { updateSpec(spec.copy(length = it.toInt())) },
                    valueRange = RandomPasswordSpec.MIN_LENGTH.toFloat()..RandomPasswordSpec.MAX_LENGTH.toFloat(),
                    steps = RandomPasswordSpec.MAX_LENGTH - RandomPasswordSpec.MIN_LENGTH - 1
                )
                Text(
                    text = if (isZh) "可选范围：4 - 18 位" else "Range: 4 - 18 characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant
                )
            }

            SettingBlock {
                PasswordOptionRow(
                    label = if (isZh) "大写字母 A-Z" else "Uppercase A-Z",
                    checked = spec.includeUppercase,
                    onCheckedChange = { updateSpec(spec.copy(includeUppercase = it)) }
                )
                PasswordOptionRow(
                    label = if (isZh) "小写字母 a-z" else "Lowercase a-z",
                    checked = spec.includeLowercase,
                    onCheckedChange = { updateSpec(spec.copy(includeLowercase = it)) }
                )
                PasswordOptionRow(
                    label = if (isZh) "数字 0-9" else "Numbers 0-9",
                    checked = spec.includeNumbers,
                    onCheckedChange = { updateSpec(spec.copy(includeNumbers = it)) }
                )
                PasswordOptionRow(
                    label = if (isZh) "符号 !@#" else "Symbols !@#",
                    checked = spec.includeSymbols,
                    onCheckedChange = { updateSpec(spec.copy(includeSymbols = it)) }
                )
            }

            Button(
                onClick = { preview = RandomPasswordGenerator.generate(spec) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isZh) "重新生成预览" else "Regenerate Preview")
            }
        }
    }
}

@Composable
private fun PreviewCard(
    preview: String,
    strengthScore: Int,
    strengthLabel: String,
    strengthHint: String,
    onRefresh: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(themeColors.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(themeColors.iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = themeColors.onBackground)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = preview,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = themeColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "Refresh",
                tint = themeColors.primary,
                modifier = Modifier.size(24.dp).clickable { onRefresh() }
            )
        }
        LinearProgressIndicator(
            progress = { strengthScore / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
        )
        Text(
            text = "$strengthLabel · $strengthScore%",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = themeColors.onBackground
        )
        Text(
            text = strengthHint,
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingBlock(content: @Composable ColumnScope.() -> Unit) {
    val themeColors = LocalThemeColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(themeColors.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun PasswordOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val themeColors = LocalThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.onBackground,
            modifier = Modifier.weight(1f)
        )
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}
