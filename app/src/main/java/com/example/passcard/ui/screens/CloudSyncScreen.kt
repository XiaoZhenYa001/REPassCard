package com.example.passcard.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.passcard.PassCardApp
import com.example.passcard.data.BiometricKeyStore
import com.example.passcard.data.CloudSyncSafety
import com.example.passcard.data.CloudSyncSnapshot
import com.example.passcard.data.SyncDirection
import com.example.passcard.data.toPasswordEntity
import com.example.passcard.data.toPasswordItem
import com.example.passcard.sync.CloudSyncRepository
import com.example.passcard.sync.S3CloudStorage
import com.example.passcard.ui.theme.ElevationLevel
import com.example.passcard.ui.theme.Radius18
import com.example.passcard.ui.theme.Radius24
import com.example.passcard.ui.theme.Spacing12
import com.example.passcard.ui.theme.Spacing16
import com.example.passcard.ui.theme.Spacing20
import com.example.passcard.ui.theme.Spacing24
import com.example.passcard.ui.theme.ThemeColors
import com.example.passcard.ui.theme.softShadow
import com.example.passcard.util.AuthHelper
import com.example.passcard.util.PreferencesManager
import com.example.passcard.util.SyncSecurityMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class CloudConfiguration(
    val securityMode: SyncSecurityMode,
    val objectPrefix: String,
    val endpoint: String,
    val region: String,
    val bucket: String,
    val accessKey: String,
    val secretKey: String,
    val sessionToken: String,
    val vaultRevision: Long = 0L
)

@Stable
private class CloudConfigurationState(initiallyLoaded: Boolean) {
    var loaded by mutableStateOf(initiallyLoaded)
    var securityMode by mutableStateOf(SyncSecurityMode.MAXIMUM_SECURITY)
    var objectPrefix by mutableStateOf("repasscard/")
    var endpoint by mutableStateOf("")
    var region by mutableStateOf("")
    var bucket by mutableStateOf("")
    var accessKey by mutableStateOf("")
    var secretKey by mutableStateOf("")
    var sessionToken by mutableStateOf("")

    val isComplete: Boolean
        get() = endpoint.isNotBlank() && bucket.isNotBlank() &&
            accessKey.isNotBlank() && secretKey.isNotBlank()

    val value: CloudConfiguration
        get() = CloudConfiguration(
            securityMode = securityMode,
            objectPrefix = objectPrefix,
            endpoint = endpoint,
            region = region,
            bucket = bucket,
            accessKey = accessKey,
            secretKey = secretKey,
            sessionToken = sessionToken
        )

    fun load(value: CloudConfiguration) {
        securityMode = value.securityMode
        objectPrefix = value.objectPrefix
        endpoint = value.endpoint
        region = value.region
        bucket = value.bucket
        accessKey = value.accessKey
        secretKey = value.secretKey
        sessionToken = value.sessionToken
        loaded = true
    }
}

@Stable
private class CloudPhraseDialogState {
    var visible by mutableStateOf(false)
    var phrase by mutableStateOf("")
    var isUpload by mutableStateOf(true)
    var saveForBiometric by mutableStateOf(false)
    var confirmEmptyOverwrite by mutableStateOf(false)

    fun open(upload: Boolean) {
        isUpload = upload
        phrase = ""
        confirmEmptyOverwrite = false
        visible = true
    }

    fun dismiss() {
        visible = false
    }

    fun finish() {
        visible = false
        phrase = ""
        confirmEmptyOverwrite = false
    }
}

private enum class CloudOperationStage {
    CHECKING,
    READING_LOCAL,
    UPLOADING,
    DOWNLOADING,
    FINALIZING
}

private fun readCloudConfiguration(manager: PreferencesManager) = CloudConfiguration(
    securityMode = manager.syncSecurityMode,
    objectPrefix = manager.objectPrefix,
    endpoint = manager.s3Endpoint,
    region = manager.s3Region,
    bucket = manager.s3Bucket,
    accessKey = manager.s3AccessKey,
    secretKey = manager.s3SecretKey,
    sessionToken = manager.s3SessionToken,
    vaultRevision = manager.vaultRevision
)

private fun persistCloudConfiguration(manager: PreferencesManager, value: CloudConfiguration) {
    manager.syncSecurityMode = value.securityMode
    manager.objectPrefix = value.objectPrefix
    manager.s3Endpoint = value.endpoint
    manager.s3Region = value.region
    manager.s3Bucket = value.bucket
    manager.s3AccessKey = value.accessKey
    manager.s3SecretKey = value.secretKey
    manager.s3SessionToken = value.sessionToken
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncContent(
    currentLanguage: AppLanguage,
    themeColors: ThemeColors,
    preferencesManager: PreferencesManager?,
    localItemCount: Int,
    replaceVaultPasswords: (List<PasswordItem>) -> Unit,
    loadAllPasswords: suspend () -> List<PasswordItem>,
    modifier: Modifier = Modifier,
    scrollState: LazyListState = rememberLazyListState()
) {
    val zh = currentLanguage == AppLanguage.CHINESE
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val applicationScope = (context.applicationContext as? PassCardApp)?.applicationScope

    val configuration = remember(preferencesManager) {
        CloudConfigurationState(initiallyLoaded = preferencesManager == null)
    }

    var connectionExpanded by rememberSaveable { mutableStateOf(false) }
    var securityExpanded by rememberSaveable { mutableStateOf(false) }
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }

    var cloudRevision by remember { mutableStateOf<Long?>(null) }
    var cloudItemCount by remember { mutableStateOf<Int?>(null) }
    var cloudHasData by remember { mutableStateOf(false) }
    var localRevision by remember { mutableLongStateOf(0L) }
    var lastCheckedAt by remember { mutableLongStateOf(0L) }
    var busy by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf<CloudOperationStage?>(null) }
    var statusText by remember { mutableStateOf("") }
    var statusIsError by remember { mutableStateOf(false) }
    val phraseDialogState = remember { CloudPhraseDialogState() }
    var hasSavedPhrase by remember { mutableStateOf(BiometricKeyStore.hasWrappedSyncKey(context)) }

    val currentConfiguration = configuration.value
    val latestConfiguration by rememberUpdatedState(currentConfiguration)
    val useRealCloud = configuration.isComplete
    val repo = remember(preferencesManager, currentConfiguration) {
        preferencesManager?.takeIf { useRealCloud }?.let { manager ->
            CloudSyncRepository(
                context.applicationContext,
                manager,
                S3CloudStorage(
                    endpoint = currentConfiguration.endpoint,
                    region = currentConfiguration.region,
                    bucketName = currentConfiguration.bucket,
                    accessKey = currentConfiguration.accessKey,
                    secretKey = currentConfiguration.secretKey,
                    sessionToken = currentConfiguration.sessionToken
                )
            )
        }
    }
    val snapshot = CloudSyncSnapshot(
        localItemCount = localItemCount,
        localVaultRevision = localRevision,
        localHasData = localItemCount > 0,
        cloudItemCount = cloudItemCount,
        cloudVaultRevision = cloudRevision,
        cloudHasData = cloudHasData
    )
    val decision = CloudSyncSafety.decideInitialSync(snapshot)

    fun applySnapshot(value: CloudSyncSnapshot) {
        cloudRevision = value.cloudVaultRevision
        cloudItemCount = value.cloudItemCount
        cloudHasData = value.cloudHasData
        localRevision = value.localVaultRevision
        lastCheckedAt = System.currentTimeMillis()
    }

    suspend fun fetchSnapshot(): Result<CloudSyncSnapshot> {
        val activeRepo = repo ?: return Result.failure(IllegalStateException(
            if (zh) "请先完成云端连接" else "Complete cloud connection first."
        ))
        return runCatching { activeRepo.fetchCloudSnapshot(localItemCount) }
    }

    fun refreshSnapshot(showToast: Boolean = false) {
        if (busy) return
        scope.launch {
            busy = true
            stage = CloudOperationStage.CHECKING
            statusIsError = false
            val result = withContext(Dispatchers.IO) { fetchSnapshot() }
            result.onSuccess {
                applySnapshot(it)
                statusText = if (zh) "云端状态已更新" else "Cloud status updated"
                if (showToast) Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
            }.onFailure {
                statusIsError = true
                statusText = it.message ?: if (zh) "无法读取云端状态" else "Unable to read cloud status"
                if (showToast) Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
            }
            stage = null
            busy = false
        }
    }

    fun prepareSync(upload: Boolean) {
        if (!useRealCloud) {
            connectionExpanded = true
            statusIsError = true
            statusText = if (zh) "请先完成云端连接" else "Complete cloud connection first."
            return
        }
        if (busy) return
        scope.launch {
            busy = true
            stage = CloudOperationStage.CHECKING
            statusIsError = false
            val result = withContext(Dispatchers.IO) { fetchSnapshot() }
            result.onSuccess { latest ->
                applySnapshot(latest)
                if (!upload && !latest.cloudHasData) {
                    statusIsError = true
                    statusText = if (zh) "云端还没有可恢复的备份" else "No cloud backup is available."
                } else {
                    phraseDialogState.open(upload)
                }
            }.onFailure {
                statusIsError = true
                statusText = it.message ?: if (zh) "连接云端失败" else "Cloud connection failed"
            }
            stage = null
            busy = false
        }
    }

    fun testConnection() {
        val activeRepo = repo ?: run {
            connectionExpanded = true
            statusIsError = true
            statusText = if (zh) "请先填写完整连接信息" else "Complete the connection details first."
            return
        }
        val manager = preferencesManager ?: return
        if (busy) return
        scope.launch {
            busy = true
            stage = CloudOperationStage.CHECKING
            statusIsError = false
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    persistCloudConfiguration(manager, currentConfiguration)
                    activeRepo.testConnection().getOrThrow()
                }
            }
            result.onSuccess {
                statusText = if (zh) "连接成功" else "Connection successful"
                stage = null
                busy = false
                refreshSnapshot()
            }.onFailure {
                statusIsError = true
                statusText = it.message ?: if (zh) "连接失败" else "Connection failed"
                stage = null
                busy = false
            }
        }
    }

    fun fillPhraseFromBiometric() {
        val activity = context as? FragmentActivity ?: return
        val cipher = BiometricKeyStore.getDecryptionCipher(context) ?: run {
            Toast.makeText(context, if (zh) "本机没有已保存的恢复助记词" else "No saved recovery phrase.", Toast.LENGTH_LONG).show()
            return
        }
        AuthHelper.authenticateWithCipher(
            activity = activity,
            cipher = cipher,
            title = if (zh) "使用生物识别填入助记词" else "Unlock Recovery Phrase",
            subtitle = if (zh) "只会解锁保存在本机的内容" else "Unlocks only the phrase stored on this device.",
            onSuccess = { authenticated ->
                BiometricKeyStore.unwrapStringWithCipher(context, authenticated)?.takeIf { it.isNotBlank() }?.let {
                    phraseDialogState.phrase = it
                }
            },
            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        )
    }

    fun savePhraseWithBiometric(phrase: String) {
        val activity = context as? FragmentActivity ?: return
        val cipher = BiometricKeyStore.getEncryptionCipher() ?: return
        AuthHelper.authenticateWithCipher(
            activity = activity,
            cipher = cipher,
            title = if (zh) "保护恢复助记词" else "Protect Recovery Phrase",
            subtitle = if (zh) "使用 Android Keystore 加密保存在本机" else "Encrypt and store it with Android Keystore.",
            onSuccess = {
                BiometricKeyStore.wrapStringWithCipher(context, it, phrase)
                hasSavedPhrase = true
                phraseDialogState.saveForBiometric = false
            },
            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        )
    }

    LaunchedEffect(preferencesManager) {
        val manager = preferencesManager ?: return@LaunchedEffect
        val value = withContext(Dispatchers.IO) { readCloudConfiguration(manager) }
        configuration.load(value)
        localRevision = value.vaultRevision
    }

    LaunchedEffect(configuration.loaded, currentConfiguration) {
        val manager = preferencesManager ?: return@LaunchedEffect
        if (!configuration.loaded) return@LaunchedEffect
        delay(350)
        withContext(Dispatchers.IO) { runCatching { persistCloudConfiguration(manager, currentConfiguration) } }
    }

    DisposableEffect(preferencesManager, configuration.loaded, applicationScope) {
        val manager = preferencesManager
        onDispose {
            if (manager != null && configuration.loaded) {
                applicationScope?.launch { runCatching { persistCloudConfiguration(manager, latestConfiguration) } }
            }
        }
    }

    if (preferencesManager == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (zh) "偏好设置尚未就绪" else "Preferences are not ready.")
        }
        return
    }

    LazyColumn(
        state = scrollState,
        modifier = modifier.fillMaxSize().background(themeColors.background),
        contentPadding = PaddingValues(horizontal = Spacing20, vertical = Spacing20),
        verticalArrangement = Arrangement.spacedBy(Spacing16)
    ) {
        item(key = "header", contentType = "header") {
            CloudHeader(zh = zh, themeColors = themeColors)
        }
        item(key = "overview", contentType = "hero") {
            CloudOverviewCard(
                zh = zh,
                themeColors = themeColors,
                configured = useRealCloud,
                localItemCount = localItemCount,
                cloudItemCount = cloudItemCount,
                lastCheckedAt = lastCheckedAt,
                onRefresh = { refreshSnapshot(showToast = true) },
                refreshEnabled = useRealCloud && !busy
            )
        }
        item(key = "actions", contentType = "actions") {
            CloudPrimaryActions(
                zh = zh,
                themeColors = themeColors,
                enabled = !busy,
                onUpload = { prepareSync(upload = true) },
                onDownload = { prepareSync(upload = false) }
            )
        }
        if (busy || statusText.isNotBlank()) {
            item(key = "progress", contentType = "status") {
                CloudProgressCard(
                    zh = zh,
                    themeColors = themeColors,
                    busy = busy,
                    stage = stage,
                    message = statusText,
                    isError = statusIsError
                )
            }
        }
        item(key = "connection", contentType = "disclosure") {
            CloudDisclosureCard(
                title = if (zh) "云端连接" else "Cloud Connection",
                summary = when {
                    !configuration.loaded -> if (zh) "正在读取配置" else "Loading settings"
                    useRealCloud -> if (zh) "已配置 · ${configuration.bucket.trim()}" else "Configured · ${configuration.bucket.trim()}"
                    else -> if (zh) "需要完成设置" else "Setup required"
                },
                icon = Icons.Outlined.CloudDone,
                accent = themeColors.blue,
                expanded = connectionExpanded,
                onToggle = { connectionExpanded = !connectionExpanded },
                themeColors = themeColors
            ) {
                CloudConnectionEditor(
                    zh = zh,
                    themeColors = themeColors,
                    busy = busy,
                    endpoint = configuration.endpoint,
                    onEndpointChange = { configuration.endpoint = it },
                    region = configuration.region,
                    onRegionChange = { configuration.region = it },
                    bucket = configuration.bucket,
                    onBucketChange = { configuration.bucket = it },
                    accessKey = configuration.accessKey,
                    onAccessKeyChange = { configuration.accessKey = it },
                    secretKey = configuration.secretKey,
                    onSecretKeyChange = { configuration.secretKey = it },
                    sessionToken = configuration.sessionToken,
                    onSessionTokenChange = { configuration.sessionToken = it },
                    objectPrefix = configuration.objectPrefix,
                    onObjectPrefixChange = { configuration.objectPrefix = it },
                    onUseTencentDefaults = {
                        val nextRegion = configuration.region.ifBlank { "ap-guangzhou" }
                        configuration.region = nextRegion
                        configuration.endpoint = "cos.$nextRegion.myqcloud.com"
                        configuration.objectPrefix = normalizePrefixInput(
                            configuration.objectPrefix.ifBlank { "repasscard/" }
                        )
                    },
                    onTestConnection = ::testConnection,
                    canTest = useRealCloud
                )
            }
        }
        item(key = "security", contentType = "disclosure") {
            CloudDisclosureCard(
                title = if (zh) "安全与恢复" else "Security & Recovery",
                summary = if (configuration.securityMode == SyncSecurityMode.MAXIMUM_SECURITY) {
                    if (zh) "每次输入恢复助记词" else "Recovery phrase every time"
                } else {
                    if (zh) "支持生物识别填入" else "Biometric-assisted entry"
                },
                icon = Icons.Outlined.Security,
                accent = themeColors.success,
                expanded = securityExpanded,
                onToggle = { securityExpanded = !securityExpanded },
                themeColors = themeColors
            ) {
                CloudSecurityEditor(
                    zh = zh,
                    themeColors = themeColors,
                    mode = configuration.securityMode,
                    onModeChange = { configuration.securityMode = it },
                    hasSavedPhrase = hasSavedPhrase,
                    onRemoveSavedPhrase = {
                        BiometricKeyStore.clear(context)
                        hasSavedPhrase = false
                    }
                )
            }
        }
        item(key = "details", contentType = "disclosure") {
            CloudDisclosureCard(
                title = if (zh) "同步详情" else "Sync Details",
                summary = if (lastCheckedAt > 0) {
                    if (zh) "上次检查 ${formatTime(lastCheckedAt)}" else "Checked ${formatTime(lastCheckedAt)}"
                } else {
                    if (zh) "按需检查，不在后台自动访问" else "Checked only when requested"
                },
                icon = Icons.Outlined.Info,
                accent = themeColors.warning,
                expanded = detailsExpanded,
                onToggle = { detailsExpanded = !detailsExpanded },
                themeColors = themeColors
            ) {
                CloudSyncDetails(
                    zh = zh,
                    themeColors = themeColors,
                    snapshot = snapshot,
                    decision = decision.direction,
                    localRevision = localRevision,
                    cloudRevision = cloudRevision,
                    lastLocalSyncAt = preferencesManager.lastLocalSyncAt
                )
            }
        }
        item(key = "footer", contentType = "footer") {
            Text(
                text = if (zh) "完整保险库只会在你确认上传或恢复后读取。" else
                    "The full vault is read only after you confirm an upload or restore.",
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.muted,
                modifier = Modifier.padding(horizontal = Spacing12, vertical = Spacing12)
            )
        }
    }

    CloudPhraseDialog(
        state = phraseDialogState,
        zh = zh,
        themeColors = themeColors,
        busy = busy,
        stage = stage,
        localHasData = snapshot.localHasData,
        cloudHasData = snapshot.cloudHasData,
        hasSavedPhrase = hasSavedPhrase,
        onUseBiometric = ::fillPhraseFromBiometric,
        onConfirm = {
            val phrase = phraseDialogState.phrase
            val isUpload = phraseDialogState.isUpload
            val confirmEmptyOverwrite = phraseDialogState.confirmEmptyOverwrite
            val saveForBiometric = phraseDialogState.saveForBiometric
            scope.launch {
                busy = true
                statusIsError = false
                try {
                    stage = CloudOperationStage.READING_LOCAL
                    val latestPasswords = withContext(Dispatchers.IO) { loadAllPasswords() }
                    withContext(Dispatchers.IO) {
                        persistCloudConfiguration(preferencesManager, currentConfiguration)
                    }
                    val deviceId = preferencesManager.getOrCreateInstallationId()
                    val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                    val entities = latestPasswords.map { it.toPasswordEntity() }
                    stage = if (isUpload) CloudOperationStage.UPLOADING else CloudOperationStage.DOWNLOADING
                    val result = if (isUpload) {
                        withContext(Dispatchers.IO) {
                            repo?.uploadEncryptedVault(
                                recoveryPhrase = phrase,
                                passwords = entities,
                                deviceId = deviceId,
                                deviceName = deviceName,
                                userConfirmedEmptyOverwrite = confirmEmptyOverwrite
                            ) ?: Result.failure(IllegalStateException("Cloud repository is unavailable"))
                        }
                    } else {
                        withContext(Dispatchers.IO) {
                            repo?.downloadAndRestoreVault(
                                recoveryPhrase = phrase,
                                currentLocalPasswords = entities,
                                deviceId = deviceId,
                                deviceName = deviceName
                            ) ?: Result.failure(IllegalStateException("Cloud repository is unavailable"))
                        }.map { restored -> replaceVaultPasswords(restored.map { it.toPasswordItem() }) }
                    }
                    stage = CloudOperationStage.FINALIZING
                    result.getOrThrow()
                    if (saveForBiometric && phrase.isNotBlank()) {
                        savePhraseWithBiometric(phrase)
                    }
                    withContext(Dispatchers.IO) { fetchSnapshot() }.onSuccess(::applySnapshot)
                    statusText = if (isUpload) {
                        if (zh) "加密备份已上传" else "Encrypted backup uploaded"
                    } else {
                        if (zh) "云端备份已恢复" else "Cloud backup restored"
                    }
                    phraseDialogState.finish()
                } catch (error: Throwable) {
                    statusIsError = true
                    statusText = error.message ?: if (zh) "同步失败" else "Sync failed"
                    Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
                } finally {
                    stage = null
                    busy = false
                }
            }
        }
    )
}

@Composable
private fun CloudPhraseDialog(
    state: CloudPhraseDialogState,
    zh: Boolean,
    themeColors: ThemeColors,
    busy: Boolean,
    stage: CloudOperationStage?,
    localHasData: Boolean,
    cloudHasData: Boolean,
    hasSavedPhrase: Boolean,
    onUseBiometric: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!state.visible) return

    AlertDialog(
        onDismissRequest = { if (!busy) state.dismiss() },
        title = {
            Text(
                if (state.isUpload) {
                    if (zh) "加密并上传" else "Encrypt & Upload"
                } else {
                    if (zh) "恢复云端备份" else "Restore Cloud Backup"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing12)) {
                Text(
                    text = if (zh) "恢复助记词只在本次操作期间保留在内存中。" else
                        "Your recovery phrase stays in memory only for this operation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.phrase,
                    onValueChange = { state.phrase = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (zh) "24 词恢复助记词" else "24-word recovery phrase") },
                    minLines = 3,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (state.isUpload && !localHasData && cloudHasData) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.confirmEmptyOverwrite,
                            onCheckedChange = { state.confirmEmptyOverwrite = it },
                            enabled = !busy
                        )
                        Text(
                            if (zh) "确认使用空的本地库覆盖云端" else "Confirm replacing cloud data with an empty local vault",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.saveForBiometric,
                        onCheckedChange = { state.saveForBiometric = it },
                        enabled = !busy
                    )
                    Text(
                        if (zh) "成功后使用生物识别保护并保存在本机" else
                            "Protect and save on this device after success",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (hasSavedPhrase && !busy) {
                    TextButton(onClick = onUseBiometric) {
                        Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (zh) "使用生物识别填入" else "Use biometric")
                    }
                }
                AnimatedVisibility(visible = busy, enter = fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            stageLabel(stage, zh),
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !busy && state.phrase.isNotBlank() &&
                    (!state.isUpload || localHasData || !cloudHasData || state.confirmEmptyOverwrite)
            ) {
                Text(if (zh) "继续" else "Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = state::dismiss, enabled = !busy) {
                Text(if (zh) "取消" else "Cancel")
            }
        }
    )
}

@Composable
private fun CloudHeader(zh: Boolean, themeColors: ThemeColors) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (zh) "加密备份" else "Encrypted Backup",
            style = MaterialTheme.typography.displayLarge,
            color = themeColors.onBackground
        )
        Text(
            text = if (zh) "先在设备上加密，再存入你的云端。" else "Encrypted on your device, then stored in your cloud.",
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.onSurfaceVariant
        )
    }
}

@Composable
private fun CloudOverviewCard(
    zh: Boolean,
    themeColors: ThemeColors,
    configured: Boolean,
    localItemCount: Int,
    cloudItemCount: Int?,
    lastCheckedAt: Long,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().softShadow(themeColors, RoundedCornerShape(Radius24), ElevationLevel.Card),
        shape = RoundedCornerShape(Radius24),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(Spacing20), verticalArrangement = Arrangement.spacedBy(Spacing20)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(themeColors.primaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(24.dp), tint = themeColors.primary)
                }
                Spacer(Modifier.width(Spacing12))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (configured) {
                            if (zh) "保险库已准备好" else "Vault ready"
                        } else {
                            if (zh) "完成云端连接" else "Connect your cloud"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColors.onBackground
                    )
                    Text(
                        if (configured) {
                            if (zh) "助记词不会发送到云端" else "Your phrase never leaves this device"
                        } else {
                            if (zh) "凭据由 Android Keystore 保护" else "Credentials are protected by Android Keystore"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh, enabled = refreshEnabled) {
                    Icon(Icons.Outlined.Refresh, if (zh) "刷新" else "Refresh", tint = if (refreshEnabled) themeColors.primary else themeColors.muted)
                }
            }
            HorizontalDivider(color = themeColors.border)
            Row(Modifier.fillMaxWidth()) {
                CloudMetric(
                    value = localItemCount.toString(),
                    label = if (zh) "本地项目" else "Local items",
                    themeColors = themeColors,
                    modifier = Modifier.weight(1f)
                )
                CloudMetric(
                    value = cloudItemCount?.toString() ?: "—",
                    label = if (lastCheckedAt > 0) {
                        if (zh) "云端项目" else "Cloud items"
                    } else {
                        if (zh) "尚未检查" else "Not checked"
                    },
                    themeColors = themeColors,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CloudMetric(value: String, label: String, themeColors: ThemeColors, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = themeColors.onBackground)
        Text(label, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
    }
}

@Composable
private fun CloudPrimaryActions(
    zh: Boolean,
    themeColors: ThemeColors,
    enabled: Boolean,
    onUpload: () -> Unit,
    onDownload: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing12)) {
        Button(
            onClick = onUpload,
            enabled = enabled,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
        ) {
            Icon(Icons.Outlined.CloudUpload, null)
            Spacer(Modifier.width(8.dp))
            Text(if (zh) "上传备份" else "Upload")
        }
        OutlinedButton(
            onClick = onDownload,
            enabled = enabled,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Outlined.CloudDownload, null)
            Spacer(Modifier.width(8.dp))
            Text(if (zh) "恢复" else "Restore")
        }
    }
}

@Composable
private fun CloudProgressCard(
    zh: Boolean,
    themeColors: ThemeColors,
    busy: Boolean,
    stage: CloudOperationStage?,
    message: String,
    isError: Boolean
) {
    Card(
        shape = RoundedCornerShape(Radius18),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) themeColors.errorContainer else themeColors.surfaceVariant
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(Spacing16), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        null,
                        Modifier.size(18.dp),
                        tint = if (isError) themeColors.error else themeColors.success
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (busy) stageLabel(stage, zh) else message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) themeColors.error else themeColors.onBackground
                )
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CloudDisclosureCard(
    title: String,
    summary: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    themeColors: ThemeColors,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius18),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
    ) {
        Column(Modifier.fillMaxWidth().animateContentSize()) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(Spacing16),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, Modifier.size(20.dp), tint = accent)
                }
                Spacer(Modifier.width(Spacing12))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, color = themeColors.onBackground)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    null,
                    Modifier.size(22.dp).graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                    tint = themeColors.muted
                )
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(horizontal = Spacing16), color = themeColors.border)
                Column(
                    Modifier.fillMaxWidth().padding(Spacing16),
                    verticalArrangement = Arrangement.spacedBy(Spacing12),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun CloudConnectionEditor(
    zh: Boolean,
    themeColors: ThemeColors,
    busy: Boolean,
    endpoint: String,
    onEndpointChange: (String) -> Unit,
    region: String,
    onRegionChange: (String) -> Unit,
    bucket: String,
    onBucketChange: (String) -> Unit,
    accessKey: String,
    onAccessKeyChange: (String) -> Unit,
    secretKey: String,
    onSecretKeyChange: (String) -> Unit,
    sessionToken: String,
    onSessionTokenChange: (String) -> Unit,
    objectPrefix: String,
    onObjectPrefixChange: (String) -> Unit,
    onUseTencentDefaults: () -> Unit,
    onTestConnection: () -> Unit,
    canTest: Boolean
) {
    Text(
        if (zh) "仅在需要上传或恢复时使用这些凭据。敏感字段会加密保存在本机。" else
            "Credentials are used only for upload or restore and are encrypted on this device.",
        style = MaterialTheme.typography.bodySmall,
        color = themeColors.onSurfaceVariant
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onUseTencentDefaults, enabled = !busy) {
            Text(if (zh) "腾讯 COS 默认值" else "Tencent COS defaults")
        }
        TextButton(onClick = onTestConnection, enabled = !busy && canTest) {
            Text(if (zh) "测试连接" else "Test connection")
        }
    }
    OutlinedTextField(endpoint, onEndpointChange, Modifier.fillMaxWidth(), label = { Text("Endpoint") }, singleLine = true, enabled = !busy)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(region, onRegionChange, Modifier.weight(1f), label = { Text(if (zh) "区域" else "Region") }, singleLine = true, enabled = !busy)
        OutlinedTextField(bucket, onBucketChange, Modifier.weight(1f), label = { Text("Bucket") }, singleLine = true, enabled = !busy)
    }
    OutlinedTextField(
        accessKey,
        onAccessKeyChange,
        Modifier.fillMaxWidth(),
        label = { Text("Access Key") },
        singleLine = true,
        enabled = !busy,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
    )
    OutlinedTextField(
        secretKey,
        onSecretKeyChange,
        Modifier.fillMaxWidth(),
        label = { Text("Secret Key") },
        singleLine = true,
        enabled = !busy,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
    )
    OutlinedTextField(
        sessionToken,
        onSessionTokenChange,
        Modifier.fillMaxWidth(),
        label = { Text(if (zh) "Session Token（可选）" else "Session Token (optional)") },
        minLines = 2,
        enabled = !busy,
        visualTransformation = PasswordVisualTransformation()
    )
    OutlinedTextField(
        objectPrefix,
        onObjectPrefixChange,
        Modifier.fillMaxWidth(),
        label = { Text(if (zh) "对象前缀" else "Object prefix") },
        singleLine = true,
        enabled = !busy
    )
}

@Composable
private fun CloudSecurityEditor(
    zh: Boolean,
    themeColors: ThemeColors,
    mode: SyncSecurityMode,
    onModeChange: (SyncSecurityMode) -> Unit,
    hasSavedPhrase: Boolean,
    onRemoveSavedPhrase: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(
            selected = mode == SyncSecurityMode.MAXIMUM_SECURITY,
            onClick = { onModeChange(SyncSecurityMode.MAXIMUM_SECURITY) },
            label = { Text(if (zh) "每次输入" else "Every time") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) }
        )
        FilterChip(
            selected = mode == SyncSecurityMode.CONVENIENCE,
            onClick = { onModeChange(SyncSecurityMode.CONVENIENCE) },
            label = { Text(if (zh) "生物识别" else "Biometric") },
            leadingIcon = { Icon(Icons.Outlined.Fingerprint, null) }
        )
    }
    Text(
        if (mode == SyncSecurityMode.MAXIMUM_SECURITY) {
            if (zh) "助记词每次手动输入，用完立即从界面状态中清除。" else
                "Enter the phrase for each operation; it is cleared immediately afterward."
        } else {
            if (zh) "可将助记词交由 Android Keystore 加密，并通过生物识别填入。" else
                "Android Keystore can protect the phrase for biometric-assisted entry."
        },
        style = MaterialTheme.typography.bodySmall,
        color = themeColors.onSurfaceVariant
    )
    if (hasSavedPhrase) {
        TextButton(onClick = onRemoveSavedPhrase) {
            Text(if (zh) "移除本机保存的助记词" else "Remove saved phrase")
        }
    }
}

@Composable
private fun CloudSyncDetails(
    zh: Boolean,
    themeColors: ThemeColors,
    snapshot: CloudSyncSnapshot,
    decision: SyncDirection,
    localRevision: Long,
    cloudRevision: Long?,
    lastLocalSyncAt: Long
) {
    val recommendation = when (decision) {
        SyncDirection.DOWNLOAD_CLOUD -> if (zh) "建议先恢复云端备份" else "Restore the cloud backup first"
        SyncDirection.UPLOAD_LOCAL -> if (zh) "可以上传本地备份" else "Your local vault is ready to upload"
        SyncDirection.CONFLICT -> if (zh) "操作前会重新检查双方状态" else "Both sides are checked before each operation"
        SyncDirection.REQUIRES_CONFIRMATION -> if (zh) "继续前需要额外确认" else "Extra confirmation is required"
    }
    Text(recommendation, style = MaterialTheme.typography.bodyMedium, color = themeColors.onBackground)
    Text(
        if (zh) {
            "本地 ${snapshot.localItemCount} 项 · 修订 $localRevision\n云端 ${snapshot.cloudItemCount ?: "—"} 项 · 修订 ${cloudRevision ?: "—"}"
        } else {
            "Local ${snapshot.localItemCount} items · revision $localRevision\nCloud ${snapshot.cloudItemCount ?: "—"} items · revision ${cloudRevision ?: "—"}"
        },
        style = MaterialTheme.typography.bodySmall,
        color = themeColors.onSurfaceVariant
    )
    Text(
        if (zh) "最近成功同步：${formatTime(lastLocalSyncAt)}" else "Last successful sync: ${formatTime(lastLocalSyncAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = themeColors.muted
    )
}

private fun stageLabel(stage: CloudOperationStage?, zh: Boolean): String = when (stage) {
    CloudOperationStage.CHECKING -> if (zh) "正在安全检查云端状态…" else "Checking cloud status…"
    CloudOperationStage.READING_LOCAL -> if (zh) "正在读取本地保险库…" else "Reading local vault…"
    CloudOperationStage.UPLOADING -> if (zh) "正在加密并上传…" else "Encrypting and uploading…"
    CloudOperationStage.DOWNLOADING -> if (zh) "正在下载、验证并解密…" else "Downloading, verifying, and decrypting…"
    CloudOperationStage.FINALIZING -> if (zh) "正在完成并校验结果…" else "Finalizing and verifying…"
    null -> if (zh) "正在处理…" else "Working…"
}

private fun normalizePrefixInput(value: String): String {
    val trimmed = value.trim().trimStart('/')
    if (trimmed.isBlank()) return ""
    return if (trimmed.endsWith('/')) trimmed else "$trimmed/"
}

private fun formatTime(value: Long): String {
    if (value <= 0L) return "—"
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(value))
}
