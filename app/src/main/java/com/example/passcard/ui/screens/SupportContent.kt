package com.example.passcard.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.ui.theme.Primary
import com.example.passcard.ui.theme.ThemeColors
import com.example.passcard.ui.theme.rememberThemeColors

@Composable
fun HelpContent(currentLanguage: AppLanguage, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val themeColors = rememberThemeColors()
    val title = if (currentLanguage == AppLanguage.CHINESE) "使用帮助" else "Help"
    val helpItems = if (currentLanguage == AppLanguage.CHINESE) listOf(
        Triple("添加密码", "点击底部导航栏的 + 按钮添加新密码。", Icons.Outlined.Add),
        Triple("查看密码", "在密码列表中点击任意条目查看详情。", Icons.Outlined.Visibility),
        Triple("复制密码", "双击密码列表中的任意条目即可复制密码。", Icons.Outlined.ContentCopy),
        Triple("编辑密码", "点击密码详情页的编辑按钮修改信息。", Icons.Outlined.Edit),
        Triple("删除密码", "在编辑页面点击删除按钮移除密码。", Icons.Outlined.Delete),
        Triple("搜索密码", "在首页或全部密码页面使用搜索框。", Icons.Outlined.Search),
        Triple("分类管理", "为密码添加分类标签便于管理。", Icons.Outlined.Category),
        Triple("主题切换", "在设置中选择浅色、深色或跟随系统。", Icons.Outlined.DarkMode)
    ) else listOf(
        Triple("Add Password", "Tap + button to add a new password.", Icons.Outlined.Add),
        Triple("View Password", "Tap any item in the list to view details.", Icons.Outlined.Visibility),
        Triple("Copy Password", "Double-tap any item to copy the password.", Icons.Outlined.ContentCopy),
        Triple("Edit Password", "Tap edit button to modify information.", Icons.Outlined.Edit),
        Triple("Delete Password", "Tap delete button on the edit page.", Icons.Outlined.Delete),
        Triple("Search", "Use the search bar on home or all passwords page.", Icons.Outlined.Search),
        Triple("Categories", "Add category tags to organize passwords.", Icons.Outlined.Category),
        Triple("Theme", "Choose light, dark, or system theme in settings.", Icons.Outlined.DarkMode)
    )

    SupportScaffold(title = title, onBack = onBack, themeColors = themeColors, modifier = modifier) {
        helpItems.forEach { (itemTitle, itemDesc, itemIcon) ->
            HelpItem(icon = itemIcon, title = itemTitle, description = itemDesc, themeColors = themeColors)
        }
    }
}

@Composable
fun PrivacyContent(currentLanguage: AppLanguage, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE
    val sections = if (isZh) listOf(
        "数据收集" to "我们收集的最少信息仅用于应用功能。我们不会收集您的密码或敏感个人信息。",
        "本地存储" to "密码数据保存在您的设备本地。启用云同步时，云端只保存加密后的备份文件。",
        "加密保护" to "本地数据库和云端备份均使用加密保护，助记词不会上传到云端。",
        "第三方访问" to "我们不会与任何第三方分享您的个人信息。",
        "数据权利" to "您可以随时删除本地数据，也可以手动清理云端备份。",
        "政策更新" to "我们可能会不时更新此隐私政策。任何更改都将在应用内通知。"
    ) else listOf(
        "Data Collection" to "We collect minimal information only for app functionality. We do not collect your passwords or sensitive personal data.",
        "Local Storage" to "Password data is stored locally on your device. When cloud sync is enabled, the cloud only stores encrypted backup files.",
        "Encryption" to "The local database and cloud backups are encrypted. Recovery phrases are never uploaded.",
        "Third Party Access" to "We do not share your personal information with any third parties.",
        "Data Rights" to "You can delete local data at any time and manually remove cloud backups.",
        "Policy Updates" to "We may update this privacy policy from time to time. Any changes will be notified within the app."
    )

    SupportScaffold(
        title = if (isZh) "隐私条款" else "Privacy Policy",
        onBack = onBack,
        themeColors = themeColors,
        modifier = modifier
    ) {
        sections.forEach { (sectionTitle, content) ->
            PrivacySection(title = sectionTitle, content = content, themeColors = themeColors)
        }
    }
}

@Composable
fun AboutContent(currentLanguage: AppLanguage, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE
    val features = if (isZh) listOf(
        "• SQLCipher 本地数据库加密",
        "• 生物识别解锁",
        "• 加密 Vault 备份",
        "• CSV / JSON 导入导出",
        "• 分类与搜索管理"
    ) else listOf(
        "• SQLCipher local database encryption",
        "• Biometric unlock",
        "• Encrypted vault backup",
        "• CSV / JSON import and export",
        "• Category and search management"
    )

    SupportScaffold(
        title = if (isZh) "关于" else "About",
        onBack = onBack,
        themeColors = themeColors,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(Primary), contentAlignment = Alignment.Center) {
                Text(text = "P", style = MaterialTheme.typography.displayLarge, color = androidx.compose.ui.graphics.Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "REPassCard", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = themeColors.onBackground)
            Text(text = "v0.72", style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
        }
        Text(
            text = if (isZh) {
                "REPassCard 是一款专注本地安全和可控备份的密码管理应用。"
            } else {
                "REPassCard is a password manager focused on local security and controlled backups."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.onSurfaceVariant
        )
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(themeColors.surface).padding(20.dp)) {
            Text(
                text = if (isZh) "功能特点" else "Features",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                color = themeColors.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            features.forEach { feature ->
                Text(text = feature, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        Text(
            text = "© 2026 REPassCard. " + if (isZh) "保留所有权利。" else "All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.onSurfaceVariant
        )
    }
}

@Composable
private fun SupportScaffold(
    title: String,
    onBack: () -> Unit,
    themeColors: ThemeColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = themeColors.onBackground,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = themeColors.onBackground)
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun HelpItem(icon: ImageVector, title: String, description: String, themeColors: ThemeColors) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(themeColors.surface).padding(16.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = title, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600), color = themeColors.onBackground)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
        }
    }
}

@Composable
private fun PrivacySection(title: String, content: String, themeColors: ThemeColors) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(themeColors.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600), color = themeColors.onBackground)
        Text(text = content, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
    }
}
