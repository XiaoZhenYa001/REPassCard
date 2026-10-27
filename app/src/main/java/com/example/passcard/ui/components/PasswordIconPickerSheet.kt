package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.screens.AppLanguage
import com.example.passcard.ui.theme.IconBgBlue
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.ui.theme.Radius12
import com.example.passcard.ui.theme.Radius14
import com.example.passcard.ui.theme.Radius16
import com.example.passcard.ui.theme.Spacing6
import com.example.passcard.ui.theme.Spacing8
import com.example.passcard.ui.theme.Spacing10
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing14
import com.example.passcard.ui.theme.Spacing20
import com.example.passcard.ui.theme.Spacing24
import com.example.passcard.ui.theme.appleSurface
import com.example.passcard.util.LocalIconImage
import com.example.passcard.util.PasswordIconType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordIconPickerSheet(
    currentLanguage: AppLanguage,
    label: String,
    selectedIconType: String,
    selectedIconValue: String,
    localImages: List<LocalIconImage>,
    busyImageUri: String?,
    isImportingImage: Boolean,
    canDeleteOldImage: Boolean,
    deleteOldImage: Boolean,
    onDeleteOldImageChange: (Boolean) -> Unit,
    onSelectedIconChange: (String, String) -> Unit,
    onRefreshLocalImages: () -> Unit,
    onLocalImageClick: (LocalIconImage) -> Unit,
    onUploadClick: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    val isZh = currentLanguage == AppLanguage.CHINESE
    var selectedTab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var closing by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val imageBusy = busyImageUri != null || isImportingImage
    val canChoose = !closing && !imageBusy
    val tileWidth = (if (selectedTab == 0) 92.dp else 76.dp) * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val tabs = listOf(
        if (isZh) "默认" else "Default",
        "Emoji",
        if (isZh) "本地图片" else "Images"
    )
    val generatedOptions = remember(query) {
        generatedIconOptions.filter { option ->
            query.isBlank() || option.zhLabel.contains(query, ignoreCase = true) || option.enLabel.contains(query, ignoreCase = true)
        }
    }
    val emojiOptions = remember(query) {
        emojiIconOptions.filter { option ->
            query.isBlank() || option.emoji.contains(query) || option.zhKeywords.contains(query, ignoreCase = true) || option.enKeywords.contains(query, ignoreCase = true)
        }
    }
    val imageOptions = remember(localImages, query) {
        localImages.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }

    fun closeSheet(confirm: Boolean) {
        if (closing || (confirm && imageBusy)) return
        closing = true
        focusManager.clearFocus()
        scope.launch {
            try {
                sheetState.hide()
                if (!sheetState.isVisible) {
                    if (confirm) onConfirm() else onDismiss()
                }
            } finally {
                closing = false
            }
        }
    }

    LaunchedEffect(selectedTab) { gridState.scrollToItem(0) }

    ModalBottomSheet(
        onDismissRequest = { if (!closing) onDismiss() },
        sheetState = sheetState,
        containerColor = themeColors.background
    ) {
        // One bounded scrolling surface keeps every action reachable above the IME.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(tileWidth),
            state = gridState,
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).imePadding(),
            contentPadding = PaddingValues(start = Spacing20, end = Spacing20, bottom = Spacing24),
            horizontalArrangement = Arrangement.spacedBy(Spacing10),
            verticalArrangement = Arrangement.spacedBy(Spacing14)
        ) {
            item(key = "picker_header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                Text(
                    text = if (isZh) "更换图标" else "Change Icon",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = themeColors.onBackground
                )
            }
            item(key = "picker_search", span = { GridItemSpan(maxLineSpan) }, contentType = "search") {
                SearchBar(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = if (isZh) "搜索图标或文件名" else "Search icons or filenames",
                    clearContentDescription = if (isZh) "清空搜索" else "Clear search"
                )
            }
            item(key = "picker_tabs", span = { GridItemSpan(maxLineSpan) }, contentType = "tabs") {
                TabRow(selectedTabIndex = selectedTab, containerColor = themeColors.background) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            enabled = canChoose,
                            onClick = {
                                if (selectedTab != index) {
                                    selectedTab = index
                                    if (index == 2) onRefreshLocalImages()
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }
            }
            when (selectedTab) {
                0 -> items(generatedOptions, key = { "generated:${it.value.ifBlank { "auto" }}" }, contentType = { "icon" }) { option ->
                    IconChoiceTile(
                        selected = selectedIconType == PasswordIconType.GENERATED && selectedIconValue == option.value,
                        label = if (isZh) option.zhLabel else option.enLabel,
                        enabled = canChoose,
                        onClick = { onSelectedIconChange(PasswordIconType.GENERATED, option.value) }
                    ) {
                        GeneratedPasswordIcon(label = label, iconValue = option.value, size = 48.dp, cornerRadius = 14.dp)
                    }
                }
                1 -> items(emojiOptions, key = { "emoji:${it.emoji}" }, contentType = { "icon" }) { option ->
                    IconChoiceTile(
                        selected = selectedIconType == PasswordIconType.EMOJI && selectedIconValue == option.emoji,
                        label = option.emoji,
                        enabled = canChoose,
                        onClick = { onSelectedIconChange(PasswordIconType.EMOJI, option.emoji) }
                    ) {
                        PasswordIcon(label = option.emoji, iconType = PasswordIconType.EMOJI, iconValue = option.emoji, size = 48.dp)
                    }
                }
                else -> {
                    item(key = "picker_upload", span = { GridItemSpan(maxLineSpan) }, contentType = "upload") {
                        Button(
                            onClick = onUploadClick,
                            enabled = canChoose,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radius16),
                            colors = ButtonDefaults.buttonColors(containerColor = IconBgBlue)
                        ) {
                            if (isImportingImage) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.size(Spacing8))
                            Text(if (isZh) "上传图片" else "Upload Image")
                        }
                    }
                    items(imageOptions, key = { "image:${it.uriString}" }, contentType = { "icon" }) { image ->
                        IconChoiceTile(
                            selected = selectedIconType == PasswordIconType.IMAGE && selectedIconValue == image.uriString,
                            label = image.name,
                            enabled = canChoose,
                            onClick = { onLocalImageClick(image) },
                            busy = busyImageUri == image.uriString
                        ) {
                            PasswordIcon(label = label, iconType = PasswordIconType.IMAGE, iconValue = image.uriString, size = 52.dp)
                        }
                    }
                }
            }
            val noResults = when (selectedTab) {
                0 -> generatedOptions.isEmpty()
                1 -> emojiOptions.isEmpty()
                else -> imageOptions.isEmpty()
            }
            if (noResults) {
                item(key = "picker_empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                    Text(
                        text = if (query.isNotBlank()) {
                            if (isZh) "没有匹配的图标" else "No matching icons"
                        } else {
                            if (isZh) "暂无本地图片" else "No local images"
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing20),
                        color = themeColors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (canDeleteOldImage) {
                item(key = "picker_delete", span = { GridItemSpan(maxLineSpan) }, contentType = "delete") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleSurface(colors = themeColors, radius = Radius12)
                            .toggleable(value = deleteOldImage, enabled = !closing, role = Role.Checkbox, onValueChange = onDeleteOldImageChange)
                            .padding(end = Spacing12),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deleteOldImage,
                            onCheckedChange = null,
                            enabled = !closing,
                            colors = CheckboxDefaults.colors(checkedColor = themeColors.primary, uncheckedColor = themeColors.muted)
                        )
                        Text(
                            text = if (isZh) "保存后删除旧图标文件" else "Delete previous icon file after saving",
                            modifier = Modifier.weight(1f).padding(vertical = Spacing12),
                            style = MaterialTheme.typography.labelSmall,
                            color = themeColors.onSurfaceVariant
                        )
                    }
                }
            }
            item(key = "picker_actions", span = { GridItemSpan(maxLineSpan) }, contentType = "actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { closeSheet(confirm = false) }, enabled = !closing) {
                        Text(if (isZh) "取消" else "Cancel")
                    }
                    Button(
                        onClick = { closeSheet(confirm = true) },
                        enabled = canChoose,
                        shape = RoundedCornerShape(Radius14),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                    ) {
                        Text(if (isZh) "确定" else "Confirm")
                    }
                }
            }
        }
    }
}

@Composable
private fun IconChoiceTile(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    busy: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeColors = LocalThemeColors.current
    val bg = if (selected) themeColors.primary.copy(alpha = 0.14f) else themeColors.surface
    val border = if (selected) themeColors.primary else themeColors.border
    PressableScale(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            this.selected = selected
            role = Role.RadioButton
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appleSurface(colors = themeColors, radius = Radius16)
                .background(bg)
                .padding(Spacing8),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing6)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(border.copy(alpha = if (selected) 0.16f else 0.08f))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
                if (busy) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(Radius16))
                            .background(themeColors.background.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) themeColors.primary else themeColors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
