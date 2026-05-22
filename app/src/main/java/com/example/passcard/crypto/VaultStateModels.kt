package com.example.passcard.crypto

data class VaultExportContext(
    val deviceId: String,
    val deviceName: String,
    val vaultRevision: Long,
    val keyVersion: Int = 1,
    val kdfVersion: Int = 2,
    val formatVersion: Int = 1
)

data class VaultImportResult(
    val metadata: VaultMetadata,
    val passwords: List<com.example.passcard.data.PasswordEntity>
)

enum class VaultSyncMode {
    MAXIMUM_SECURITY,
    CONVENIENCE
}
