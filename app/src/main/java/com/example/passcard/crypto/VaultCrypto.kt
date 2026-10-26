package com.example.passcard.crypto

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class VaultEncryptionResult(val file: VaultEncFile, val json: String)

/** Version 2 exclusively accepts verbatim backup passwords; mnemonic and legacy HMAC formats are unsupported. */
class VaultCrypto {
    private val random = SecureRandom()

    fun encryptVault(
        records: List<VaultPasswordRecord>,
        backupPassword: String,
        metadata: VaultMetadata,
        kdfVersion: Int = KDF_VERSION_BACKUP_PASSWORD,
        formatVersion: Int = FORMAT_VERSION
    ): VaultEncryptionResult = encryptVault(VaultPayload(records), backupPassword, metadata, kdfVersion, formatVersion)

    fun encryptVault(
        payload: VaultPayload,
        backupPassword: String,
        metadata: VaultMetadata,
        kdfVersion: Int = KDF_VERSION_BACKUP_PASSWORD,
        formatVersion: Int = FORMAT_VERSION
    ): VaultEncryptionResult {
        BackupPasswordPolicy.requireUsablePassword(backupPassword)
        require(kdfVersion == KDF_VERSION_BACKUP_PASSWORD && formatVersion == FORMAT_VERSION) { "不支持的备份加密版本。" }
        val authenticatedMetadata = metadata.copy(kdfVersion = kdfVersion, formatVersion = formatVersion)
        require(authenticatedMetadata.keyVersion == KEY_VERSION) { "不支持的备份密钥版本。" }
        val salt = ByteArray(SALT_BYTES).apply { random.nextBytes(this) }
        val dataKey = ByteArray(KEY_BYTES).apply { random.nextBytes(this) }
        var payloadBytes: ByteArray? = null
        var kek: ByteArray? = null
        try {
            payloadBytes = payload.toJsonString().toByteArray(Charsets.UTF_8)
            kek = deriveKey(backupPassword, salt)
            val header = authenticatedHeader(authenticatedMetadata, salt)
            val wrappedDataKey = aesGcmEncrypt(kek, dataKey, header + WRAPPING_PURPOSE)
            val crypto = aesGcmEncrypt(dataKey, payloadBytes, header + PAYLOAD_PURPOSE)
            val file = VaultEncFile(
                format = FORMAT_NAME,
                version = formatVersion,
                kdf = KdfParameters(KDF_NAME, Base64.getEncoder().encodeToString(salt), REQUIRED_KDF_PARAMETERS),
                wrappedDataKey = wrappedDataKey.toCipherBlob(),
                crypto = crypto.toCipherBlob(),
                metadata = authenticatedMetadata
            )
            return VaultEncryptionResult(file, file.toJsonString())
        } finally {
            Arrays.fill(dataKey, 0)
            kek?.fill(0)
            payloadBytes?.fill(0)
        }
    }

    fun decryptVault(json: String, backupPassword: String): Pair<VaultMetadata, List<VaultPasswordRecord>> {
        val (metadata, payload) = decryptVaultPayload(json, backupPassword)
        return metadata to payload.items
    }

    fun decryptVaultPayload(json: String, backupPassword: String): Pair<VaultMetadata, VaultPayload> {
        BackupPasswordPolicy.requireUsablePassword(backupPassword)
        val encFile = VaultEncFile.fromJsonString(json)
        validateEnvelope(encFile)
        val salt = Base64.getDecoder().decode(encFile.kdf.salt)
        var kek: ByteArray? = null
        var dataKey: ByteArray? = null
        var payloadBytes: ByteArray? = null
        try {
            kek = deriveKey(backupPassword, salt)
            val header = authenticatedHeader(encFile.metadata, salt)
            dataKey = aesGcmDecrypt(kek, encFile.wrappedDataKey.fromCipherBlob(), header + WRAPPING_PURPOSE)
            require(dataKey.size == KEY_BYTES) { "备份数据密钥长度无效。" }
            payloadBytes = aesGcmDecrypt(dataKey, encFile.crypto.fromCipherBlob(), header + PAYLOAD_PURPOSE)
            return encFile.metadata to VaultPayload.fromJsonString(String(payloadBytes, Charsets.UTF_8))
        } finally {
            kek?.fill(0)
            dataKey?.fill(0)
            payloadBytes?.fill(0)
        }
    }

    private fun deriveKey(backupPassword: String, salt: ByteArray): ByteArray {
        val chars = backupPassword.toCharArray()
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, KEY_BYTES * 8)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            Arrays.fill(chars, '\u0000')
        }
    }

    /** Explicit length-prefix encoding is stable across JSONObject implementations and authenticates every metadata field. */
    private fun authenticatedHeader(metadata: VaultMetadata, salt: ByteArray): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { data ->
            listOf(FORMAT_NAME, FORMAT_VERSION.toString(), KDF_NAME, PBKDF2_ITERATIONS.toString(),
                Base64.getEncoder().encodeToString(salt), metadata.deviceId, metadata.deviceName,
                metadata.createdAt.toString(), metadata.updatedAt.toString(), metadata.vaultRevision.toString(),
                metadata.itemCount.toString(), metadata.keyVersion.toString(), metadata.kdfVersion.toString(),
                metadata.formatVersion.toString()).forEach { value ->
                val encoded = value.toByteArray(Charsets.UTF_8)
                data.writeInt(encoded.size)
                data.write(encoded)
            }
        }
        bytes.toByteArray()
    }

    private fun aesGcmEncrypt(keyBytes: ByteArray, plaintext: ByteArray, associatedData: ByteArray): AesGcmBlob {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val nonce = ByteArray(NONCE_BYTES).apply { random.nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(associatedData)
        val ciphertextWithTag = cipher.doFinal(plaintext)
        val split = ciphertextWithTag.size - TAG_BYTES
        return AesGcmBlob(nonce, ciphertextWithTag.copyOfRange(0, split), ciphertextWithTag.copyOfRange(split, ciphertextWithTag.size))
    }

    private fun aesGcmDecrypt(keyBytes: ByteArray, blob: AesGcmBlob, associatedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, blob.nonce))
        cipher.updateAAD(associatedData)
        return cipher.doFinal(blob.ciphertext + blob.tag)
    }

    private fun CipherBlob.fromCipherBlob() = AesGcmBlob(Base64.getDecoder().decode(nonce),
        Base64.getDecoder().decode(ciphertext), Base64.getDecoder().decode(tag))

    private fun AesGcmBlob.toCipherBlob() = CipherBlob(CIPHER_NAME, Base64.getEncoder().encodeToString(nonce),
        Base64.getEncoder().encodeToString(ciphertext), Base64.getEncoder().encodeToString(tag))

    private data class AesGcmBlob(val nonce: ByteArray, val ciphertext: ByteArray, val tag: ByteArray)

    companion object {
        const val FORMAT_VERSION = 2
        const val KDF_VERSION_BACKUP_PASSWORD = 3
        const val KEY_VERSION = 1
        const val PBKDF2_ITERATIONS = 600_000
        private const val FORMAT_NAME = "repasscard-vault"
        private const val KDF_NAME = "pbkdf2-hmac-sha256"
        private const val CIPHER_NAME = "AES-256-GCM"
        private const val SALT_BYTES = 16
        private const val KEY_BYTES = 32
        private const val NONCE_BYTES = 12
        private const val TAG_BYTES = 16
        private val WRAPPING_PURPOSE = "wrapped-data-key".toByteArray(Charsets.UTF_8)
        private val PAYLOAD_PURPOSE = "vault-payload".toByteArray(Charsets.UTF_8)
        private val REQUIRED_KDF_PARAMETERS = KdfParameterDetail(0, PBKDF2_ITERATIONS, 1, KEY_BYTES, "base64")

        /** Reject old formats, unknown versions, KDF downgrades and expensive attacker-chosen parameters before deriving a key. */
        fun validateEnvelope(file: VaultEncFile) {
            require(file.format == FORMAT_NAME && file.version == FORMAT_VERSION &&
                file.metadata.formatVersion == FORMAT_VERSION) { "不支持的加密备份版本；此版本仅支持备份密码格式。" }
            require(file.metadata.kdfVersion == KDF_VERSION_BACKUP_PASSWORD && file.metadata.keyVersion == KEY_VERSION &&
                file.kdf.name == KDF_NAME && file.kdf.params == REQUIRED_KDF_PARAMETERS) { "不支持的备份密钥派生参数。" }
            require(Base64.getDecoder().decode(file.kdf.salt).size == SALT_BYTES) { "备份盐值长度无效。" }
            listOf(file.wrappedDataKey, file.crypto).forEach { blob ->
                require(blob.algorithm == CIPHER_NAME && Base64.getDecoder().decode(blob.nonce).size == NONCE_BYTES &&
                    Base64.getDecoder().decode(blob.tag).size == TAG_BYTES && blob.ciphertext.isNotEmpty()) { "备份加密参数无效。" }
            }
            require(Base64.getDecoder().decode(file.wrappedDataKey.ciphertext).size == KEY_BYTES) { "备份数据密钥长度无效。" }
        }
    }
}
