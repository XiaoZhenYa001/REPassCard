package com.example.passcard.crypto

import android.util.Base64
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class VaultEncryptionResult(
    val file: VaultEncFile,
    val json: String
)

class VaultCrypto {
    private val random = SecureRandom()

    fun encryptVault(
        records: List<VaultPasswordRecord>,
        recoveryPhrase: String,
        metadata: VaultMetadata,
        kdfVersion: Int = KDF_VERSION_PBKDF2,
        formatVersion: Int = 1
    ): VaultEncryptionResult {
        val salt = ByteArray(16).apply { random.nextBytes(this) }
        val dataKey = ByteArray(32).apply { random.nextBytes(this) }
        val payloadJson = VaultPayload(records).toJsonString()
        val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)

        val kdfDetail = KdfParameterDetail(0, 120_000, 1, 32, "base64")
        val kek = deriveKek(recoveryPhrase, salt, kdfDetail, kdfVersion)
        val wrappedDataKey = aesGcmEncrypt(kek, dataKey, associatedData = null)
        val crypto = aesGcmEncrypt(dataKey, payloadBytes, associatedData = metadata.vaultRevision.toString().toByteArray())

        val file = VaultEncFile(
            format = "repasscard-vault",
            version = formatVersion,
            kdf = KdfParameters(
                name = "pbkdf2-hmac-sha256",
                salt = Base64.encodeToString(salt, Base64.NO_WRAP),
                params = kdfDetail
            ),
            wrappedDataKey = wrappedDataKey.toCipherBlob(),
            crypto = crypto.toCipherBlob(),
            metadata = metadata.copy(kdfVersion = kdfVersion, formatVersion = formatVersion)
        )

        Arrays.fill(dataKey, 0)
        Arrays.fill(kek, 0)
        return VaultEncryptionResult(file, file.toJsonString())
    }

    fun decryptVault(json: String, recoveryPhrase: String): Pair<VaultMetadata, List<VaultPasswordRecord>> {
        val encFile = VaultEncFile.fromJsonString(json)
        require(encFile.format == "repasscard-vault") { "Unsupported vault format" }
        require(encFile.version >= 1) { "Unsupported vault version" }
        require(encFile.kdf.name == "pbkdf2-hmac-sha256") { "Unsupported KDF" }

        val salt = Base64.decode(encFile.kdf.salt, Base64.NO_WRAP)
        val kek = deriveKek(recoveryPhrase, salt, encFile.kdf.params, encFile.metadata.kdfVersion)
        val wrappedDataKey = encFile.wrappedDataKey.fromCipherBlob()
        val dataKey = aesGcmDecrypt(kek, wrappedDataKey)
        val cryptoBlob = encFile.crypto.fromCipherBlob()
        val payloadBytes = aesGcmDecrypt(
            dataKey,
            cryptoBlob,
            associatedData = encFile.metadata.vaultRevision.toString().toByteArray()
        )
        val payload = VaultPayload.fromJsonString(String(payloadBytes, Charsets.UTF_8))

        Arrays.fill(kek, 0)
        Arrays.fill(dataKey, 0)

        return encFile.metadata to payload.items
    }

    private fun deriveKek(recoveryPhrase: String, salt: ByteArray, params: KdfParameterDetail, kdfVersion: Int): ByteArray {
        return when {
            kdfVersion >= KDF_VERSION_PBKDF2 ->
                deriveKeyPbkdf2(RecoveryPhraseManager.normalize(recoveryPhrase), salt, params)
            else -> deriveKeyLegacyHmac(recoveryPhrase, salt, params)
        }
    }

    private fun deriveKeyPbkdf2(normalizedPhrase: String, salt: ByteArray, params: KdfParameterDetail): ByteArray {
        val chars = normalizedPhrase.toCharArray()
        return try {
            val spec = PBEKeySpec(chars, salt, params.iterations.coerceAtLeast(10_000), params.hashLength * 8)
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            Arrays.fill(chars, '\u0000')
        }
    }

    private fun deriveKeyLegacyHmac(recoveryPhrase: String, salt: ByteArray, params: KdfParameterDetail): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val key = SecretKeySpec((recoveryPhrase + ":repasscard").toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(key)
        var result = mac.doFinal(salt)
        repeat(params.iterations - 1) {
            mac.init(key)
            result = mac.doFinal(result)
        }
        return result.copyOf(params.hashLength)
    }

    private fun aesGcmEncrypt(keyBytes: ByteArray, plaintext: ByteArray, associatedData: ByteArray?): AesGcmBlob {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val nonce = ByteArray(12).apply { random.nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, nonce))
        if (associatedData != null) cipher.updateAAD(associatedData)
        val ciphertextWithTag = cipher.doFinal(plaintext)
        val split = ciphertextWithTag.size - 16
        return AesGcmBlob(
            nonce = nonce,
            ciphertext = ciphertextWithTag.copyOfRange(0, split),
            tag = ciphertextWithTag.copyOfRange(split, ciphertextWithTag.size)
        )
    }

    private fun aesGcmDecrypt(keyBytes: ByteArray, blob: AesGcmBlob, associatedData: ByteArray? = null): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, blob.nonce))
        if (associatedData != null) cipher.updateAAD(associatedData)
        return cipher.doFinal(blob.ciphertext + blob.tag)
    }

    private fun CipherBlob.fromCipherBlob(): AesGcmBlob {
        return AesGcmBlob(
            nonce = Base64.decode(nonce, Base64.NO_WRAP),
            ciphertext = Base64.decode(ciphertext, Base64.NO_WRAP),
            tag = Base64.decode(tag, Base64.NO_WRAP)
        )
    }

    private fun AesGcmBlob.toCipherBlob(): CipherBlob {
        return CipherBlob(
            algorithm = "AES-256-GCM",
            nonce = Base64.encodeToString(nonce, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            tag = Base64.encodeToString(tag, Base64.NO_WRAP)
        )
    }

    private data class AesGcmBlob(
        val nonce: ByteArray,
        val ciphertext: ByteArray,
        val tag: ByteArray
    )

    companion object {
        const val KDF_VERSION_PBKDF2 = 2
        const val KDF_VERSION_LEGACY_HMAC = 1
    }
}
