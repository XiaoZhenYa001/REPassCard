package com.example.passcard.crypto

import android.content.Context
import com.example.passcard.data.PasswordEntity

class VaultBackupManager(
    private val vaultCrypto: VaultCrypto = VaultCrypto(),
    context: Context? = null,
    private val iconStorage: VaultIconStorage? = context?.let { AndroidVaultIconStorage(it) }
) {
    fun exportVault(
        passwords: List<PasswordEntity>,
        backupPassword: String,
        deviceId: String,
        deviceName: String,
        vaultRevision: Long,
        keyVersion: Int = 1,
        kdfVersion: Int = VaultCrypto.KDF_VERSION_BACKUP_PASSWORD,
        formatVersion: Int = VaultCrypto.FORMAT_VERSION
    ): String {
        val records = passwords.map { it.toVaultRecord() }
        val now = System.currentTimeMillis()
        val metadata = VaultMetadata(
            deviceId = deviceId,
            deviceName = deviceName,
            createdAt = now,
            updatedAt = now,
            vaultRevision = vaultRevision,
            itemCount = records.size,
            keyVersion = keyVersion,
            kdfVersion = kdfVersion,
            formatVersion = formatVersion
        )
        val payload = VaultIconArchive(iconStorage).prepareExport(records)
        return vaultCrypto.encryptVault(payload, backupPassword, metadata, kdfVersion, formatVersion).json
    }

    /** Validates the password without writing restored icon files or changing the local vault. */
    fun verifyBackupPassword(json: String, backupPassword: String) {
        val (metadata, payload) = vaultCrypto.decryptVaultPayload(json, backupPassword)
        require(payload.items.size == metadata.itemCount) { "备份项目数量与内容不一致。" }
    }

    fun importVault(json: String, backupPassword: String): ImportedVault {
        val (metadata, payload) = vaultCrypto.decryptVaultPayload(json, backupPassword)
        val restored = VaultIconArchive(iconStorage).prepareRestore(payload)
        return ImportedVault(metadata, restored.records.map { it.toPasswordEntity() }, restored.warnings)
    }
}

data class ImportedVault(
    val metadata: VaultMetadata,
    val passwords: List<PasswordEntity>,
    val warnings: List<String> = emptyList()
)

private fun PasswordEntity.toVaultRecord(): VaultPasswordRecord {
    return VaultPasswordRecord(
        id = id,
        name = name,
        username = username,
        phone = phone,
        email = email,
        password = password,
        category = category,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        revision = revision,
        deviceId = deviceId,
        deletedAt = deletedAt,
        iconType = iconType,
        iconValue = iconValue
    )
}

private fun VaultPasswordRecord.toPasswordEntity(): PasswordEntity {
    return PasswordEntity(
        id = id,
        name = name,
        username = username,
        phone = phone,
        email = email,
        password = password,
        category = category,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        revision = revision,
        deviceId = deviceId,
        deletedAt = deletedAt,
        iconType = iconType,
        iconValue = iconValue
    )
}
