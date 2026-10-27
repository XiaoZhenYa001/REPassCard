package com.example.passcard.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.passcard.PassCardApp
import com.example.passcard.crypto.BackupPasswordPolicy
import com.example.passcard.data.AutomaticBackupKeyStore
import com.example.passcard.data.BiometricKeyStore
import com.example.passcard.data.CloudSyncSafety
import com.example.passcard.data.CloudSyncSnapshot
import com.example.passcard.data.SyncDirection
import com.example.passcard.data.toPasswordEntity
import com.example.passcard.data.toPasswordItem
import com.example.passcard.sync.AutomaticBackupScheduler
import com.example.passcard.sync.CloudSyncRepository
import com.example.passcard.sync.VaultBackupSummary
import com.example.passcard.sync.VaultBackupSource
import com.example.passcard.sync.CloudConditionalWriteCheckException
import com.example.passcard.sync.CloudWriteConflictException
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
import com.example.passcard.util.AutoUploadFrequency
import com.example.passcard.util.AutoUploadStatus
import com.example.passcard.util.PreferencesManager
import com.example.passcard.util.SyncSecurityMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.passcard.ui.components.LoadingIndicator
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

private enum class CloudOperationStage {
    AUTHENTICATING,
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
    replaceVaultPasswords: suspend (List<PasswordItem>) -> Unit,
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
    var statusWarnings by remember { mutableStateOf(emptyList<String>()) }
    var showHistory by remember { mutableStateOf(false) }
    var selectedHistoryBackup by remember { mutableStateOf<VaultBackupSummary?>(null) }
    val passwordDialogState = remember { BackupPasswordDialogState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val supportsBiometric = remember(context) {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
    var activePrompt by remember { mutableStateOf<BiometricPrompt?>(null) }
    val authentication = remember { BackupAuthenticationSession() }
    var hasSavedPassword by remember { mutableStateOf(BiometricKeyStore.hasWrappedSyncKey(context)) }
    var autoUploadFrequency by remember {
        mutableStateOf(preferencesManager?.autoUploadFrequency ?: AutoUploadFrequency.OFF)
    }
    var autoUploadDialogVisible by remember { mutableStateOf(false) }
    var pendingAutoUploadFrequency by remember { mutableStateOf(AutoUploadFrequency.OFF) }
    var autoUploadPassword by remember { mutableStateOf("") }
    var autoUploadConsent by remember { mutableStateOf(false) }
    var autoUploadUsesBiometric by remember { mutableStateOf(false) }
    var autoUploadError by remember { mutableStateOf<String?>(null) }

    val currentConfiguration = configuration.value
    val latestConfiguration by rememberUpdatedState(currentConfiguration)
    val useRealCloud = configuration.isComplete
    val repo = remember(preferencesManager, currentConfiguration) {
        preferencesManager?.let { manager ->
            CloudSyncRepository(
                context.applicationContext,
                manager,
                if (useRealCloud) S3CloudStorage(
                    endpoint = currentConfiguration.endpoint,
                    region = currentConfiguration.region,
                    bucketName = currentConfiguration.bucket,
                    accessKey = currentConfiguration.accessKey,
                    secretKey = currentConfiguration.secretKey,
                    sessionToken = currentConfiguration.sessionToken
                ) else null
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
                statusText = cloudOperationFailureMessage(it, zh)
                if (showToast) Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
            }
            stage = null
            busy = false
        }
    }

    fun resetAuthentication() {
        authentication.invalidate()
        val prompt = activePrompt
        activePrompt = null
        prompt?.cancelAuthentication()
        if (stage == CloudOperationStage.AUTHENTICATING) {
            stage = null
            busy = false
        }
    }

    fun authenticationFailed(message: String) {
        stage = null
        busy = false
        if (passwordDialogState.visible) passwordDialogState.errorMessage = message
        if (autoUploadDialogVisible) autoUploadError = message
    }

    fun unlockBackupPassword(onReady: (String) -> Unit) {
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        busy = true
        stage = CloudOperationStage.AUTHENTICATING
        val token = authentication.begin()
        scope.launch {
            val activity = context as? FragmentActivity
            val cipher = withContext(Dispatchers.IO) { BiometricKeyStore.getDecryptionCipher(context) }
            if (!authentication.isCurrent(token)) return@launch
            if (activity == null || cipher == null || !supportsBiometric) {
                authentication.claim(token)
                BiometricKeyStore.clear(context)
                hasSavedPassword = false
                passwordDialogState.usePassword()
                autoUploadUsesBiometric = false
                authenticationFailed(if (zh) "指纹凭据暂不可用，请输入备份密码。" else "Biometric credentials are unavailable. Enter the backup password.")
                return@launch
            }
            runCatching {
                activePrompt = AuthHelper.authenticateWithCipher(
                    activity = activity, cipher = cipher,
                    title = if (zh) "验证指纹" else "Verify biometrics",
                    subtitle = if (zh) "授权使用本机保护的备份密码" else "Authorize the protected backup password on this device",
                    negativeButtonText = if (zh) "取消" else "Cancel",
                    onSuccess = authenticated@ { authenticated ->
                        if (!authentication.claim(token)) return@authenticated
                        activePrompt = null
                        val password = BiometricKeyStore.unwrapStringWithCipher(context, authenticated)
                        if (password.isNullOrEmpty()) {
                            BiometricKeyStore.clear(context)
                            hasSavedPassword = false
                            passwordDialogState.usePassword()
                            autoUploadUsesBiometric = false
                            authenticationFailed(if (zh) "无法解锁备份密码，请手动输入。" else "Unable to unlock the backup password. Enter it manually.")
                        } else {
                            stage = null
                            busy = false
                            onReady(password)
                        }
                    },
                    onError = failed@ { message ->
                        if (!authentication.claim(token)) return@failed
                        activePrompt = null
                        authenticationFailed(message)
                    }
                )
            }.onFailure {
                if (authentication.claim(token)) authenticationFailed(if (zh) "暂时无法启动指纹验证，请改用备份密码。" else "Unable to start biometrics. Use the backup password.")
            }
        }
    }

    fun savePasswordForBiometric(password: String, operationToken: Long) {
        if (!supportsBiometric || !authentication.isCurrent(operationToken) ||
            !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        val activity = context as? FragmentActivity ?: return
        busy = true
        stage = CloudOperationStage.AUTHENTICATING
        val token = authentication.begin()
        fun notSaved() {
            statusWarnings = statusWarnings + if (zh) "本次未启用指纹，下次仍可输入备份密码。" else "Biometrics were not enabled. You can enter the backup password next time."
            busy = false
            stage = null
        }
        scope.launch {
            val cipher = withContext(Dispatchers.IO) { BiometricKeyStore.getEncryptionCipher() }
            if (!authentication.isCurrent(token)) return@launch
            if (cipher == null) {
                authentication.claim(token)
                notSaved()
                return@launch
            }
            runCatching {
                activePrompt = AuthHelper.authenticateWithCipher(
                    activity = activity, cipher = cipher,
                    title = if (zh) "启用备份指纹确认" else "Enable biometric confirmation",
                    subtitle = if (zh) "以后上传和恢复时可用指纹确认" else "Use biometrics for future uploads and restores",
                    negativeButtonText = if (zh) "暂不启用" else "Not now",
                    onSuccess = authenticated@ { authenticated ->
                        if (!authentication.claim(token)) return@authenticated
                        activePrompt = null
                        runCatching { BiometricKeyStore.wrapStringWithCipher(context, authenticated, password) }
                            .onSuccess { hasSavedPassword = true; busy = false; stage = null }
                            .onFailure { notSaved() }
                    },
                    onError = failed@ {
                        if (!authentication.claim(token)) return@failed
                        activePrompt = null
                        notSaved()
                    }
                )
            }.onFailure { if (authentication.claim(token)) notSaved() }
        }
    }

    fun runSync(backupPassword: String) {
        if (busy) return
        val operationToken = authentication.version
        val targetBackup = selectedHistoryBackup
        val isUpload = passwordDialogState.isUpload
        val confirmEmptyOverwrite = passwordDialogState.confirmEmptyOverwrite
        val saveForBiometric = passwordDialogState.manualEntry && passwordDialogState.saveForBiometric && supportsBiometric
        // Keep manually entered text for an in-dialog retry; backgrounding and dismissal still clear it.
        // A biometric secret is passed directly to the operation and never placed in these fields.
        if (!passwordDialogState.manualEntry) passwordDialogState.clearCredentials()
        passwordDialogState.errorMessage = null
        statusWarnings = emptyList()
        busy = true
        scope.launch {
            var completed = false
            statusIsError = false
            try {
                val manager = preferencesManager ?: error(if (zh) "设置尚未就绪，请重试。" else "Settings are not ready. Try again.")
                val activeRepo = repo ?: error(if (zh) "备份尚未就绪，请重试。" else "Backup is not ready. Try again.")
                stage = CloudOperationStage.READING_LOCAL
                val currentItems = if (isUpload) emptyList() else withContext(Dispatchers.IO) { loadAllPasswords() }
                withContext(Dispatchers.IO) { persistCloudConfiguration(manager, currentConfiguration) }
                val deviceId = manager.getOrCreateInstallationId()
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                val entities = withContext(Dispatchers.Default) { currentItems.map { it.toPasswordEntity() } }
                stage = if (isUpload) CloudOperationStage.UPLOADING else CloudOperationStage.DOWNLOADING
                if (isUpload) {
                    activeRepo.uploadEncryptedVault(
                        backupPassword = backupPassword, passwords = entities,
                        deviceId = deviceId, deviceName = deviceName,
                        userConfirmedEmptyOverwrite = confirmEmptyOverwrite,
                        loadPasswords = {
                            val latest = loadAllPasswords()
                            withContext(Dispatchers.Default) { latest.map { it.toPasswordEntity() } }
                        }
                    ).getOrThrow()
                } else {
                    if (targetBackup != null) activeRepo.restoreBackup(
                        summary = targetBackup, backupPassword = backupPassword,
                        currentLocalPasswords = entities, deviceId = deviceId, deviceName = deviceName,
                        replaceLocalVault = { restored -> replaceVaultPasswords(restored.map { it.toPasswordItem() }) }
                    ).getOrThrow() else activeRepo.downloadAndRestoreVault(
                        backupPassword = backupPassword, currentLocalPasswords = entities,
                        deviceId = deviceId, deviceName = deviceName,
                        replaceLocalVault = { restored -> replaceVaultPasswords(restored.map { it.toPasswordItem() }) }
                    ).getOrThrow()
                }
                stage = CloudOperationStage.FINALIZING
                if (useRealCloud && targetBackup == null) withContext(Dispatchers.IO) { fetchSnapshot() }.onSuccess(::applySnapshot)
                localRevision = manager.vaultRevision
                if (!isUpload) statusWarnings = activeRepo.lastRestoreWarnings
                statusText = if (isUpload) {
                    if (zh) "加密备份已上传" else "Encrypted backup uploaded"
                } else if (targetBackup != null) {
                    if (zh) "历史备份已恢复到本机，云端备份未更改" else "History restored on this device; cloud data is unchanged"
                } else {
                    if (zh) "云端备份已恢复" else "Cloud backup restored"
                }
                completed = true
                passwordDialogState.finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                statusIsError = true
                statusText = cloudOperationFailureMessage(error, zh)
                passwordDialogState.errorMessage = statusText
                Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
            } finally {
                stage = null
                busy = false
            }
            if (completed && saveForBiometric) savePasswordForBiometric(backupPassword, operationToken)
        }
    }

    fun confirmSync() {
        if (busy) return
        if (!passwordDialogState.manualEntry && hasSavedPassword) {
            unlockBackupPassword(::runSync)
        } else {
            passwordDialogState.passwordError()?.let {
                passwordDialogState.errorMessage = backupPasswordErrorMessage(it, zh)
                return
            }
            if (passwordDialogState.password.isEmpty()) {
                passwordDialogState.errorMessage = if (zh) "请输入备份密码。" else "Enter the backup password."
                return
            }
            runSync(passwordDialogState.password)
        }
    }

    fun prepareSync(upload: Boolean) {
        if (busy) return
        selectedHistoryBackup = null
        statusWarnings = emptyList()
        if (!useRealCloud) {
            connectionExpanded = true
            statusIsError = true
            statusText = if (zh) "请先完成云端连接" else "Complete cloud connection first."
            return
        }
        scope.launch {
            busy = true
            stage = CloudOperationStage.CHECKING
            statusIsError = false
            try {
                withContext(Dispatchers.IO) { fetchSnapshot() }.onSuccess { latest ->
                    applySnapshot(latest)
                    if (!upload && !latest.cloudHasData) {
                        statusIsError = true
                        statusText = if (zh) "云端还没有可恢复的备份" else "No cloud backup is available."
                    } else passwordDialogState.open(upload, hasSavedPassword && supportsBiometric)
                }.onFailure {
                    statusIsError = true
                    statusText = cloudOperationFailureMessage(it, zh)
                }
            } finally { stage = null; busy = false }
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
                statusText = if (zh) "连接与上传保护验证通过" else "Connection and upload protection verified"
                stage = null
                busy = false
                refreshSnapshot()
            }.onFailure {
                statusIsError = true
                statusText = cloudOperationFailureMessage(it, zh)
                stage = null
                busy = false
            }
        }
    }

    fun disableAutomaticUpload() {
        AutomaticBackupKeyStore.clear(context)
        autoUploadFrequency = AutoUploadFrequency.OFF
        preferencesManager?.autoUploadFrequency = AutoUploadFrequency.OFF
        AutomaticBackupScheduler.reconcile(context, AutoUploadFrequency.OFF)
    }

    fun authorizeAutomaticUpload(backupPassword: String) {
        if (busy || !autoUploadConsent) return
        val manager = preferencesManager ?: return
        val activeRepo = repo ?: return
        busy = true
        autoUploadError = null
        stage = CloudOperationStage.CHECKING
        autoUploadPassword = ""
        scope.launch {
            try {
                activeRepo.verifyBackupPassword(backupPassword).getOrThrow()
                configuration.securityMode = SyncSecurityMode.CONVENIENCE
                withContext(Dispatchers.IO) {
                    persistCloudConfiguration(manager, currentConfiguration.copy(securityMode = SyncSecurityMode.CONVENIENCE))
                    AutomaticBackupKeyStore.save(context, backupPassword)
                    val previousFrequency = manager.autoUploadFrequency
                    manager.autoUploadFrequency = pendingAutoUploadFrequency
                    try {
                        AutomaticBackupScheduler.reconcile(context, pendingAutoUploadFrequency, replace = true)
                    } catch (error: Exception) {
                        manager.autoUploadFrequency = previousFrequency
                        if (previousFrequency == AutoUploadFrequency.OFF) AutomaticBackupKeyStore.clear(context)
                        throw error
                    }
                }
                autoUploadFrequency = pendingAutoUploadFrequency
                statusIsError = false
                statusText = if (zh) "自动上传已设为${autoUploadFrequencyLabel(pendingAutoUploadFrequency, true)}"
                    else "Automatic upload set to ${autoUploadFrequencyLabel(pendingAutoUploadFrequency, false)}"
                autoUploadDialogVisible = false
                autoUploadConsent = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                autoUploadError = cloudOperationFailureMessage(error, zh)
            } finally { busy = false; stage = null }
        }
    }

    fun selectAutomaticUploadFrequency(frequency: AutoUploadFrequency) {
        if (frequency == AutoUploadFrequency.OFF) {
            disableAutomaticUpload()
            statusIsError = false
            statusText = if (zh) "自动上传已关闭，后台密钥已移除" else
                "Automatic upload is off and its background key was removed."
            return
        }
        if (!useRealCloud) {
            connectionExpanded = true
            statusIsError = true
            statusText = if (zh) "请先完成并测试云端连接" else "Complete and test the cloud connection first."
            return
        }
        if ((preferencesManager?.lastSyncedCloudVaultRevision ?: 0L) <= 0L) {
            statusIsError = true
            statusText = if (zh) "请先手动上传或恢复一次，确认本机与云端版本后再开启自动上传。" else
                "Upload or restore once to establish the current cloud version before enabling automatic uploads."
            return
        }
        pendingAutoUploadFrequency = frequency
        autoUploadPassword = ""
        autoUploadConsent = false
        autoUploadError = null
        autoUploadUsesBiometric = hasSavedPassword && supportsBiometric
        autoUploadDialogVisible = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                resetAuthentication()
                passwordDialogState.clearCredentials()
                autoUploadPassword = ""
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            resetAuthentication()
            passwordDialogState.finish()
            autoUploadPassword = ""
        }
    }

    LaunchedEffect(preferencesManager) {
        val manager = preferencesManager ?: return@LaunchedEffect
        val value = withContext(Dispatchers.IO) { readCloudConfiguration(manager) }
        configuration.load(value)
        localRevision = value.vaultRevision
        autoUploadFrequency = manager.autoUploadFrequency
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
                    isError = statusIsError,
                    warnings = statusWarnings
                )
            }
        }
        item(key = "history", contentType = "history") {
            OutlinedButton(onClick = { showHistory = true }, enabled = !busy && repo != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (zh) "查看历史备份" else "View backup history")
            }
        }
        item(key = "connection", contentType = "disclosure") {
            CloudDisclosureCard(
                zh = zh,
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
                zh = zh,
                title = if (zh) "安全与恢复" else "Security & Recovery",
                summary = if (hasSavedPassword) {
                    if (zh) "指纹确认 · 备份密码可用于换机恢复" else "Biometric confirmation · password recovery on a new device"
                } else {
                    if (zh) "独立备份密码 · 可启用指纹确认" else "Separate backup password · optional biometrics"
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
                    hasSavedPassword = hasSavedPassword,
                    busy = busy,
                    onRemoveSavedPassword = {
                        if (!busy) {
                            BiometricKeyStore.clear(context)
                            hasSavedPassword = false
                        }
                    }
                )
                HorizontalDivider(color = themeColors.onSurfaceVariant.copy(alpha = 0.22f))
                AutomaticUploadEditor(
                    zh = zh,
                    themeColors = themeColors,
                    frequency = autoUploadFrequency,
                    onFrequencyChange = ::selectAutomaticUploadFrequency,
                    enabled = !busy,
                    lastRunAt = preferencesManager.lastAutoUploadAt,
                    lastStatus = preferencesManager.lastAutoUploadStatus,
                    lastMessage = preferencesManager.lastAutoUploadMessage
                )
            }
        }
        item(key = "details", contentType = "disclosure") {
            CloudDisclosureCard(
                zh = zh,
                title = if (zh) "同步详情" else "Sync Details",
                summary = if (lastCheckedAt > 0) {
                    if (zh) "上次检查 ${formatTime(lastCheckedAt)}" else "Checked ${formatTime(lastCheckedAt)}"
                } else {
                    if (autoUploadFrequency == AutoUploadFrequency.OFF) {
                        if (zh) "按需检查，自动上传已关闭" else "On-demand checks; automatic upload is off"
                    } else {
                        if (zh) "自动上传：${autoUploadFrequencyLabel(autoUploadFrequency, true)}" else
                            "Automatic upload: ${autoUploadFrequencyLabel(autoUploadFrequency, false)}"
                    }
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
                text = if (zh) {
                    if (autoUploadFrequency == AutoUploadFrequency.OFF) "本机完整记录只会在你确认上传或恢复后读取。" else
                        "启用自动上传后，系统仅在网络、电量和存储条件允许时于所选周期附近读取并加密上传。"
                } else {
                    if (autoUploadFrequency == AutoUploadFrequency.OFF) "The full vault is read only after you confirm an upload or restore." else
                        "When enabled, Android reads and encrypts the vault near the selected interval when system constraints allow."
                },
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.muted,
                modifier = Modifier.padding(horizontal = Spacing12, vertical = Spacing12)
            )
        }
    }

    if (showHistory && repo != null) {
        BackupHistorySheet(repository = repo, cloudAvailable = useRealCloud, zh = zh,
            onDismiss = { showHistory = false },
            onSelect = { summary ->
                selectedHistoryBackup = summary
                statusWarnings = emptyList()
                statusIsError = false
                showHistory = false
                passwordDialogState.open(upload = false, hasSavedPassword = hasSavedPassword && supportsBiometric)
            })
    }
    BackupPasswordDialog(
        state = passwordDialogState, selectedBackup = selectedHistoryBackup, zh = zh,
        themeColors = themeColors, busy = busy, stage = stage,
        localHasData = snapshot.localHasData, cloudHasData = snapshot.cloudHasData,
        hasSavedPassword = hasSavedPassword, supportsBiometric = supportsBiometric,
        onConfirm = ::confirmSync
    )

    if (autoUploadDialogVisible) {
        AlertDialog(
            onDismissRequest = { if (!busy) {
                autoUploadDialogVisible = false
                autoUploadPassword = ""
                autoUploadConsent = false
                autoUploadError = null
            } },
            icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
            title = { Text(if (zh) "授权自动加密上传" else "Authorize automatic encrypted uploads") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing12)) {
                    Text(
                        if (zh) "后台无法弹出指纹窗口。启用后，备份密码会由本设备密钥加密保存，供后台加密上传；关闭后会移除这份授权。" else
                            "Background tasks cannot show a biometric prompt. Enabling this stores a separate device-encrypted copy of your backup password for unattended uploads. Turning it off removes that authorization.",
                        style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant
                    )
                    if (autoUploadUsesBiometric) {
                        Text(if (zh) "确认后使用指纹授权，不需要再次输入备份密码。" else
                            "Confirm with biometrics; your backup password does not need to be entered again.", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { autoUploadUsesBiometric = false; autoUploadError = null }, enabled = !busy) {
                            Text(if (zh) "改用备份密码" else "Use backup password")
                        }
                    } else {
                        OutlinedTextField(
                            value = autoUploadPassword, onValueChange = { autoUploadPassword = it; autoUploadError = null },
                            modifier = Modifier.fillMaxWidth(), label = { Text(if (zh) "备份密码" else "Backup password") },
                            singleLine = true, enabled = !busy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Checkbox(checked = autoUploadConsent, onCheckedChange = { autoUploadConsent = it }, enabled = !busy)
                        Text(if (zh) "允许后台无需再次指纹确认执行加密上传。" else
                            "Allow encrypted uploads in the background without another biometric prompt.",
                            style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp))
                    }
                    autoUploadError?.let { Text(it, color = themeColors.error, style = MaterialTheme.typography.bodySmall) }
                    if (busy) LoadingIndicator(stageLabel(stage, zh), compact = true)
                }
            },
            confirmButton = {
                Button(enabled = !busy && autoUploadConsent && (autoUploadUsesBiometric || autoUploadPassword.isNotEmpty()),
                    onClick = {
                        if (autoUploadUsesBiometric) unlockBackupPassword(::authorizeAutomaticUpload)
                        else authorizeAutomaticUpload(autoUploadPassword)
                    }) { Text(if (zh) "授权并启用" else "Authorize & enable") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = {
                    autoUploadDialogVisible = false
                    autoUploadPassword = ""
                    autoUploadConsent = false
                    autoUploadError = null
                }) { Text(if (zh) "取消" else "Cancel") }
            }
        )
    }
}

@Composable
private fun BackupPasswordDialog(
    state: BackupPasswordDialogState,
    selectedBackup: VaultBackupSummary?,
    zh: Boolean,
    themeColors: ThemeColors,
    busy: Boolean,
    stage: CloudOperationStage?,
    localHasData: Boolean,
    cloudHasData: Boolean,
    hasSavedPassword: Boolean,
    supportsBiometric: Boolean,
    onConfirm: () -> Unit
) {
    if (!state.visible) return
    val manual = state.manualEntry || !hasSavedPassword
    val creating = state.isUpload && !cloudHasData
    AlertDialog(
        onDismissRequest = { if (!busy) state.finish() },
        icon = { Icon(if (manual) Icons.Outlined.Lock else Icons.Outlined.Fingerprint, contentDescription = null) },
        title = { Text(if (state.isUpload) {
            if (creating && manual) { if (zh) "设置备份密码并上传" else "Set backup password & upload" }
            else { if (zh) "上传加密备份" else "Upload encrypted backup" }
        } else if (selectedBackup != null) { if (zh) "恢复历史备份" else "Restore history backup" }
            else { if (zh) "恢复云端备份" else "Restore cloud backup" }) },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing12)) {
                Text(if (manual) {
                    if (state.isUpload) {
                        if (zh) "备份密码独立于应用主密码。请妥善记住，换机或重装后恢复需要它；密码不会发送到云端。"
                        else "This password is separate from your app password. Keep it for recovery on a new device or after reinstalling. It is never sent to the cloud."
                    } else {
                        if (zh) "请输入创建此备份时使用的备份密码。密码错误时不会替换本机记录。"
                        else "Enter the password used to create this backup. A wrong password will not replace your records."
                    }
                } else {
                    if (zh) "确认后使用指纹授权，本机保护的备份密码不会显示在输入框中。"
                    else "Confirm with biometrics. The password protected on this device will not appear in a text field."
                }, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
                selectedBackup?.let { Text(backupDate(it.updatedAt, zh) + " · " + backupSize(it.sizeBytes), style = MaterialTheme.typography.bodyMedium) }
                if (manual) {
                    OutlinedTextField(value = state.password, onValueChange = { state.password = it; state.errorMessage = null },
                        modifier = Modifier.fillMaxWidth(), label = { Text(if (zh) "备份密码" else "Backup password") },
                        singleLine = true, enabled = !busy, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = if (state.isUpload) { { Text(if (zh) "至少 15 个字符，空格和大小写会原样保留。" else "At least 15 characters. Spaces and letter case are preserved exactly.") } } else null)
                    if (state.isUpload) {
                        OutlinedTextField(value = state.confirmation, onValueChange = { state.confirmation = it; state.errorMessage = null },
                            modifier = Modifier.fillMaxWidth(), label = { Text(if (zh) "再次输入备份密码" else "Confirm backup password") },
                            singleLine = true, enabled = !busy, visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    }
                    if (supportsBiometric) Row(verticalAlignment = Alignment.Top) {
                        Checkbox(checked = state.saveForBiometric, onCheckedChange = { state.saveForBiometric = it }, enabled = !busy)
                        Text(if (zh) "成功后启用指纹，下次无需重复输入。" else "Enable biometrics after success so you do not need to type this again.",
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                    }
                } else if (!busy) {
                    TextButton(onClick = state::usePassword) { Text(if (zh) "改用备份密码" else "Use backup password") }
                }
                if (!state.isUpload) Row(verticalAlignment = Alignment.Top) {
                    Checkbox(checked = state.confirmRestore, onCheckedChange = { state.confirmRestore = it }, enabled = !busy)
                    Text(if (zh) "确认替换本机记录。恢复前保存当前记录的加密副本，云端不会被改写。"
                        else "Replace this device's records. An encrypted copy is saved first; cloud data is not changed.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                }
                if (state.isUpload && !localHasData && cloudHasData) Row(verticalAlignment = Alignment.Top) {
                    Checkbox(checked = state.confirmEmptyOverwrite, onCheckedChange = { state.confirmEmptyOverwrite = it }, enabled = !busy)
                    Text(if (zh) "确认使用空的本机保险库覆盖云端。" else "Confirm replacing cloud data with an empty local vault.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                }
                state.errorMessage?.let { Text(it, color = themeColors.error, style = MaterialTheme.typography.bodySmall) }
                if (busy) LoadingIndicator(stageLabel(stage, zh), compact = true)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm,
                enabled = !busy && (!manual || state.password.isNotEmpty()) && (state.isUpload || state.confirmRestore) &&
                    (!state.isUpload || localHasData || !cloudHasData || state.confirmEmptyOverwrite)) {
                Text(if (!manual) { if (zh) "指纹确认" else "Confirm with biometrics" } else { if (zh) "继续" else "Continue" })
            }
        },
        dismissButton = { TextButton(onClick = state::finish, enabled = !busy) { Text(if (zh) "取消" else "Cancel") } }
    )
}

@Composable
private fun CloudHeader(zh: Boolean, themeColors: ThemeColors) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (zh) "加密备份" else "Encrypted Backup",
            style = MaterialTheme.typography.headlineLarge,
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
                            if (zh) "备份密码不会发送到云端" else "Your backup password is never sent to the cloud"
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
@OptIn(ExperimentalLayoutApi::class)
private fun CloudPrimaryActions(
    zh: Boolean,
    themeColors: ThemeColors,
    enabled: Boolean,
    onUpload: () -> Unit,
    onDownload: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val singleColumn = maxWidth < 320.dp * LocalDensity.current.fontScale
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing12),
            verticalArrangement = Arrangement.spacedBy(Spacing12),
            maxItemsInEachRow = if (singleColumn) 1 else 2
        ) {
            Button(
                onClick = onUpload,
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 54.dp),
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
                modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.CloudDownload, null)
                Spacer(Modifier.width(8.dp))
                Text(if (zh) "恢复" else "Restore")
            }
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
    isError: Boolean,
    warnings: List<String> = emptyList()
) {
    Card(
        shape = RoundedCornerShape(Radius18),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) themeColors.errorContainer else themeColors.surfaceVariant
        )
    ) {
        if (busy) {
            LoadingIndicator(stageLabel(stage, zh), Modifier.fillMaxWidth().padding(Spacing16), compact = true)
        } else {
            Row(
                Modifier.fillMaxWidth().padding(Spacing16).semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                    null, Modifier.size(18.dp), tint = if (isError) themeColors.error else themeColors.success)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(message, style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) themeColors.error else themeColors.onBackground)
                    if (warnings.isNotEmpty()) Text(warnings.take(3).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall, color = themeColors.warning)
                }
            }
        }
    }
}

@Composable
private fun CloudDisclosureCard(
    zh: Boolean,
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
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        stateDescription = if (expanded) {
                            if (zh) "已展开" else "Expanded"
                        } else {
                            if (zh) "已折叠" else "Collapsed"
                        }
                    }
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (expanded) {
                            if (zh) "收起" else "Collapse"
                        } else {
                            if (zh) "展开" else "Expand"
                        },
                        onClick = onToggle
                    )
                    .padding(Spacing16),
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
    Text(
        if (zh) "自动识别腾讯 COS、阿里云 OSS 和 S3 兼容服务。测试连接会验证读写与防覆盖。" else
            "Tencent COS, Alibaba OSS and S3-compatible services are detected automatically. The test verifies reads, writes and overwrite protection.",
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
    hasSavedPassword: Boolean,
    busy: Boolean,
    onRemoveSavedPassword: () -> Unit
) {
    Text(if (zh) "备份密码只用于加密和恢复备份，不影响打开软件的主密码。换机或重装后，需要输入原备份密码。忘记密码将无法解密备份。"
        else "Your backup password encrypts and restores backups and is separate from app unlock. Keep it for recovery on a new device or after reinstalling. Forgotten backup passwords cannot be recovered.",
        style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
    Text(if (hasSavedPassword) {
        if (zh) "已启用指纹确认。系统指纹变化或本机凭据失效时，可以改用备份密码并重新绑定。"
        else "Biometric confirmation is enabled. If biometrics change or the device key is invalidated, enter the backup password to rebind it."
    } else {
        if (zh) "上传或恢复成功后，可启用指纹确认。备份密码由系统密钥加密保存在本机，不会上传。"
        else "After a successful upload or restore, enable biometric confirmation. A system key protects the password on this device; it is not uploaded."
    }, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
    if (hasSavedPassword) TextButton(onClick = onRemoveSavedPassword, enabled = !busy) {
        Text(if (zh) "关闭本机指纹确认" else "Disable biometric confirmation on this device")
    }
}

@Composable
private fun AutomaticUploadEditor(
    zh: Boolean,
    themeColors: ThemeColors,
    frequency: AutoUploadFrequency,
    onFrequencyChange: (AutoUploadFrequency) -> Unit,
    enabled: Boolean,
    lastRunAt: Long,
    lastStatus: AutoUploadStatus,
    lastMessage: String
) {
    Text(
        if (zh) "密码自动上传" else "Automatic password upload",
        style = MaterialTheme.typography.titleSmall,
        color = themeColors.onBackground
    )
    Text(
        if (zh) "选择周期后，Android 会在周期附近运行；具体时间由系统统一调度。" else
            "Android runs near the selected interval; the exact time is system-managed.",
        style = MaterialTheme.typography.bodySmall,
        color = themeColors.onSurfaceVariant
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(AutoUploadFrequency.OFF, AutoUploadFrequency.DAILY).forEach { option ->
            FilterChip(
                selected = frequency == option,
                onClick = { onFrequencyChange(option) },
                enabled = enabled,
                label = { Text(autoUploadFrequencyLabel(option, zh)) }
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(AutoUploadFrequency.WEEKLY, AutoUploadFrequency.MONTHLY).forEach { option ->
            FilterChip(
                selected = frequency == option,
                onClick = { onFrequencyChange(option) },
                enabled = enabled,
                label = { Text(autoUploadFrequencyLabel(option, zh)) }
            )
        }
    }
    if (lastStatus != AutoUploadStatus.NEVER || lastRunAt > 0L) {
        val statusColor = when (lastStatus) {
            AutoUploadStatus.ERROR, AutoUploadStatus.CONFLICT -> themeColors.error
            AutoUploadStatus.SUCCESS -> themeColors.success
            else -> themeColors.onSurfaceVariant
        }
        Text(
            buildString {
                append(if (zh) "上次后台任务：" else "Last background run: ")
                append(formatTime(lastRunAt))
                if (lastMessage.isNotBlank()) append(" · ").append(lastMessage)
            },
            style = MaterialTheme.typography.bodySmall,
            color = statusColor
        )
    }
}

private fun autoUploadFrequencyLabel(frequency: AutoUploadFrequency, zh: Boolean): String = when (frequency) {
    AutoUploadFrequency.OFF -> if (zh) "关闭" else "Off"
    AutoUploadFrequency.DAILY -> if (zh) "每天" else "Daily"
    AutoUploadFrequency.WEEKLY -> if (zh) "每周" else "Weekly"
    AutoUploadFrequency.MONTHLY -> if (zh) "每月" else "Monthly"
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
    CloudOperationStage.AUTHENTICATING -> if (zh) "等待指纹确认…" else "Waiting for biometric confirmation…"
    CloudOperationStage.CHECKING -> if (zh) "正在安全检查云端状态…" else "Checking cloud status…"
    CloudOperationStage.READING_LOCAL -> if (zh) "正在读取本地保险库…" else "Reading local vault…"
    CloudOperationStage.UPLOADING -> if (zh) "正在加密并上传…" else "Encrypting and uploading…"
    CloudOperationStage.DOWNLOADING -> if (zh) "正在下载、验证并解密…" else "Downloading, verifying, and decrypting…"
    CloudOperationStage.FINALIZING -> if (zh) "正在完成并校验结果…" else "Finalizing and verifying…"
    null -> if (zh) "正在处理…" else "Working…"
}

private fun cloudOperationFailureMessage(error: Throwable, zh: Boolean): String = when (error) {
    is CloudWriteConflictException -> if (zh) {
        "云端备份已更新，已停止上传。请先保留本地记录，再比较或恢复云端备份。"
    } else {
        "The cloud backup changed, so upload was stopped. Keep your local records safe before comparing or restoring the cloud backup."
    }
    is CloudConditionalWriteCheckException -> if (zh) error.detail else error.detailEnglish
    is javax.crypto.BadPaddingException -> if (zh) {
        "备份密码不正确，或备份内容已损坏。请检查密码后重试。"
    } else {
        "The backup password is incorrect or the backup is damaged. Check the password and try again."
    }
    else -> error.message?.takeIf { it.isNotBlank() }
        ?: if (zh) "云端操作失败，请检查连接后重试。" else "Cloud operation failed. Check the connection and try again."
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
