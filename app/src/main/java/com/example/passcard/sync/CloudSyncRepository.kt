package com.example.passcard.sync

import android.content.Context
import com.example.passcard.crypto.VaultBackupManager
import com.example.passcard.crypto.VaultCrypto
import com.example.passcard.crypto.VaultEncFile
import com.example.passcard.crypto.ImportedVault
import com.example.passcard.data.CloudSyncSnapshot
import com.example.passcard.data.PasswordEntity
import com.example.passcard.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CloudSyncRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val client: CloudStorageClient? = null,
    private val vaultBackupManager: VaultBackupManager = VaultBackupManager(context = context)
) {
    var lastRestoreWarnings: List<String> = emptyList()
        private set

    private fun transfer() = CloudVaultTransfer(requireNotNull(client) { "请先完成云端存储配置。" },
        preferencesManager.objectPrefix, JsonCloudVaultCodec)

    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val storage = requireNotNull(client) { "请先完成云端存储配置。" }
            storage.testConnection().getOrThrow()
            storage.verifyCreateOnlyWrites(preferencesManager.objectPrefix).getOrThrow()
        }
    }

    suspend fun listLocalBackupHistory(): Result<List<VaultBackupSummary>> = withContext(Dispatchers.IO) {
        LocalVaultBackupStorage.list(context)
    }

    suspend fun listCloudBackupHistory(): Result<List<VaultBackupSummary>> = withContext(Dispatchers.IO) {
        runCatching { transfer().listBackupHistory() }
    }

    suspend fun restoreBackup(
        summary: VaultBackupSummary,
        backupPassword: String,
        currentLocalPasswords: List<PasswordEntity>,
        deviceId: String,
        deviceName: String,
        replaceLocalVault: suspend (List<PasswordEntity>) -> Unit
    ): Result<List<PasswordEntity>> = withContext(Dispatchers.IO) {
        runCatching { VaultMutationGate.run {
            var restoredRevision = 0L
            val backupCurrent: suspend () -> Unit = {
                saveLocalRollback(currentLocalPasswords, backupPassword, deviceId, deviceName)
            }
            val recordHistoryRestore: (ImportedVault) -> Unit = { restored ->
                // Clear authorization first: history restoration must never authorize a cloud rollback.
                preferencesManager.lastCloudEtag = ""
                preferencesManager.lastSyncedCloudVaultRevision = 0L
                preferencesManager.lastCloudUpdatedAt = 0L
                preferencesManager.vaultRevision = restoredRevision
                lastRestoreWarnings = restored.warnings
            }
            val imported = when (summary.source) {
                VaultBackupSource.LOCAL -> {
                    val bytes = LocalVaultBackupStorage.read(context, summary).getOrThrow()
                    val restored = decodeVault(bytes, backupPassword)
                    restoredRevision = nextHistoryRestoreRevision(preferencesManager.vaultRevision,
                        restored.metadata.vaultRevision, preferencesManager.lastSyncedCloudVaultRevision)
                    backupCurrent()
                    VaultMutationGate.commit {
                        replaceLocalVault(restored.passwords)
                        recordHistoryRestore(restored)
                    }
                    restored
                }
                VaultBackupSource.CLOUD -> transfer().restoreHistoryBackup(summary,
                    decodeVault = { bytes -> decodeVault(bytes, backupPassword) },
                    validateRestored = { selected, restored ->
                        require(restored.metadata.vaultRevision == selected.metadata.vaultRevision &&
                            restored.metadata.itemCount == selected.metadata.itemCount) { "历史备份统计与内容不一致。" }
                        restoredRevision = nextHistoryRestoreRevision(preferencesManager.vaultRevision,
                            restored.metadata.vaultRevision, selected.remoteRevision)
                    },
                    saveLocalBackup = backupCurrent,
                    replaceLocalVault = { replaceLocalVault(it.passwords) },
                    // All history selections restore only this device; the remote manifest is untouched.
                    invalidateCheckpoint = { _, restored -> recordHistoryRestore(restored) }
                )
            }
            imported.passwords
        } }
    }

    private fun decodeVault(bytes: ByteArray, backupPassword: String): ImportedVault =
        vaultBackupManager.importVault(String(bytes, Charsets.UTF_8), backupPassword).also {
            require(it.passwords.size == it.metadata.itemCount) { "备份项目数量与内容不一致。" }
            require(it.metadata.vaultRevision > 0) { "备份修订无效。" }
        }

    private fun saveLocalRollback(passwords: List<PasswordEntity>, backupPassword: String,
        deviceId: String, deviceName: String) {
        val backupJson = vaultBackupManager.exportVault(passwords, backupPassword, deviceId, deviceName,
            preferencesManager.vaultRevision.coerceAtLeast(1L))
        LocalVaultBackupStorage.saveEncryptedVaultFile(context, backupJson).getOrThrow()
    }

    suspend fun fetchCloudSnapshot(localItemCount: Int): CloudSyncSnapshot = withContext(Dispatchers.IO) {
        val head = transfer().readRemote().head
        CloudSyncSnapshot(
            localItemCount = localItemCount,
            localVaultRevision = preferencesManager.vaultRevision,
            localHasData = localItemCount > 0,
            cloudItemCount = head?.itemCount,
            cloudVaultRevision = head?.vaultRevision,
            cloudHasData = head != null
        )
    }

    /** Verifies the current remote backup without uploading, restoring, or materializing icons. */
    suspend fun verifyBackupPassword(backupPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val remote = transfer().readRemote()
            val vault = requireNotNull(remote.vault) { "云端还没有备份，请先手动上传一次。" }
            vaultBackupManager.verifyBackupPassword(String(vault.bytes, Charsets.UTF_8), backupPassword)
        }
    }

    suspend fun uploadEncryptedVault(
        backupPassword: String,
        passwords: List<PasswordEntity>,
        deviceId: String,
        deviceName: String,
        userConfirmedEmptyOverwrite: Boolean,
        loadPasswords: (suspend () -> List<PasswordEntity>)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { VaultMutationGate.runWithSnapshot(
            loadSnapshot = { loadPasswords?.invoke() ?: passwords }
        ) { latestPasswords ->
            val uploaded = transfer().upload(
                localRevision = preferencesManager.vaultRevision,
                lastSyncedRevision = preferencesManager.lastSyncedCloudVaultRevision,
                lastSyncedManifestEtag = preferencesManager.lastCloudEtag,
                itemCount = latestPasswords.size,
                deviceId = deviceId,
                deviceName = deviceName,
                confirmedEmptyOverwrite = userConfirmedEmptyOverwrite,
                validateRemoteVault = { bytes ->
                    vaultBackupManager.verifyBackupPassword(String(bytes, Charsets.UTF_8), backupPassword)
                }
            ) { revision ->
                vaultBackupManager.exportVault(latestPasswords, backupPassword, deviceId, deviceName, revision)
                    .toByteArray(Charsets.UTF_8)
            }
            preferencesManager.recordCloudSync(
                uploaded.head.vaultRevision, uploaded.head.updatedAt, uploaded.manifestEtag
            )
        } }
    }

    suspend fun downloadAndRestoreVault(
        backupPassword: String,
        currentLocalPasswords: List<PasswordEntity>,
        deviceId: String,
        deviceName: String,
        replaceLocalVault: suspend (List<PasswordEntity>) -> Unit
    ): Result<List<PasswordEntity>> = withContext(Dispatchers.IO) {
        runCatching { VaultMutationGate.run {
            val imported = transfer().restoreVault(
                decodeVault = { bytes ->
                    decodeVault(bytes, backupPassword)
                },
                saveLocalBackup = {
                    saveLocalRollback(currentLocalPasswords, backupPassword, deviceId, deviceName)
                },
                replaceLocalVault = { replaceLocalVault(it.passwords) },
                saveCheckpoint = { remote, restored ->
                    preferencesManager.recordCloudSync(
                        restored.metadata.vaultRevision, remote.head!!.updatedAt,
                        (remote.manifest?.meta?.etag ?: remote.vault!!.meta.etag)!!
                    )
                }
            )
            lastRestoreWarnings = imported.warnings
            imported.passwords
        } }
    }
}

/** V3 commits publish immutable vaults by sequence; older manifests remain read compatible. */
internal object JsonCloudVaultCodec : CloudVaultCodec {
    override fun readManifest(bytes: ByteArray): CloudVaultHead {
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        require(root.requiredString("format") == "repasscard-manifest") { "不支持的云端备份清单格式。" }
        val version = root.requiredLong("version")
        require(version in 1L..3L) { "不支持的云端备份清单版本。" }
        val sequence = if (version == 3L) root.requiredLong("sequence").also {
            require(it > 0) { "云端备份提交序号无效。" }
        } else {
            require(!root.has("sequence")) { "旧版备份清单包含无效提交序号。" }
            0L
        }
        val current = root.getJSONObject("current")
        val backups = root.getJSONArray("backups")
        return CloudVaultHead(
            objectKey = current.requiredString("objectKey"),
            vaultRevision = current.requiredLong("vaultRevision"),
            itemCount = current.requiredCount(),
            updatedAt = current.requiredLong("updatedAt"),
            etag = current.requiredString("etag"),
            deviceId = current.optionalString("deviceId"),
            deviceName = current.optionalString("deviceName"),
            backups = (0 until backups.length()).map {
                backups.getJSONObject(it).requiredString("objectKey")
            },
            commitSequence = sequence
        )
    }

    override fun writeManifest(head: CloudVaultHead): ByteArray = JSONObject().apply {
        put("format", "repasscard-manifest")
        require(head.commitSequence >= 0) { "云端备份提交序号无效。" }
        put("version", if (head.commitSequence > 0) 3 else 2)
        if (head.commitSequence > 0) put("sequence", head.commitSequence)
        put("current", JSONObject().apply {
            put("objectKey", head.objectKey)
            put("updatedAt", head.updatedAt)
            put("vaultRevision", head.vaultRevision)
            put("etag", head.etag)
            put("itemCount", head.itemCount)
            put("deviceId", head.deviceId)
            put("deviceName", head.deviceName)
        })
        put("backups", JSONArray().apply {
            head.backups.forEach { key ->
                put(JSONObject().apply {
                    put("backupId", key.substringAfterLast('/').removeSuffix(".enc"))
                    put("objectKey", key)
                })
            }
        })
    }.toString().toByteArray(Charsets.UTF_8)

    override fun readVaultMetadata(bytes: ByteArray, objectKey: String, etag: String): CloudVaultHead {
        val json = String(bytes, Charsets.UTF_8)
        val root = JSONObject(json)
        require(root.requiredString("format") == "repasscard-vault") { "不支持的加密备份格式。" }
        require(root.requiredLong("version") == VaultCrypto.FORMAT_VERSION.toLong()) { "不支持的加密备份版本。" }
        // Parse the entire envelope so malformed/missing cipher fields cannot masquerade as a valid snapshot.
        val file = VaultEncFile.fromJsonString(json)
        VaultCrypto.validateEnvelope(file)
        require(file.kdf.name == "pbkdf2-hmac-sha256" &&
            file.crypto.algorithm == "AES-256-GCM" && file.wrappedDataKey.algorithm == "AES-256-GCM") {
            "不支持的加密备份算法。"
        }
        require(file.crypto.nonce.isNotBlank() && file.crypto.tag.isNotBlank() && file.crypto.ciphertext.isNotBlank() &&
            file.wrappedDataKey.nonce.isNotBlank() && file.wrappedDataKey.tag.isNotBlank() &&
            file.wrappedDataKey.ciphertext.isNotBlank() && file.kdf.salt.isNotBlank()) { "加密备份内容不完整。" }
        val metadata = root.getJSONObject("metadata")
        return CloudVaultHead(
            objectKey, metadata.requiredLong("vaultRevision"), metadata.requiredCount(),
            metadata.requiredLong("updatedAt"), etag,
            metadata.optionalString("deviceId"), metadata.optionalString("deviceName")
        )
    }

    private fun JSONObject.requiredString(name: String): String {
        val value = get(name)
        require(value is String && value.isNotBlank()) { "备份清单缺少有效字段：$name" }
        return value
    }

    private fun JSONObject.optionalString(name: String): String {
        if (!has(name)) return ""
        val value = get(name)
        require(value is String) { "备份清单包含无效字段：$name" }
        return value
    }

    private fun JSONObject.requiredLong(name: String): Long {
        val value = get(name)
        require(value is Int || value is Long) { "备份清单包含无效数字字段：$name" }
        return (value as Number).toLong()
    }

    private fun JSONObject.requiredCount(): Int {
        val value = requiredLong("itemCount")
        require(value in 0L..Int.MAX_VALUE.toLong()) { "备份项目数量无效。" }
        return value.toInt()
    }
}
