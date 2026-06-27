package com.example.passcard.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.passcard.R
import com.example.passcard.ui.components.PressableScale
import com.example.passcard.ui.theme.BackButtonSize
import com.example.passcard.ui.theme.IconBgBlue
import com.example.passcard.ui.theme.IconBgCyan
import com.example.passcard.ui.theme.IconBgGray
import com.example.passcard.ui.theme.IconBgGreen
import com.example.passcard.ui.theme.IconBgOrange
import com.example.passcard.ui.theme.IconBgPurple
import com.example.passcard.ui.theme.IconBgRed
import com.example.passcard.ui.theme.Radius10
import com.example.passcard.ui.theme.Radius12
import com.example.passcard.ui.theme.Radius16
import com.example.passcard.ui.theme.Radius18
import com.example.passcard.ui.theme.Radius24
import com.example.passcard.ui.theme.Spacing4
import com.example.passcard.ui.theme.Spacing8
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing14
import com.example.passcard.ui.theme.Spacing16
import com.example.passcard.ui.theme.Spacing20
import com.example.passcard.ui.theme.Spacing24
import com.example.passcard.ui.theme.ThemeColors
import com.example.passcard.ui.theme.appleSurface
import com.example.passcard.ui.theme.rememberThemeColors

@Composable
fun HelpContent(
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    onNavigateToSearchHelp: () -> Unit,
    onNavigateToCloudBackupHelp: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE
    val helpItems = if (isZh) {
        listOf(
            HelpEntry("添加密码", "点击底部导航栏的 + 按钮添加新的密码记录。", Icons.Outlined.Add, IconBgPurple),
            HelpEntry("查看密码", "在密码列表中点击任意条目，查看或进入编辑详情。", Icons.Outlined.Visibility, IconBgBlue),
            HelpEntry("复制密码", "在条目详情或快捷操作中复制需要的信息。", Icons.Outlined.ContentCopy, IconBgGreen),
            HelpEntry("编辑密码", "进入条目后修改名称、账号、密码、分类和备注。", Icons.Outlined.Edit, IconBgOrange),
            HelpEntry("删除密码", "在编辑页面使用删除操作移除不再需要的记录。", Icons.Outlined.Delete, IconBgRed),
            HelpEntry("分类管理", "为密码添加分类，便于筛选、搜索和整理。", Icons.Outlined.Category, IconBgCyan),
            HelpEntry("主题切换", "在设置中选择浅色、深色或跟随系统。", Icons.Outlined.DarkMode, IconBgGray)
        )
    } else {
        listOf(
            HelpEntry("Add Password", "Tap the + button in the bottom bar to add a new record.", Icons.Outlined.Add, IconBgPurple),
            HelpEntry("View Password", "Tap any password item to view or edit details.", Icons.Outlined.Visibility, IconBgBlue),
            HelpEntry("Copy Password", "Copy needed fields from details or quick actions.", Icons.Outlined.ContentCopy, IconBgGreen),
            HelpEntry("Edit Password", "Open an item to edit name, account, password, category, and notes.", Icons.Outlined.Edit, IconBgOrange),
            HelpEntry("Delete Password", "Use delete on the edit page to remove a record.", Icons.Outlined.Delete, IconBgRed),
            HelpEntry("Categories", "Add categories to make filtering, search, and organization easier.", Icons.Outlined.Category, IconBgCyan),
            HelpEntry("Theme", "Choose light, dark, or system theme in settings.", Icons.Outlined.DarkMode, IconBgGray)
        )
    }

    SupportScaffold(
        title = if (isZh) "使用帮助" else "Help",
        onBack = onBack,
        themeColors = themeColors,
        modifier = modifier,
        scrollState = scrollState
    ) {
        HelpItem(
            entry = HelpEntry(
                title = if (isZh) "云同步与加密备份" else "Cloud Sync & Encrypted Backup",
                description = if (isZh) "了解真实云端配置、恢复助记词和同步前后的注意事项。" else "Learn real cloud setup, recovery phrases, and sync safety notes.",
                icon = Icons.Outlined.CloudDone,
                iconColor = IconBgBlue
            ),
            themeColors = themeColors,
            onClick = onNavigateToCloudBackupHelp
        )
        HelpItem(
            entry = HelpEntry(
                title = if (isZh) "搜索密码" else "Search Passwords",
                description = if (isZh) "了解搜索结果优先级和 /t 字段检索语法。" else "Learn result priority and /t field search syntax.",
                icon = Icons.Outlined.Search,
                iconColor = IconBgPurple
            ),
            themeColors = themeColors,
            onClick = onNavigateToSearchHelp
        )
        helpItems.forEach { entry ->
            HelpItem(entry = entry, themeColors = themeColors)
        }
    }
}

@Composable
fun SearchHelpContent(
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE

    SupportScaffold(
        title = if (isZh) "搜索密码" else "Search Passwords",
        onBack = onBack,
        themeColors = themeColors,
        modifier = modifier
    ) {
        PrivacySection(
            title = if (isZh) "普通搜索排序" else "Default Result Order",
            content = if (isZh) {
                "输入关键词后，结果会按字段优先级排序：名称、用户名、手机号、邮箱、密码、分类、备注。同一字段内优先显示完全匹配，其次是开头匹配，最后是包含匹配。"
            } else {
                "Type a keyword to rank results by field priority: name, username, phone, email, password, category, then note. Within a field, exact matches appear first, then prefix matches, then contains matches."
            },
            themeColors = themeColors
        )
        PrivacySection(
            title = if (isZh) "字段检索" else "Field Search",
            content = if (isZh) {
                "输入 /t 字段 关键词 可以只搜索指定字段。例如：/t 名称 微信、/t 用户名 alex、/t 手机 138、/t 邮箱 gmail、/t 分类 工作、/t 备注 备用。"
            } else {
                "Use /t field keyword to search one field only. Examples: /t name wechat, /t username alex, /t phone 138, /t email gmail, /t category work, /t note backup."
            },
            themeColors = themeColors
        )
        PrivacySection(
            title = if (isZh) "隐私提醒" else "Privacy Note",
            content = if (isZh) {
                "支持按密码字段检索，但列表不会因为搜索结果额外明文展示密码内容；需要查看或修改时再进入条目详情。"
            } else {
                "Password-field search is supported, but the list does not reveal matching password text. Open the item when you need details or editing."
            },
            themeColors = themeColors
        )
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
            title = if (isZh) "需要先完成云端配置" else "Cloud Configuration Is Required",
            content = if (isZh) {
                "云同步只连接你填写的真实 S3 兼容对象存储。Endpoint、Bucket、Access Key 和 Secret Key 未填写完整时，同步按钮不会执行上传或下载。"
            } else {
                "Cloud sync only connects to the real S3-compatible storage you configure. Upload and download stay disabled until Endpoint, Bucket, Access Key, and Secret Key are complete."
            },
            themeColors = themeColors
        )
        PrivacySection(
            title = if (isZh) "恢复助记词负责加密" else "Recovery Phrase Encrypts The Backup",
            content = if (isZh) {
                "上传前会在本机用恢复助记词派生密钥并加密密码库，云端只保存密文。下载恢复时必须输入当时加密所用的助记词。"
            } else {
                "Before upload, the vault is encrypted locally with a key derived from the recovery phrase. The cloud stores ciphertext only. Restore requires the phrase used for that backup."
            },
            themeColors = themeColors
        )
        PrivacySection(
            title = if (isZh) "建议使用 STS 临时密钥" else "Prefer STS Temporary Credentials",
            content = if (isZh) {
                "如果云服务支持，优先使用权限受限、可过期的 STS 临时密钥。长期 SecretKey 建议只用于个人受限子账号，并避免赋予删除整个 Bucket 的权限。"
            } else {
                "Use restricted, expiring STS credentials when your provider supports them. Permanent SecretKeys should be limited to personal restricted accounts and should not grant broad bucket deletion permissions."
            },
            themeColors = themeColors
        )
    }
}

@Composable
fun PrivacyContent(
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE
    val sections = if (isZh) {
        listOf(
            "数据收集" to "我们只在应用功能需要时保存必要信息，不会收集你的密码或敏感个人信息。",
            "本地存储" to "密码数据保存在你的设备本地。启用云同步时，云端只保存加密后的备份文件。",
            "加密保护" to "本地数据库和云端备份均使用加密保护，恢复助记词不会上传到云端。",
            "第三方访问" to "我们不会与任何第三方分享你的个人信息。",
            "数据权利" to "你可以随时删除本地数据，也可以手动清理云端备份。",
            "政策更新" to "我们可能会不时更新隐私条款，重要变更会在应用内提示。"
        )
    } else {
        listOf(
            "Data Collection" to "We store only the minimum information needed for app functionality. We do not collect your passwords or sensitive personal data.",
            "Local Storage" to "Password data is stored locally on your device. When cloud sync is enabled, the cloud only stores encrypted backup files.",
            "Encryption" to "The local database and cloud backups are encrypted. Recovery phrases are never uploaded.",
            "Third Party Access" to "We do not share your personal information with any third parties.",
            "Data Rights" to "You can delete local data at any time and manually remove cloud backups.",
            "Policy Updates" to "We may update this policy from time to time. Important changes will be shown inside the app."
        )
    }

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
fun AboutContent(
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberThemeColors()
    val isZh = currentLanguage == AppLanguage.CHINESE
    val features = if (isZh) {
        listOf(
            "SQLCipher 本地数据库加密",
            "生物识别解锁",
            "加密 Vault 备份",
            "CSV / JSON 导入导出",
            "分类、搜索与随机密码工具"
        )
    } else {
        listOf(
            "SQLCipher local database encryption",
            "Biometric unlock",
            "Encrypted vault backup",
            "CSV / JSON import and export",
            "Categories, search, and random password tools"
        )
    }

    SupportScaffold(
        title = if (isZh) "关于" else "About",
        onBack = onBack,
        themeColors = themeColors,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appleSurface(colors = themeColors, radius = Radius24)
                .padding(Spacing24),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(Radius24))
                    .background(themeColors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.passcard_launcher_foreground),
                    contentDescription = if (isZh) "PassCard 图标" else "PassCard icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing8)
                )
            }
            Spacer(modifier = Modifier.height(Spacing16))
            Text(
                text = "PassCard",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.W800),
                color = themeColors.onBackground
            )
            Text(
                text = "v0.81",
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.onSurfaceVariant
            )
        }
        PrivacySection(
            title = if (isZh) "应用定位" else "Positioning",
            content = if (isZh) {
                "PassCard 是一款专注本地安全、真实云端备份和可控恢复的密码管理应用。"
            } else {
                "PassCard is a password manager focused on local security, real cloud backups, and controlled recovery."
            },
            themeColors = themeColors
        )
        SupportCard(themeColors = themeColors) {
            Text(
                text = if (isZh) "功能特点" else "Features",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
                color = themeColors.onBackground
            )
            Spacer(modifier = Modifier.height(Spacing12))
            features.forEach { feature ->
                Text(
                    text = "• $feature",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing4))
            }
        }
        Text(
            text = "© 2026 XiaoZhen. " + if (isZh) "保留所有权利。" else "All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = Spacing20),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressableScale(onClick = onBack) {
                Box(
                    modifier = Modifier
                        .size(BackButtonSize)
                        .clip(RoundedCornerShape(Radius12))
                        .background(themeColors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = themeColors.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing14))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = themeColors.onBackground
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = Spacing20)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing16)
        ) {
            Spacer(modifier = Modifier.height(Spacing4))
            content()
        }
    }
}

@Composable
private fun HelpItem(
    entry: HelpEntry,
    themeColors: ThemeColors,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        PressableScale(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            HelpItemRow(entry = entry, themeColors = themeColors, showChevron = true)
        }
    } else {
        HelpItemRow(entry = entry, themeColors = themeColors, showChevron = false)
    }
}

@Composable
private fun HelpItemRow(
    entry: HelpEntry,
    themeColors: ThemeColors,
    showChevron: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleSurface(colors = themeColors, radius = Radius18)
            .padding(Spacing16),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing12)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius10))
                .background(entry.iconColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = entry.title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
                color = themeColors.onBackground
            )
            Spacer(modifier = Modifier.height(Spacing4))
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = themeColors.muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CloudHelpDiagram(currentLanguage: AppLanguage, themeColors: ThemeColors) {
    val isZh = currentLanguage == AppLanguage.CHINESE
    SupportCard(themeColors = themeColors) {
        Text(
            text = if (isZh) "同步流程" else "Sync Flow",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
            color = themeColors.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing12))
        CloudHelpStep(
            icon = Icons.Outlined.Lock,
            title = if (isZh) "1. 本机加密" else "1. Encrypt Locally",
            description = if (isZh) "恢复助记词只在本机参与加密。" else "The recovery phrase is used only on device.",
            iconColor = IconBgPurple,
            themeColors = themeColors
        )
        CloudHelpStep(
            icon = Icons.Outlined.CloudUpload,
            title = if (isZh) "2. 上传密文" else "2. Upload Ciphertext",
            description = if (isZh) "云端对象存储保存的是加密后的备份文件。" else "Object storage receives the encrypted backup file.",
            iconColor = IconBgBlue,
            themeColors = themeColors
        )
        CloudHelpStep(
            icon = Icons.Outlined.Security,
            title = if (isZh) "3. 恢复时校验" else "3. Verify On Restore",
            description = if (isZh) "只有正确助记词才能解密恢复。" else "Only the correct phrase can decrypt the backup.",
            iconColor = IconBgGreen,
            themeColors = themeColors
        )
    }
}

@Composable
private fun CloudHelpStep(
    icon: ImageVector,
    title: String,
    description: String,
    iconColor: Color,
    themeColors: ThemeColors
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing12),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing8)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(Radius10))
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W700),
                color = themeColors.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    content: String,
    themeColors: ThemeColors
) {
    SupportCard(themeColors = themeColors) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W700),
            color = themeColors.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing8))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.onSurfaceVariant
        )
    }
}

@Composable
private fun SupportCard(
    themeColors: ThemeColors,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .appleSurface(colors = themeColors, radius = Radius18)
            .padding(Spacing16),
        content = content
    )
}

private data class HelpEntry(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color
)
