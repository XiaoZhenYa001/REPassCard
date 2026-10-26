package com.example.passcard.crypto

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStream
import java.security.GeneralSecurityException
import java.util.Base64

/** Executes the production AES/GCM and JSON implementation, without Android's mock JSON classes. */
class VaultBackupPasswordRoundTripTest {
    private val password = "  Violet lanterns 夜空 🔐  "
    private val metadata = VaultMetadata("device", "Phone", 1, 2, 7, 1, 1,
        VaultCrypto.KDF_VERSION_BACKUP_PASSWORD, VaultCrypto.FORMAT_VERSION)
    private val record = VaultPasswordRecord("id", "Example", "user", "", "", "  exact secret 🔑  ", "", "", 1, 2)
    private val crypto = VaultCrypto()
    private fun encrypted(backupPassword: String = password) = crypto.encryptVault(listOf(record), backupPassword, metadata).json

    @Test fun preservesNonBmpPasswordAndVaultContents() {
        val result = crypto.decryptVault(encrypted(), password)
        assertEquals(metadata, result.first)
        assertEquals(listOf(record), result.second)
    }

    @Test fun spacesCaseAndWrongPasswordsCannotDecrypt() {
        val json = encrypted()
        listOf(password.trim(), password.lowercase(), "incorrect backup password").forEach { wrong ->
            assertThrows(GeneralSecurityException::class.java) { crypto.decryptVault(json, wrong) }
        }
    }

    @Test fun canonicallyEquivalentUnicodeIsNotNormalized() {
        val composed = "é lanterns above the lake"
        val decomposed = "e\u0301 lanterns above the lake"
        val json = encrypted(composed)
        assertEquals(record, crypto.decryptVault(json, composed).second.single())
        assertThrows(GeneralSecurityException::class.java) { crypto.decryptVault(json, decomposed) }
    }

    @Test fun everyEncryptionUsesFreshSaltNonceAndDataKey() {
        val first = VaultEncFile.fromJsonString(encrypted())
        val second = VaultEncFile.fromJsonString(encrypted())
        assertNotEquals(first.kdf.salt, second.kdf.salt)
        assertNotEquals(first.crypto.nonce, second.crypto.nonce)
        assertNotEquals(first.wrappedDataKey.ciphertext, second.wrappedDataKey.ciphertext)
        assertNotEquals(first.crypto.ciphertext, second.crypto.ciphertext)
        assertEquals(record, crypto.decryptVault(second.toJsonString(), password).second.single())
    }

    @Test fun authenticatesDeviceTimeRevisionAndCountMetadata() {
        val json = encrypted()
        listOf("deviceId", "deviceName", "createdAt", "updatedAt", "vaultRevision", "itemCount").forEach { field ->
            val changed = JSONObject(json)
            changed.getJSONObject("metadata").put(field, if (field.startsWith("device")) "tampered" else 99)
            assertThrows(GeneralSecurityException::class.java) { crypto.decryptVault(changed.toString(), password) }
        }
    }

    @Test fun payloadTamperingCannotDecrypt() {
        val changed = JSONObject(encrypted())
        val payload = changed.getJSONObject("crypto")
        val bytes = Base64.getDecoder().decode(payload.getString("ciphertext"))
        bytes[0] = (bytes[0].toInt() xor 1).toByte()
        payload.put("ciphertext", Base64.getEncoder().encodeToString(bytes))
        assertThrows(GeneralSecurityException::class.java) { crypto.decryptVault(changed.toString(), password) }
    }

    @Test fun encryptedPayloadRetainsEmbeddedIconsAndPortableReferences() {
        val image = "portable-image-content".toByteArray()
        val asset = VaultIconAsset(iconContentHash(image), Base64.getEncoder().encodeToString(image))
        val payload = VaultPayload(listOf(record.copy(iconType = "image", iconAssetId = asset.id)), listOf(asset))
        val json = crypto.encryptVault(payload, password, metadata).json
        assertFalse(json.contains(asset.contentBase64))
        assertEquals(payload, crypto.decryptVaultPayload(json, password).second)
    }

    @Test fun rejectsOldFormatAndKdfDowngradeInsteadOfTryingLegacyDecryption() {
        val json = encrypted()
        val old = JSONObject(json).put("version", 1)
        assertThrows(IllegalArgumentException::class.java) { crypto.decryptVault(old.toString(), password) }
        val downgraded = JSONObject(json)
        downgraded.getJSONObject("metadata").put("kdfVersion", 2)
        assertThrows(IllegalArgumentException::class.java) { crypto.decryptVault(downgraded.toString(), password) }
        val weakened = JSONObject(json)
        weakened.getJSONObject("kdf").getJSONObject("params").put("iterations", 120_000)
        assertThrows(IllegalArgumentException::class.java) { crypto.decryptVault(weakened.toString(), password) }
    }

    @Test fun rejectsFractionalAndStringEncodedVersionOrKdfNumbers() {
        val json = encrypted()
        listOf<Any>(2.5, "2").forEach { value ->
            val changed = JSONObject(json).put("version", value)
            assertThrows(IllegalArgumentException::class.java) { crypto.decryptVault(changed.toString(), password) }
        }
        listOf<Any>(600_000.5, "600000").forEach { value ->
            val changed = JSONObject(json)
            changed.getJSONObject("kdf").getJSONObject("params").put("iterations", value)
            assertThrows(IllegalArgumentException::class.java) { crypto.decryptVault(changed.toString(), password) }
        }
    }

    @Test fun passwordVerificationDoesNotOpenOrWriteIconFiles() {
        val image = "portable-image-content".toByteArray()
        val asset = VaultIconAsset(iconContentHash(image), Base64.getEncoder().encodeToString(image))
        val payload = VaultPayload(listOf(record.copy(iconType = "image", iconAssetId = asset.id)), listOf(asset))
        val json = crypto.encryptVault(payload, password, metadata).json
        var reads = 0
        var writes = 0
        val storage = object : VaultIconStorage {
            override fun openLocalIcon(reference: String): InputStream? { reads++; error("Verification must not read icons") }
            override fun storeRestoredIcon(bytes: ByteArray): String { writes++; error("Verification must not write icons") }
        }
        val manager = VaultBackupManager(iconStorage = storage)
        manager.verifyBackupPassword(json, password)
        assertThrows(GeneralSecurityException::class.java) { manager.verifyBackupPassword(json, "incorrect backup password") }
        assertEquals(0, reads)
        assertEquals(0, writes)
    }
}
