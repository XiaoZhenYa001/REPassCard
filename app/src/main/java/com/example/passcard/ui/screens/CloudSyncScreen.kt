package com.example.passcard.ui.screens

import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.passcard.data.BiometricKeyStore
import com.example.passcard.data.CloudSyncSafety
import com.example.passcard.data.CloudSyncSnapshot
import com.example.passcard.data.PasswordEntity
import com.example.passcard.data.SyncDirection
import com.example.passcard.sync.CloudSyncRepository
import com.example.passcard.sync.S3CloudStorage
import com.example.passcard.ui.theme.Primary
import com.example.passcard.ui.theme.ThemeColors
import com.example.passcard.util.AuthHelper
import com.example.passcard.util.PreferencesManager
import com.example.passcard.util.SyncSecurityMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncContent(
    currentLanguage: AppLanguage,
    themeColors: ThemeColors,
    preferencesManager: PreferencesManager?,
    passwords: List<PasswordItem>,
    loadAllPasswords: suspend () -> List<PasswordItem> = { passwords },
    replaceVaultPasswords: (List<PasswordItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val zh = currentLanguage == AppLanguage.CHINESE
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var securityMode by remember {
        mutableStateOf<SyncSecurityMode>(preferencesManager?.syncSecurityMode ?: SyncSecurityMode.MAXIMUM_SECURITY)
    }
    var objectPrefix by remember { mutableStateOf(preferencesManager?.objectPrefix ?: "repasscard/") }
    var s3Endpoint by remember { mutableStateOf(preferencesManager?.s3Endpoint ?: "") }
    var s3Region by remember { mutableStateOf(preferencesManager?.s3Region ?: "") }
    var s3Bucket by remember { mutableStateOf(preferencesManager?.s3Bucket ?: "") }
    var s3AccessKey by remember { mutableStateOf(preferencesManager?.s3AccessKey ?: "") }
    var s3SecretKey by remember { mutableStateOf(preferencesManager?.s3SecretKey ?: "") }
    var s3SessionToken by remember { mutableStateOf(preferencesManager?.s3SessionToken ?: "") }

    val useRealCloud = s3Endpoint.isNotBlank() &&
        s3Bucket.isNotBlank() &&
        s3AccessKey.isNotBlank() &&
        s3SecretKey.isNotBlank()

    val repo = remember(preferencesManager, s3Endpoint, s3Region, s3Bucket, s3AccessKey, s3SecretKey, s3SessionToken) {
        preferencesManager?.takeIf { useRealCloud }?.let { prefs ->
            val storageClient = S3CloudStorage(
                endpoint = s3Endpoint,
                region = s3Region,
                bucketName = s3Bucket,
                accessKey = s3AccessKey,
                secretKey = s3SecretKey,
                sessionToken = s3SessionToken
            )
            CloudSyncRepository(context.applicationContext, prefs, storageClient)
        }
    }

    var cloudRevision by remember { mutableStateOf<Long?>(null) }
    var cloudItemCount by remember { mutableStateOf<Int?>(null) }
    var cloudHasData by remember { mutableStateOf(false) }
    var localRevision by remember { mutableStateOf(preferencesManager?.vaultRevision ?: 0L) }
    var confirmEmptyOverwrite by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var syncPhrase by remember { mutableStateOf("") }
    var showPhraseDialog by remember { mutableStateOf(false) }
    var phraseIsUpload by remember { mutableStateOf(true) }
    var savePhraseForBiometric by remember { mutableStateOf(false) }
    var hasSavedPhrase by remember { mutableStateOf(BiometricKeyStore.hasWrappedSyncKey(context)) }
    var syncPasswords by remember { mutableStateOf(passwords) }
    val isTencentCos = s3Endpoint.contains("myqcloud.com", ignoreCase = true)
    val cosBucketLooksValid = !isTencentCos || Regex(""".+-\d{5,}$""").matches(s3Bucket.trim())

    fun refreshSnapshot(showToast: Boolean = false) {
        val r = repo ?: run {
            if (showToast) {
                val msg = if (zh) "请先填写完整云端配置" else "Complete cloud configuration first."
                statusText = msg
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            return
        }
        val pm = preferencesManager ?: return
        scope.launch(Dispatchers.IO) {
            val result = runCatching { r.fetchCloudSnapshot(syncPasswords.size) }
            withContext(Dispatchers.Main) {
                result.onSuccess { snap ->
                    cloudRevision = snap.cloudVaultRevision
                    cloudItemCount = snap.cloudItemCount
                    cloudHasData = snap.cloudHasData
                    localRevision = pm.vaultRevision
                    statusText = if (zh) "云端状态已刷新" else "Cloud status refreshed"
                    if (showToast) Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    statusText = error.message ?: if (zh) "刷新云端状态失败" else "Failed to refresh cloud status"
                    if (showToast) Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun testConnection() {
        val r = repo ?: run {
            val msg = if (zh) "请先填写完整云端配置" else "Complete cloud configuration first."
            statusText = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            busy = true
            statusText = if (zh) "正在测试连接..." else "Testing connection..."
            val result = withContext(Dispatchers.IO) { r.testConnection() }
            result.onSuccess {
                statusText = if (zh) "连接成功，可以访问对象存储。" else "Connection succeeded."
                Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                refreshSnapshot()
            }.onFailure { error ->
                statusText = error.message ?: if (zh) "连接失败" else "Connection failed"
                Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
            }
            busy = false
        }
    }

    fun applyTencentCosDefaults() {
        val nextRegion = s3Region.ifBlank { "ap-guangzhou" }
        s3Region = nextRegion
        s3Endpoint = "cos.$nextRegion.myqcloud.com"
        objectPrefix = normalizePrefixInput(objectPrefix.ifBlank { "repasscard/" })
        preferencesManager?.s3Region = s3Region
        preferencesManager?.s3Endpoint = s3Endpoint
        preferencesManager?.objectPrefix = objectPrefix
    }

    fun fillPhraseFromBiometric() {
        val activity = context as? FragmentActivity
        if (activity == null) {
            Toast.makeText(context, if (zh) "当前页面不支持生物识别验证" else "Biometric auth is not available here.", Toast.LENGTH_LONG).show()
            return
        }
        val cipher = BiometricKeyStore.getDecryptionCipher(context)
        if (cipher == null) {
            Toast.makeText(context, if (zh) "本机没有保存可用的助记词" else "No saved recovery phrase on this device.", Toast.LENGTH_LONG).show()
            return
        }
        AuthHelper.authenticateWithCipher(
            activity = activity,
            cipher = cipher,
            title = if (zh) "验证指纹以填入助记词" else "Unlock Recovery Phrase",
            subtitle = if (zh) "只会解锁曾经保存在本机的助记词" else "Only a phrase previously saved on this device can be unlocked.",
            onSuccess = { authedCipher ->
                val phrase = BiometricKeyStore.unwrapStringWithCipher(context, authedCipher)
                if (phrase.isNullOrBlank()) {
                    Toast.makeText(context, if (zh) "助记词解锁失败" else "Failed to unlock phrase.", Toast.LENGTH_LONG).show()
                } else {
                    syncPhrase = phrase
                    Toast.makeText(context, if (zh) "已通过指纹填入助记词" else "Recovery phrase filled.", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        )
    }

    fun savePhraseWithBiometric(phrase: String) {
        val activity = context as? FragmentActivity ?: return
        val cipher = BiometricKeyStore.getEncryptionCipher() ?: return
        AuthHelper.authenticateWithCipher(
            activity = activity,
            cipher = cipher,
            title = if (zh) "验证指纹以保存助记词" else "Save Recovery Phrase",
            subtitle = if (zh) "助记词会被 Android Keystore 加密后保存在本机" else "The phrase will be encrypted with Android Keystore on this device.",
            onSuccess = { authedCipher ->
                BiometricKeyStore.wrapStringWithCipher(context, authedCipher, phrase)
                hasSavedPhrase = true
                savePhraseForBiometric = false
                Toast.makeText(context, if (zh) "已保存，可下次用指纹填入" else "Saved for biometric unlock.", Toast.LENGTH_SHORT).show()
            },
            onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
        )
    }

    LaunchedEffect(preferencesManager, passwords.size, objectPrefix, useRealCloud) {
        syncPasswords = withContext(Dispatchers.IO) {
            runCatching { loadAllPasswords() }.getOrElse { passwords }
        }
        securityMode = preferencesManager?.syncSecurityMode ?: SyncSecurityMode.MAXIMUM_SECURITY
        objectPrefix = preferencesManager?.objectPrefix ?: "repasscard/"
        refreshSnapshot()
    }

    if (preferencesManager == null) {
        Text(
            text = if (zh) "偏好设置尚未初始化" else "Preferences are not ready.",
            modifier = modifier.padding(24.dp)
        )
        return
    }

    val snapshot = CloudSyncSnapshot(
        localItemCount = syncPasswords.size,
        localVaultRevision = localRevision,
        localHasData = syncPasswords.isNotEmpty(),
        cloudItemCount = cloudItemCount,
        cloudVaultRevision = cloudRevision,
        cloudHasData = cloudHasData
    )
    val decision = CloudSyncSafety.decideInitialSync(snapshot)
    val recommendedLabel = when (decision.direction) {
        SyncDirection.DOWNLOAD_CLOUD -> if (zh) "先从云端拉取" else "Download first"
        SyncDirection.UPLOAD_LOCAL -> if (zh) "上传本地保险库" else "Upload local vault"
        SyncDirection.CONFLICT -> if (zh) "可用当前本地库重新上传" else "Upload local vault if intended"
        SyncDirection.REQUIRES_CONFIRMATION -> if (zh) "需要确认" else "Confirmation required"
    }
    val decisionText = when (decision.direction) {
        SyncDirection.DOWNLOAD_CLOUD -> if (zh) {
            "云端已有数据而本地为空，建议先从云端恢复，避免用空库覆盖云端。"
        } else {
            "Cloud has data while local vault is empty. Download first to avoid wiping cloud data."
        }
        SyncDirection.UPLOAD_LOCAL -> if (zh) {
            if (snapshot.localHasData) "云端为空，本地有数据，可以上传初始化云端。" else "本地与云端都为空，可以执行首次同步。"
        } else {
            if (snapshot.localHasData) "Cloud is empty and local has data. Upload to initialize cloud." else "Both vaults are empty. Initial sync is allowed."
        }
        SyncDirection.CONFLICT -> if (zh) {
            "本地和云端都有数据。上传会使用本次输入的助记词重新加密本地库，并覆盖云端当前备份；下载恢复仍需要旧助记词。"
        } else {
            "Both sides have data. Upload will re-encrypt the local vault with the phrase you enter now and replace the current cloud backup. Download still needs the old phrase."
        }
        SyncDirection.REQUIRES_CONFIRMATION -> if (zh) "继续前需要额外确认。" else "Extra confirmation is required."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (zh) "云同步与加密备份" else "Cloud Sync & Encrypted Backup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = themeColors.onBackground
        )
        Text(
            text = if (zh) {
                "密码库会先在本机使用恢复助记词加密，再上传到你配置的真实对象存储。"
            } else {
                "The vault is encrypted locally with your recovery phrase before uploading to the real object storage you configure."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.onSurfaceVariant
        )
        AnimatedVisibility(
            visible = !useRealCloud,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CloudNoticeCard(
                themeColors = themeColors,
                text = if (zh) {
                    "请先完成云端配置。配置不完整时不会连接云端，也不会执行上传或下载。"
                } else {
                    "Complete cloud configuration first. Upload and download are disabled until the required fields are filled."
                }
            )
        }

        CloudCard(themeColors = themeColors) {
            SectionTitle(
                icon = { Icon(Icons.Outlined.Security, contentDescription = null) },
                title = if (zh) "加密模式" else "Encryption Mode",
                themeColors = themeColors
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = securityMode == SyncSecurityMode.MAXIMUM_SECURITY,
                    onClick = {
                        securityMode = SyncSecurityMode.MAXIMUM_SECURITY
                        preferencesManager.syncSecurityMode = securityMode
                    },
                    label = { Text(if (zh) "每次输入助记词" else "Phrase every time") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) }
                )
                FilterChip(
                    selected = securityMode == SyncSecurityMode.CONVENIENCE,
                    onClick = {
                        securityMode = SyncSecurityMode.CONVENIENCE
                        preferencesManager.syncSecurityMode = securityMode
                    },
                    label = { Text(if (zh) "便捷模式" else "Convenience") },
                    leadingIcon = { Icon(Icons.Outlined.Fingerprint, contentDescription = null) }
                )
            }
            Text(
                text = if (securityMode == SyncSecurityMode.MAXIMUM_SECURITY) {
                    if (zh) "同步时只在内存中使用助记词派生密钥，完成后清空输入。" else "The phrase is used in memory for each sync and cleared afterwards."
                } else {
                    if (zh) "便捷模式会保存模式偏好；当前同步操作仍会要求输入助记词。" else "The preference is saved; this build still asks for the phrase before each sync."
                },
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
        }

        CloudCard(themeColors = themeColors) {
            SectionTitle(
                icon = { Icon(Icons.Outlined.CloudDone, contentDescription = null) },
                title = if (zh) "云端配置" else "Cloud Configuration",
                themeColors = themeColors
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { applyTencentCosDefaults() },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (zh) "填入默认端点" else "Default Endpoint")
                }
                Button(
                    onClick = { testConnection() },
                    enabled = !busy && useRealCloud,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (zh) "测试连接" else "Test")
                }
            }
            OutlinedTextField(
                value = s3Endpoint,
                onValueChange = {
                    s3Endpoint = it
                    preferencesManager.s3Endpoint = it
                },
                label = { Text(if (zh) "Endpoint，例如 cos.ap-guangzhou.myqcloud.com" else "Endpoint, e.g. s3.amazonaws.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = s3Region,
                    onValueChange = {
                        s3Region = it
                        preferencesManager.s3Region = it
                    },
                    label = { Text(if (zh) "区域" else "Region") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = s3Bucket,
                    onValueChange = {
                        s3Bucket = it
                        preferencesManager.s3Bucket = it
                    },
                    label = { Text("Bucket") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            if (isTencentCos && !cosBucketLooksValid) {
                Text(
                    text = if (zh) {
                        "腾讯云 COS 的 Bucket 通常必须包含 APPID，例如 my-vault-1250000000。"
                    } else {
                        "Tencent COS bucket names normally include the APPID, e.g. my-vault-1250000000."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            OutlinedTextField(
                value = s3AccessKey,
                onValueChange = {
                    s3AccessKey = it
                    preferencesManager.s3AccessKey = it
                },
                label = { Text("Access Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
            )
            OutlinedTextField(
                value = s3SecretKey,
                onValueChange = {
                    s3SecretKey = it
                    preferencesManager.s3SecretKey = it
                },
                label = { Text("Secret Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
            )
            OutlinedTextField(
                value = s3SessionToken,
                onValueChange = {
                    s3SessionToken = it
                    preferencesManager.s3SessionToken = it
                },
                label = { Text(if (zh) "Session Token（STS 临时密钥，可选）" else "Session Token (optional STS)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
            )
            OutlinedTextField(
                value = objectPrefix,
                onValueChange = {
                    objectPrefix = normalizePrefixInput(it)
                    preferencesManager.objectPrefix = objectPrefix
                },
                label = { Text(if (zh) "对象前缀" else "Object prefix") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                text = if (useRealCloud) {
                    if (zh) "当前将同步到真实 S3 兼容对象存储。Access Key、Secret Key 和 STS Token 会使用 Android Keystore 加密保存。" else "Real S3-compatible object storage is active. Access Key, Secret Key and STS token are encrypted with Android Keystore."
                } else {
                    if (zh) "云端配置尚未完整，当前不会进行任何云端读写。" else "Cloud settings are incomplete. No cloud read or write will run."
                },
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
            Text(
                text = if (zh) {
                    "发布环境建议优先使用 STS 临时密钥；永久 SecretKey 仅建议用于个人测试或受限子账号。"
                } else {
                    "For release builds, prefer STS temporary credentials. Permanent SecretKeys are best kept to personal testing or restricted sub-accounts."
                },
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
        }

        CloudCard(themeColors = themeColors) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (zh) "同步状态" else "Sync Status",
                    fontWeight = FontWeight.SemiBold,
                    color = themeColors.onBackground
                )
                TextButton(onClick = { refreshSnapshot(showToast = true) }, enabled = !busy && useRealCloud) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (zh) "刷新" else "Refresh")
                }
            }
            Text(
                text = if (useRealCloud) {
                    decisionText
                } else {
                    if (zh) "完成云端配置后，可以测试连接并读取云端备份状态。" else "After cloud configuration is complete, test the connection and refresh cloud backup status."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.onSurfaceVariant
            )
            Text(
                text = if (zh) {
                    "本地 ${snapshot.localItemCount} 条，修订号 $localRevision；云端 ${cloudItemCount ?: "-"} 条，修订号 ${cloudRevision ?: "-"}。"
                } else {
                    "Local: ${snapshot.localItemCount} items, rev $localRevision. Cloud: ${cloudItemCount ?: "-"} items, rev ${cloudRevision ?: "-"}."
                },
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
            Text(
                text = if (useRealCloud) {
                    if (zh) "建议：$recommendedLabel" else "Recommended: $recommendedLabel"
                } else {
                    if (zh) "建议：先完成云端配置" else "Recommended: complete cloud configuration first"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            AnimatedVisibility(
                visible = statusText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant
                )
            }
        }

        CloudCard(themeColors = themeColors) {
            Text(
                text = if (zh) "上传说明" else "Upload Notes",
                fontWeight = FontWeight.SemiBold,
                color = themeColors.onBackground
            )
            Text(
                text = if (zh) {
                    "上传只需要当前本地密码库和你现在输入的助记词。忘记旧助记词时，只要本地数据还在，就可以用新助记词重新上传并覆盖云端备份。"
                } else {
                    "Upload only needs the current local vault and the phrase you enter now. If you forgot the old phrase but still have local data, you can upload again with a new phrase."
                },
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
            if (!snapshot.localHasData && snapshot.cloudHasData) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = confirmEmptyOverwrite, onCheckedChange = { confirmEmptyOverwrite = it })
                    Text(
                        text = if (zh) "我确认要用本地空库覆盖云端" else "Confirm empty local vault overwrites cloud",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant
                    )
                }
            }
        }

        CloudCard(themeColors = themeColors) {
            Text(
                text = if (zh) "同步操作" else "Sync Actions",
                fontWeight = FontWeight.SemiBold,
                color = themeColors.onBackground
            )
            Button(
                onClick = {
                    phraseIsUpload = true
                    syncPhrase = ""
                    showPhraseDialog = true
                },
                enabled = !busy && useRealCloud,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (zh) "加密上传到云端" else "Encrypt & Upload")
            }
            Button(
                onClick = {
                    phraseIsUpload = false
                    syncPhrase = ""
                    showPhraseDialog = true
                },
                enabled = !busy && useRealCloud && cloudHasData,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (zh) "从云端恢复并覆盖本地" else "Download & Replace Local")
            }
            AnimatedVisibility(
                visible = busy,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(20.dp)
                            .graphicsLayer { alpha = 0.9f },
                        strokeWidth = 2.dp
                    )
                    Text(if (zh) "正在同步..." else "Syncing...", color = themeColors.onSurfaceVariant)
                }
            }
        }

        HorizontalDivider(color = themeColors.border)
        Text(
            text = if (zh) {
                "最近本机同步：${formatTime(preferencesManager.lastLocalSyncAt)}"
            } else {
                "Last local sync: ${formatTime(preferencesManager.lastLocalSyncAt)}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.onSurfaceVariant
        )
    }

    if (showPhraseDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) showPhraseDialog = false },
            title = {
                Text(
                    if (phraseIsUpload) {
                        if (zh) "输入恢复助记词以上传" else "Enter Recovery Phrase to Upload"
                    } else {
                        if (zh) "输入恢复助记词以恢复" else "Enter Recovery Phrase to Restore"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = syncPhrase,
                        onValueChange = { syncPhrase = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (zh) "24 词恢复助记词" else "24-word recovery phrase") },
                        minLines = 3,
                        enabled = !busy,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = savePhraseForBiometric,
                            onCheckedChange = { savePhraseForBiometric = it },
                            enabled = !busy
                        )
                        Text(
                            text = if (zh) "同步成功后用指纹保护保存到本机" else "Save on this device behind biometric unlock after a successful sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { fillPhraseFromBiometric() },
                            enabled = !busy && hasSavedPhrase
                        ) {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (zh) "指纹填入" else "Use biometric")
                        }
                        TextButton(
                            onClick = {
                                BiometricKeyStore.clear(context)
                                hasSavedPhrase = false
                                Toast.makeText(context, if (zh) "已清除本机保存的助记词" else "Saved phrase removed.", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !busy && hasSavedPhrase
                        ) {
                            Text(if (zh) "清除保存" else "Remove saved")
                        }
                    }
                    Text(
                        text = if (hasSavedPhrase) {
                            if (zh) "本机已保存助记词，可通过指纹解锁填入。" else "A phrase is saved on this device and can be unlocked with biometrics."
                        } else {
                            if (zh) "未曾保存助记词时，指纹无法找回丢失的助记词。" else "Biometrics cannot recover a phrase that was never saved."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val activeRepo = repo ?: run {
                                    val msg = if (zh) "请先填写完整云端配置" else "Complete cloud configuration first."
                                    statusText = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val deviceId = Settings.Secure.getString(
                                    context.contentResolver,
                                    Settings.Secure.ANDROID_ID
                                ) ?: "unknown"
                                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                                val entities = syncPasswords.map { it.toPasswordEntity() }
                                val result: Result<Unit> = if (phraseIsUpload) {
                                    withContext(Dispatchers.IO) {
                                        activeRepo.uploadEncryptedVault(
                                            recoveryPhrase = syncPhrase,
                                            passwords = entities,
                                            deviceId = deviceId,
                                            deviceName = deviceName,
                                            userConfirmedEmptyOverwrite = confirmEmptyOverwrite
                                        )
                                    }
                                } else {
                                    withContext(Dispatchers.IO) {
                                        activeRepo.downloadAndRestoreVault(
                                            recoveryPhrase = syncPhrase,
                                            currentLocalPasswords = entities,
                                            deviceId = deviceId,
                                            deviceName = deviceName
                                        )
                                    }.map { restored ->
                                        replaceVaultPasswords(restored.map { it.toPasswordItem() })
                                    }
                                }
                                result.onSuccess {
                                    val phraseToSave = syncPhrase
                                    val done = if (zh) "同步完成" else "Sync complete"
                                    statusText = done
                                    Toast.makeText(context, done, Toast.LENGTH_SHORT).show()
                                    if (savePhraseForBiometric && phraseToSave.isNotBlank()) {
                                        savePhraseWithBiometric(phraseToSave)
                                    }
                                    showPhraseDialog = false
                                    syncPhrase = ""
                                    confirmEmptyOverwrite = false
                                    refreshSnapshot()
                                }.onFailure { error ->
                                    val msg = error.message ?: if (zh) "同步失败" else "Sync failed"
                                    statusText = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && syncPhrase.isNotBlank()
                ) {
                    Text(if (zh) "执行" else "Run")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhraseDialog = false }, enabled = !busy) {
                    Text(if (zh) "取消" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun CloudCard(themeColors: ThemeColors, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun CloudNoticeCard(themeColors: ThemeColors, text: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.CloudDone, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SectionTitle(
    icon: @Composable () -> Unit,
    title: String,
    themeColors: ThemeColors
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        icon()
        Text(title, fontWeight = FontWeight.SemiBold, color = themeColors.onBackground)
    }
}

private fun normalizePrefixInput(value: String): String {
    val trimmed = value.trim().trimStart('/')
    if (trimmed.isBlank()) return ""
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
}

private fun formatTime(value: Long): String {
    if (value <= 0L) return "-"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(value))
}

private fun PasswordItem.toPasswordEntity(): PasswordEntity {
    val now = System.currentTimeMillis()
    return PasswordEntity(
        id = id,
        name = name,
        username = username,
        phone = phone,
        email = email,
        password = password,
        category = category,
        note = note,
        iconType = iconType,
        iconValue = iconValue,
        createdAt = now,
        updatedAt = now
    )
}

private fun PasswordEntity.toPasswordItem(): PasswordItem {
    return PasswordItem(
        id = id,
        name = name,
        username = username,
        phone = phone,
        email = email,
        password = password,
        category = category,
        note = note,
        iconType = iconType,
        iconValue = iconValue
    )
}
