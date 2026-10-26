package com.example.passcard.crypto

import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64

class VaultEnvelopeValidationTest {
    private val encoder = Base64.getEncoder()
    private fun encoded(size: Int) = encoder.encodeToString(ByteArray(size))
    private fun valid() = VaultEncFile(
        "repasscard-vault", VaultCrypto.FORMAT_VERSION,
        KdfParameters("pbkdf2-hmac-sha256", encoded(16), KdfParameterDetail(0, 600_000, 1, 32, "base64")),
        CipherBlob("AES-256-GCM", encoded(12), encoded(32), encoded(16)),
        CipherBlob("AES-256-GCM", encoded(12), encoded(8), encoded(16)),
        VaultMetadata("device", "Phone", 1, 2, 7, 1, 1, VaultCrypto.KDF_VERSION_BACKUP_PASSWORD, VaultCrypto.FORMAT_VERSION)
    )
    private fun rejects(file: VaultEncFile) { assertThrows(IllegalArgumentException::class.java) { VaultCrypto.validateEnvelope(file) } }

    @Test fun acceptsOnlyTheCurrentPasswordEnvelope() { VaultCrypto.validateEnvelope(valid()) }
    @Test fun rejectsOldAndUnknownFormatVersions() {
        listOf(0, 1, 3, Int.MAX_VALUE).forEach { rejects(valid().copy(version = it)) }
        rejects(valid().copy(metadata = valid().metadata.copy(formatVersion = 1)))
    }
    @Test fun rejectsLegacyAndFutureKdfVersions() {
        listOf(0, 1, 2, 4, Int.MAX_VALUE).forEach { rejects(valid().copy(metadata = valid().metadata.copy(kdfVersion = it))) }
    }
    @Test fun rejectsUnknownKeyVersionsAndAlgorithms() {
        rejects(valid().copy(metadata = valid().metadata.copy(keyVersion = 2)))
        rejects(valid().copy(kdf = valid().kdf.copy(name = "legacy-hmac")))
        rejects(valid().copy(crypto = valid().crypto.copy(algorithm = "AES-CBC")))
    }
    @Test fun rejectsKdfDowngradesAndUnboundedWorkBeforeKeyDerivation() {
        listOf(-1, 0, 10_000, 120_000, 599_999, 600_001, Int.MAX_VALUE).forEach {
            rejects(valid().copy(kdf = valid().kdf.copy(params = valid().kdf.params.copy(iterations = it))))
        }
    }
    @Test fun rejectsUnexpectedKdfSizesAndEncoding() {
        listOf(valid().kdf.params.copy(memory = 1), valid().kdf.params.copy(parallelism = 2),
            valid().kdf.params.copy(hashLength = 16), valid().kdf.params.copy(outputEncoding = "hex")).forEach {
            rejects(valid().copy(kdf = valid().kdf.copy(params = it)))
        }
    }
    @Test fun rejectsInvalidSaltNonceTagAndWrappedKeySizes() {
        rejects(valid().copy(kdf = valid().kdf.copy(salt = encoded(8))))
        rejects(valid().copy(crypto = valid().crypto.copy(nonce = encoded(16))))
        rejects(valid().copy(crypto = valid().crypto.copy(tag = encoded(12))))
        rejects(valid().copy(wrappedDataKey = valid().wrappedDataKey.copy(ciphertext = encoded(16))))
    }
}
