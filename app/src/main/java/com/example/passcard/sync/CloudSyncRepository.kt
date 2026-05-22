package com.example.passcard.sync

import android.content.Context
import com.example.passcard.crypto.VaultBackupManager
import com.example.passcard.data.CloudSyncSafety
import com.example.passcard.data.CloudSyncSnapshot
import com.example.passcard.data.PasswordEntity
import com.example.passcard.util.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CloudSyncRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val client: CloudStorageClient,
    private val vaultBackupManager: VaultBackupManager = VaultBackupManager()
) {

    private fun prefix(): String = preferencesManager.objectPrefix

    private fun keyVaultCurrent(): String = "${prefix()}vault/current/vault.enc"
    private fun keyManifest(): String = "${prefix()}vault/current/manifest.json"
    private fun keyBackup(id: String): String = "${prefix()}vault/backups/$id.enc"
    private fun backupsPrefix(): String = "${prefix()}vault/backups/"

    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        client.testConnection()
    }

    suspend fun fetchCloudSnapshot(localItemCount: Int): CloudSyncSnapshot = withContext(Dispatchers.IO) {
        val hasVault = client.getObjectMeta(keyVaultCurrent()).isSuccess
        val manifestRoot = readManifestRoot()
        val current = manifestRoot?.optJSONObject("current")
        val cloudRev = when {
            current != null -> current.optLong("vaultRevision", 0L)
            hasVault -> 1L
            else -> null
        }
        val cloudItems = when {
            current != null -> current.optInt("itemCount")
            hasVault -> null
            else -> null
        }
        CloudSyncSnapshot(
            localItemCount = localItemCount,
            localVaultRevision = preferencesManager.vaultRevision,
            localHasData = localItemCount > 0,
            cloudItemCount = if (hasVault) cloudItems else null,
            cloudVaultRevision = if (hasVault) cloudRev else null,
            cloudHasData = hasVault
        )
    }

    suspend fun uploadEncryptedVault(
        recoveryPhrase: String,
        passwords: List<PasswordEntity>,
        deviceId: String,
        deviceName: String,
        userConfirmedEmptyOverwrite: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val snap = fetchCloudSnapshot(passwords.size)
            check(CloudSyncSafety.shouldAllowUpload(snap, userConfirmedEmptyOverwrite)) {
                "不允许在未确认的情况下用空库覆盖云端。"
            }
            val newRevision = preferencesManager.nextVaultRevision()
            val vaultJson = vaultBackupManager.exportVault(
                passwords, recoveryPhrase, deviceId, deviceName, newRevision
            )
            val vaultBytes = vaultJson.toByteArray(Charsets.UTF_8)
            val backupId = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            client.uploadObject(keyBackup(backupId), vaultBytes).getOrThrow()
            client.uploadObject(keyVaultCurrent(), vaultBytes).getOrThrow()
            val vaultMeta = client.getObjectMeta(keyVaultCurrent()).getOrThrow()
            val manifestStr = buildManifestJson(
                vaultRevision = newRevision,
                itemCount = passwords.size,
                vaultObjectKey = keyVaultCurrent(),
                updatedAt = vaultMeta.lastModified,
                etag = vaultMeta.etag,
                deviceId = deviceId,
                deviceName = deviceName
            )
            client.uploadObject(keyManifest(), manifestStr.toByteArray(Charsets.UTF_8)).getOrThrow()
            pruneRemoteBackups(keep = 7)
            val m = client.getObjectMeta(keyManifest()).getOrThrow()
            preferencesManager.lastSyncedCloudVaultRevision = newRevision
            preferencesManager.lastCloudUpdatedAt = vaultMeta.lastModified
            preferencesManager.lastCloudEtag = m.etag ?: ""
            preferencesManager.lastLocalSyncAt = System.currentTimeMillis()
        }
    }

    suspend fun downloadAndRestoreVault(
        recoveryPhrase: String,
        currentLocalPasswords: List<PasswordEntity>,
        deviceId: String,
        deviceName: String
    ): Result<List<PasswordEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            val vaultBytes = client.downloadObject(keyVaultCurrent()).getOrThrow()
            val encryptedJson = String(vaultBytes, Charsets.UTF_8)
            val localRev = preferencesManager.vaultRevision.coerceAtLeast(1L)
            val backupJson = vaultBackupManager.exportVault(
                currentLocalPasswords,
                recoveryPhrase,
                deviceId,
                deviceName,
                localRev
            )
            LocalVaultBackupStorage.saveEncryptedVaultFile(context, backupJson).getOrThrow()
            val imported = vaultBackupManager.importVault(encryptedJson, recoveryPhrase)
            preferencesManager.vaultRevision = imported.metadata.vaultRevision
            preferencesManager.lastSyncedCloudVaultRevision = imported.metadata.vaultRevision
            preferencesManager.lastCloudUpdatedAt = imported.metadata.updatedAt
            preferencesManager.lastLocalSyncAt = System.currentTimeMillis()
            imported.passwords
        }
    }

    private suspend fun readManifestRoot(): JSONObject? {
        val bytes = client.downloadObject(keyManifest()).getOrNull() ?: return null
        return runCatching { JSONObject(String(bytes, Charsets.UTF_8)) }.getOrNull()
    }

    private suspend fun buildManifestJson(
        vaultRevision: Long,
        itemCount: Int,
        vaultObjectKey: String,
        updatedAt: Long,
        etag: String?,
        deviceId: String,
        deviceName: String
    ): String {
        val metas = client.listObjects(backupsPrefix()).getOrElse { emptyList() }
            .filter { it.key.endsWith(".enc") }
            .sortedByDescending { it.key }
            .take(7)
        val backups = JSONArray()
        metas.forEach { m ->
            val id = m.key.substringAfterLast('/').removeSuffix(".enc")
            backups.put(
                JSONObject().apply {
                    put("backupId", id)
                    put("objectKey", m.key)
                    put("createdAt", m.lastModified)
                    put("itemCount", itemCount)
                    put("summary", "备份 $id，共 $itemCount 条")
                    put("isAutoGeneratedSummary", true)
                    put("keyVersion", 1)
                }
            )
        }
        val current = JSONObject().apply {
            put("objectKey", vaultObjectKey)
            put("updatedAt", updatedAt)
            put("vaultRevision", vaultRevision)
            put("etag", etag ?: "")
            put("itemCount", itemCount)
            put("deviceId", deviceId)
            put("deviceName", deviceName)
        }
        return JSONObject().apply {
            put("format", "repasscard-manifest")
            put("version", 1)
            put("current", current)
            put("backups", backups)
        }.toString()
    }

    private suspend fun pruneRemoteBackups(keep: Int) {
        val metas = client.listObjects(backupsPrefix()).getOrElse { return }
            .filter { it.key.endsWith(".enc") }
            .sortedByDescending { it.key }
        metas.drop(keep).forEach { client.deleteObject(it.key) }
    }
}
