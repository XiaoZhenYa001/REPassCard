package com.example.passcard.crypto

import com.example.passcard.data.PasswordEntity

class VaultBackupManager(
    private val vaultCrypto: VaultCrypto = VaultCrypto()
) {
    fun exportVault(
        passwords: List<PasswordEntity>,
        recoveryPhrase: String,
        deviceId: String,
        deviceName: String,
        vaultRevision: Long,
        keyVersion: Int = 1,
        kdfVersion: Int = VaultCrypto.KDF_VERSION_PBKDF2,
        formatVersion: Int = 1
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
        return vaultCrypto.encryptVault(records, recoveryPhrase, metadata, kdfVersion, formatVersion).json
    }

    fun importVault(json: String, recoveryPhrase: String): ImportedVault {
        val (metadata, records) = vaultCrypto.decryptVault(json, recoveryPhrase)
        return ImportedVault(metadata, records.map { it.toPasswordEntity() })
    }
}

data class ImportedVault(
    val metadata: VaultMetadata,
    val passwords: List<PasswordEntity>
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
        deletedAt = deletedAt
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
        deletedAt = deletedAt
    )
}
