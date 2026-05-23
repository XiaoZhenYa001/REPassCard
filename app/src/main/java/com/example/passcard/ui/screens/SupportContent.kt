package com.example.passcard.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passcard.R
import com.example.passcard.ui.theme.Primary
import com.example.passcard.ui.theme.ThemeColors
import com.example.passcard.ui.theme.rememberThemeColors

@Composable
fun HelpContent(
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    onNavigateToCloudBackupHelp: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
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

    SupportScaffold(title = title, onBack = onBack, themeColors = themeColors, modifier = modifier, scrollState = scrollState) {
        HelpItem(
            icon = Icons.Outlined.CloudDone,
            title = if (currentLanguage == AppLanguage.CHINESE) "云同步与加密备份" else "Cloud Sync & Encrypted Backup",
            description = if (currentLanguage == AppLanguage.CHINESE) {
                "了解真实云端配置、恢复助记词和同步前后的注意事项。"
            } else {
                "Learn real cloud setup, recovery phrases, and sync safety notes."
            },
            themeColors = themeColors,
            onClick = onNavigateToCloudBackupHelp
        )
        helpItems.forEach { (itemTitle, itemDesc, itemIcon) ->
            HelpItem(icon = itemIcon, title = itemTitle, description = itemDesc, themeColors = themeColors)
        }
    }
}

@Composable
fun CloudBackupHelpContent(
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE

    SupportScaffold(
        title = if (isZh) "云同步与加密备份" else "Cloud Sync & Encrypted Backup",
        onBack = onBack,
        themeColors = themeColors,
        modifier = modifier
    ) {
        CloudHelpDiagram(currentLanguage = currentLanguage, themeColors = themeColors)
        PrivacySection(
            title = if (isZh) "需要先完成云端配置" else "Cloud configuration is required",
            content = if (isZh) {
                "云同步只连接你填写的真实 S3 兼容对象存储。Endpoint、Bucket、Access Key 和 Secret Key 未填完整时，同步按钮不会执行上传或下载。"
            } else {
                "Cloud sync only connects to the real S3-compatible storage you configure. Upload and download stay disabled until Endpoint, Bucket, Access Key, and Secret Key are complete."
            },
            themeColors = themeColors
        )
        PrivacySection(
            title = if (isZh) "恢复助记词负责加密" else "Recovery phrase encrypts the backup",
            content = if (isZh) {
                "上传前会在本机用恢复助记词派生密钥并加密密码库，云端只保存密文。下载恢复时必须输入当时加密所用的助记词。"
            } else {
                "Before upload, the vault is encrypted locally with a key derived from the recovery phrase. The cloud stores ciphertext only. Restore requires the phrase used for that backup."
            },
            themeColors = themeColors
        )
        PrivacySection(
            title = if (isZh) "建议使用 STS 临时密钥" else "Prefer STS temporary credentials",
            content = if (isZh) {
                "如果云服务支持，优先使用权限受限、可过期的 STS 临时密钥。长期 SecretKey 只建议用于个人受限子账号，并且要避免赋予删除整个 Bucket 的权限。"
            } else {
                "Use restricted, expiring STS credentials when your provider supports them. Permanent SecretKeys should be limited to personal restricted accounts and should not grant broad bucket deletion permissions."
            },
            themeColors = themeColors
        )
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
            Image(
                painter = painterResource(id = R.drawable.ic_passcard_logo),
                contentDescription = if (isZh) "PassCard 图标" else "PassCard icon",
                modifier = Modifier.size(88.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "PassCard", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = themeColors.onBackground)
            Text(text = "v0.80", style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
        }
        Text(
            text = if (isZh) {
                "PassCard 是一款专注本地安全、真实云端备份和可控恢复的密码管理应用。"
            } else {
                "PassCard is a password manager focused on local security, real cloud backups, and controlled recovery."
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
            text = "© 2026 XiaoZhen. " + if (isZh) "保留所有权利。" else "All rights reserved.",
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
    scrollState: ScrollState = rememberScrollState(),
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
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 24.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun HelpItem(
    icon: ImageVector,
    title: String,
    description: String,
    themeColors: ThemeColors,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(themeColors.surface)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = title, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600), color = themeColors.onBackground)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
        }
        if (onClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = themeColors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CloudHelpDiagram(currentLanguage: AppLanguage, themeColors: ThemeColors) {
    val isZh = currentLanguage == AppLanguage.CHINESE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(themeColors.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isZh) "同步流程" else "Sync flow",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
            color = themeColors.onBackground
        )
        CloudHelpStep(
            icon = Icons.Outlined.Lock,
            title = if (isZh) "1. 本机加密" else "1. Encrypt locally",
            description = if (isZh) "恢复助记词只在本机参与加密。" else "The recovery phrase is used only on device.",
            themeColors = themeColors
        )
        CloudHelpStep(
            icon = Icons.Outlined.CloudUpload,
            title = if (isZh) "2. 上传密文" else "2. Upload ciphertext",
            description = if (isZh) "云端对象存储保存的是加密后的备份文件。" else "Object storage receives the encrypted backup file.",
            themeColors = themeColors
        )
        CloudHelpStep(
            icon = Icons.Outlined.Security,
            title = if (isZh) "3. 恢复时校验" else "3. Verify on restore",
            description = if (isZh) "只有正确助记词才能解密恢复。" else "Only the correct phrase can decrypt the backup.",
            themeColors = themeColors
        )
    }
}

@Composable
private fun CloudHelpStep(
    icon: ImageVector,
    title: String,
    description: String,
    themeColors: ThemeColors
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600), color = themeColors.onBackground)
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
