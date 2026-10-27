package com.example.passcard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.passcard.util.ClipboardHelper
import com.example.passcard.ui.components.PressableScale
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.ui.theme.MotionDuration
import com.example.passcard.ui.theme.MotionEasing
import com.example.passcard.ui.theme.Radius10
import com.example.passcard.ui.theme.Radius12
import com.example.passcard.ui.theme.Radius18
import com.example.passcard.ui.theme.Radius24
import com.example.passcard.ui.theme.Spacing4
import com.example.passcard.ui.theme.Spacing8
import com.example.passcard.ui.theme.Spacing10
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing16
import com.example.passcard.ui.theme.Spacing20
import com.example.passcard.ui.theme.appleSurface
import com.example.passcard.util.PreferencesManager
import com.example.passcard.util.PasswordCharacterGroup
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
    val context = LocalContext.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    var spec by remember(preferencesManager) {
        mutableStateOf(preferencesManager?.randomPasswordSpec ?: RandomPasswordSpec())
    }
    var preview by remember(preferencesManager) { mutableStateOf(RandomPasswordGenerator.generate(spec)) }
    var expandedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    val strength = remember(spec) { RandomPasswordGenerator.strength(spec) }

    fun updateSpec(next: RandomPasswordSpec) {
        if (!next.hasSelectedCharacters) {
            Toast.makeText(
                context,
                if (isZh) "至少保留一个可用字符" else "Keep at least one available character",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val normalized = next.normalized()
        if (normalized == spec) return
        spec = normalized
        preferencesManager?.randomPasswordSpec = spec
        preview = RandomPasswordGenerator.generate(spec)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .padding(horizontal = Spacing20, vertical = Spacing4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressableScale(onClick = onBack) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(Radius12))
                        .background(themeColors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = if (isZh) "返回" else "Back",
                        tint = themeColors.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing16))
            Text(
                text = if (isZh) "随机密码" else "Random Password",
                style = MaterialTheme.typography.titleLarge,
                color = themeColors.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing20, vertical = Spacing16),
            verticalArrangement = Arrangement.spacedBy(Spacing16)
        ) {
            PreviewCard(
                preview = preview,
                strengthScore = strength.score,
                strengthLabel = if (isZh) strength.labelZh else strength.labelEn,
                strengthHint = if (isZh) strength.hintZh else strength.hintEn,
                isZh = isZh,
                onCopy = {
                    ClipboardHelper.copyToClipboard(context, preview, label = "Random Password", showToast = false)
                    Toast.makeText(
                        context,
                        if (isZh) "已复制随机密码" else "Random password copied",
                        Toast.LENGTH_SHORT
                    ).show()
                },
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
                        color = themeColors.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = spec.length.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = themeColors.primary
                    )
                }
                Slider(
                    modifier = Modifier.semantics {
                        contentDescription = if (isZh) "密码长度" else "Password length"
                        stateDescription = if (isZh) "${spec.length} 位" else "${spec.length} characters"
                    },
                    value = spec.length.toFloat(),
                    onValueChange = { updateSpec(spec.copy(length = it.toInt())) },
                    valueRange = RandomPasswordSpec.MIN_LENGTH.toFloat()..RandomPasswordSpec.MAX_LENGTH.toFloat(),
                    steps = RandomPasswordSpec.MAX_LENGTH - RandomPasswordSpec.MIN_LENGTH - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = themeColors.primary,
                        activeTrackColor = themeColors.primary,
                        inactiveTrackColor = themeColors.primaryLight
                    )
                )
                Text(
                    text = if (isZh) "可选范围：4 - 18 位" else "Range: 4 - 18 characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant
                )
            }

            SettingBlock {
                Text(
                    text = if (isZh) "可用字符" else "Allowed characters",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = themeColors.onBackground
                )
                Text(
                    text = if (isZh) {
                        "点开分类，点亮允许使用的字符。自动保存，并用于添加和编辑密码。"
                    } else {
                        "Expand a group and light up the characters to use. Saves automatically for new and edited passwords."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant
                )
                PasswordCharacterGroup.entries.forEach { group ->
                    CharacterGroupPicker(
                        group = group,
                        selectedCharacters = spec.selectedCharacters(group),
                        expanded = expandedGroup == group.name,
                        isZh = isZh,
                        onExpand = { expandedGroup = if (expandedGroup == group.name) null else group.name },
                        onSelectionChange = { updateSpec(spec.withSelectedCharacters(group, it)) }
                    )
                }
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
    isZh: Boolean,
    onCopy: () -> Unit,
    onRefresh: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appleSurface(colors = themeColors, radius = Radius24)
            .padding(Spacing20),
        verticalArrangement = Arrangement.spacedBy(Spacing12)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isZh) "密码预览" else "Password preview",
                style = MaterialTheme.typography.labelMedium,
                color = themeColors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            PressableScale(onClick = onCopy) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(Radius10))
                        .background(themeColors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = if (isZh) "复制密码" else "Copy password",
                        tint = themeColors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing8))
            PressableScale(onClick = onRefresh) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(Radius10))
                        .background(themeColors.primaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = if (isZh) "重新生成密码" else "Generate another password",
                        tint = themeColors.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
        Text(
            text = preview,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            color = themeColors.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        LinearProgressIndicator(
            progress = { strengthScore / 100f },
            color = themeColors.primary,
            trackColor = themeColors.primaryLight,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
        )
        Text(
            text = "${if (isZh) "生成配置参考" else "Generation reference"}: $strengthLabel · $strengthScore / 100",
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
            .appleSurface(colors = themeColors, radius = Radius18)
            .padding(Spacing16),
        verticalArrangement = Arrangement.spacedBy(Spacing10),
        content = content
    )
}

private fun PasswordCharacterGroup.label(isZh: Boolean): String = when (this) {
    PasswordCharacterGroup.Uppercase -> if (isZh) "大写字母" else "Uppercase"
    PasswordCharacterGroup.Lowercase -> if (isZh) "小写字母" else "Lowercase"
    PasswordCharacterGroup.Numbers -> if (isZh) "数字" else "Numbers"
    PasswordCharacterGroup.Symbols -> if (isZh) "符号" else "Symbols"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterGroupPicker(
    group: PasswordCharacterGroup,
    selectedCharacters: String,
    expanded: Boolean,
    isZh: Boolean,
    onExpand: () -> Unit,
    onSelectionChange: (String) -> Unit
) {
    val themeColors = LocalThemeColors.current
    val touchInput = LocalInputModeManager.current.inputMode == InputMode.Touch
    val duration = if (touchInput) MotionDuration.Emphasized else 0
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(duration, easing = MotionEasing.EnterExit),
        label = "character_group_chevron"
    )
    val label = group.label(isZh)
    Column(modifier = Modifier.fillMaxWidth()) {
        PressableScale(
            onClick = onExpand,
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = if (isZh) {
                    "${if (expanded) "已展开" else "已收起"}，已选 ${selectedCharacters.length} 个，共 ${group.availableCharacters.length} 个"
                } else {
                    "${if (expanded) "Expanded" else "Collapsed"}, ${selectedCharacters.length} of ${group.availableCharacters.length} selected"
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(Radius12))
                    .background(if (expanded) themeColors.primaryLight else themeColors.surfaceVariant)
                    .padding(horizontal = Spacing12, vertical = Spacing8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing8)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = themeColors.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${selectedCharacters.length} / ${group.availableCharacters.length}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selectedCharacters.isNotEmpty()) themeColors.primary else themeColors.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = themeColors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = chevron }
                )
            }
        }
        // A bounded accordion must change layout height so the following headers remain reachable.
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(duration, easing = MotionEasing.EnterExit), expandFrom = Alignment.Top) +
                fadeIn(tween(duration, easing = MotionEasing.EnterExit)),
            exit = shrinkVertically(tween(duration, easing = MotionEasing.EnterExit), shrinkTowards = Alignment.Top) +
                fadeOut(tween(if (touchInput) MotionDuration.Exit else 0, easing = MotionEasing.EnterExit))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .then(if (expanded) Modifier else Modifier.clearAndSetSemantics { })
                    .padding(top = Spacing8, bottom = Spacing4),
                verticalArrangement = Arrangement.spacedBy(Spacing8)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing4),
                    verticalArrangement = Arrangement.spacedBy(Spacing4)
                ) {
                    group.availableCharacters.forEach { character ->
                        CharacterChoice(
                            character = character,
                            selected = character in selectedCharacters,
                            enabled = expanded,
                            description = "$label $character",
                            onClick = {
                                onSelectionChange(
                                    if (character in selectedCharacters) selectedCharacters.replace(character.toString(), "")
                                    else selectedCharacters + character
                                )
                            }
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing8),
                    verticalArrangement = Arrangement.spacedBy(Spacing4)
                ) {
                    TextButton(
                        onClick = { onSelectionChange(group.availableCharacters) },
                        enabled = expanded && selectedCharacters.length < group.availableCharacters.length,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(if (isZh) "全选" else "Select all") }
                    TextButton(
                        onClick = { onSelectionChange("") },
                        enabled = expanded && selectedCharacters.isNotEmpty(),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(if (isZh) "清空" else "Clear") }
                }
            }
        }
    }
}

@Composable
private fun CharacterChoice(
    character: Char,
    selected: Boolean,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    val touchInput = LocalInputModeManager.current.inputMode == InputMode.Touch
    val background = animateColorAsState(
        targetValue = if (selected) themeColors.primaryLight else themeColors.surfaceVariant,
        animationSpec = tween(if (touchInput) MotionDuration.Fast else 0, easing = MotionEasing.EnterExit),
        label = "character_selection"
    )
    PressableScale(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.semantics(mergeDescendants = true) {
            role = Role.Checkbox
            toggleableState = if (selected) ToggleableState.On else ToggleableState.Off
            contentDescription = description
        }
    ) {
        Box(
            modifier = Modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
                .clip(RoundedCornerShape(Radius10))
                .drawBehind { drawRect(background.value) }
                .padding(horizontal = Spacing12, vertical = Spacing8),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = character.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selected) themeColors.primary else themeColors.onSurfaceVariant
            )
        }
    }
}
