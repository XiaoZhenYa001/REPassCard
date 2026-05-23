package com.example.passcard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.screens.AppLanguage
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.util.LocalIconImage
import com.example.passcard.util.PasswordIconType

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tabs = listOf(
        if (isZh) "默认" else "Default",
        "Emoji",
        if (isZh) "本地图片" else "Images"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeColors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isZh) "更换图标" else "Change Icon",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = themeColors.onBackground
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(if (isZh) "搜索图标或文件名" else "Search icons or filenames") }
            )
            TabRow(selectedTabIndex = selectedTab, containerColor = themeColors.background) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (index == 2) onRefreshLocalImages()
                        },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> GeneratedIconGrid(
                    label = label,
                    query = query,
                    selectedIconValue = selectedIconValue.takeIf { selectedIconType == PasswordIconType.GENERATED }.orEmpty(),
                    currentLanguage = currentLanguage,
                    onSelected = { onSelectedIconChange(PasswordIconType.GENERATED, it) }
                )
                1 -> EmojiIconGrid(
                    query = query,
                    selectedIconValue = selectedIconValue.takeIf { selectedIconType == PasswordIconType.EMOJI }.orEmpty(),
                    onSelected = { onSelectedIconChange(PasswordIconType.EMOJI, it) }
                )
                else -> LocalImageGrid(
                    label = label,
                    query = query,
                    images = localImages,
                    selectedIconValue = selectedIconValue.takeIf { selectedIconType == PasswordIconType.IMAGE }.orEmpty(),
                    busyImageUri = busyImageUri,
                    isImportingImage = isImportingImage,
                    currentLanguage = currentLanguage,
                    onUploadClick = onUploadClick,
                    onSelected = onLocalImageClick
                )
            }

            if (canDeleteOldImage) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeColors.surface)
                        .clickable { onDeleteOldImageChange(!deleteOldImage) }
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = deleteOldImage, onCheckedChange = onDeleteOldImageChange)
                    Text(
                        text = if (isZh) "保存后删除旧图标文件" else "Delete previous icon file after saving",
                        style = MaterialTheme.typography.labelSmall,
                        color = themeColors.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(if (isZh) "取消" else "Cancel")
                }
                Button(onClick = onConfirm, enabled = busyImageUri == null && !isImportingImage) {
                    Text(if (isZh) "确定" else "Confirm")
                }
            }
        }
    }
}

@Composable
private fun GeneratedIconGrid(
    label: String,
    query: String,
    selectedIconValue: String,
    currentLanguage: AppLanguage,
    onSelected: (String) -> Unit
) {
    val isZh = currentLanguage == AppLanguage.CHINESE
    val filtered = generatedIconOptions.filter { option ->
        query.isBlank() ||
            option.zhLabel.contains(query, ignoreCase = true) ||
            option.enLabel.contains(query, ignoreCase = true)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(210.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(filtered, key = { it.value.ifBlank { "auto" } }) { option ->
            IconChoiceTile(
                selected = selectedIconValue == option.value,
                label = if (isZh) option.zhLabel else option.enLabel,
                onClick = { onSelected(option.value) }
            ) {
                GeneratedPasswordIcon(label = label, iconValue = option.value, size = 48.dp, cornerRadius = 14.dp)
            }
        }
    }
}

@Composable
private fun EmojiIconGrid(
    query: String,
    selectedIconValue: String,
    onSelected: (String) -> Unit
) {
    val filtered = emojiIconOptions.filter { option ->
        query.isBlank() ||
            option.emoji.contains(query) ||
            option.zhKeywords.contains(query, ignoreCase = true) ||
            option.enKeywords.contains(query, ignoreCase = true)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.height(250.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(filtered, key = { it.emoji }) { option ->
            IconChoiceTile(
                selected = selectedIconValue == option.emoji,
                label = option.emoji,
                onClick = { onSelected(option.emoji) }
            ) {
                PasswordIcon(label = option.emoji, iconType = PasswordIconType.EMOJI, iconValue = option.emoji, size = 48.dp)
            }
        }
    }
}

@Composable
private fun LocalImageGrid(
    label: String,
    query: String,
    images: List<LocalIconImage>,
    selectedIconValue: String,
    busyImageUri: String?,
    isImportingImage: Boolean,
    currentLanguage: AppLanguage,
    onUploadClick: () -> Unit,
    onSelected: (LocalIconImage) -> Unit
) {
    val isZh = currentLanguage == AppLanguage.CHINESE
    val filtered = images.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onUploadClick,
            enabled = !isImportingImage && busyImageUri == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isImportingImage) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(if (isZh) "上传图片" else "Upload Image")
        }
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isZh) "暂无本地图片" else "No local images",
                    color = LocalThemeColors.current.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(250.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.uriString }) { image ->
                    IconChoiceTile(
                        selected = selectedIconValue == image.uriString,
                        label = image.name,
                        onClick = { onSelected(image) },
                        busy = busyImageUri == image.uriString
                    ) {
                        PasswordIcon(label = label, iconType = PasswordIconType.IMAGE, iconValue = image.uriString, size = 52.dp)
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
    busy: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeColors = LocalThemeColors.current
    val bg = if (selected) themeColors.primary.copy(alpha = 0.14f) else themeColors.surface
    val border = if (selected) themeColors.primary else themeColors.border
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(enabled = !busy, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(border.copy(alpha = 0.08f))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
            if (busy) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
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
            color = themeColors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
