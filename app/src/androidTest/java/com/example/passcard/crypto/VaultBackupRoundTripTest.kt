package com.example.passcard.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.passcard.data.PasswordEntity
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Base64
import java.util.UUID

/** Exercises Android's real JSON codec and AES-GCM envelope, including cross-device icon materialization. */
@RunWith(AndroidJUnit4::class)
class VaultBackupRoundTripTest {
    private val backupPassword = "  Violet lanterns drift 夜空 🔐  "
    private val png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg==")

    @Test fun encryptedRoundTripIncludesImagesAndPreservesAllIconMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceDirectory = File(context.cacheDir, "source-icons-${UUID.randomUUID()}")
        val sourceFiles = VaultIconFiles(sourceDirectory)
        val sourceImage = sourceFiles.store(png)
        val sourceStore = object : VaultIconStorage {
            override fun openLocalIcon(reference: String) = sourceFiles.openOwnedReference(reference)
            override fun storeRestoredIcon(bytes: ByteArray) = error("Export should not restore images")
        }
        val manager = VaultBackupManager(iconStorage = sourceStore)
        val originals = listOf(
            PasswordEntity("image", "Example", "user", password = "  secret  ", iconType = "image", iconValue = sourceImage.toURI().toString()),
            PasswordEntity("emoji", "Other", "user", password = "", iconType = "emoji", iconValue = "🔑"),
            PasswordEntity("generated", "Letter", "", password = "", iconType = "generated", iconValue = "letter")
        )
        try {
            val encrypted = manager.exportVault(originals, backupPassword, "device", "Phone", 7)
            assertFalse(encrypted.contains(sourceImage.toURI().toString()))
            assertFalse(encrypted.contains(Base64.getEncoder().encodeToString(png)))
            assertFalse(encrypted.contains("  secret  "))
            assertFalse(JSONObject(encrypted).has("icons"))
            sourceDirectory.deleteRecursively()
            val restored = VaultBackupManager(context = context).importVault(encrypted, backupPassword)
            assertTrue(restored.warnings.isEmpty())
            assertEquals(originals[0].password, restored.passwords[0].password)
            assertEquals("image", restored.passwords[0].iconType)
            val restoredFile = File(java.net.URI(restored.passwords[0].iconValue))
            assertEquals(File(context.filesDir, "vault-icons").canonicalFile, restoredFile.canonicalFile.parentFile)
            assertArrayEquals(png, restoredFile.readBytes())
            assertEquals("emoji", restored.passwords[1].iconType)
            assertEquals("🔑", restored.passwords[1].iconValue)
            assertEquals("generated", restored.passwords[2].iconType)
            assertEquals("letter", restored.passwords[2].iconValue)
        } finally {
            sourceDirectory.deleteRecursively()
        }
    }

    @Test fun passwordBackupMayOmitOptionalIconFields() {
        val metadata = metadata()
        val oldRecord = VaultPasswordRecord("id", "Example", "user", "", "", "secret", "", "", 1, 2)
        // Optional icon fields can be omitted without changing password-format version checks.
        val json = oldRecord.toJson().apply { remove("iconType"); remove("iconValue"); remove("iconAssetId") }
        val oldPayload = VaultPayload.fromJsonString(JSONObject().put("items", org.json.JSONArray().put(json)).toString())
        val encrypted = VaultCrypto().encryptVault(oldPayload, backupPassword, metadata).json
        val restored = VaultBackupManager().importVault(encrypted, backupPassword)
        assertEquals("generated", restored.passwords.single().iconType)
        assertEquals("", restored.passwords.single().iconValue)
        assertTrue(restored.warnings.isEmpty())
    }

    @Test fun imageReferenceCannotReadAppSecrets() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val secret = File(context.filesDir, "sensitive-${UUID.randomUUID()}")
        secret.writeText("private-data")
        try {
            val record = VaultPasswordRecord("id", "Example", "user", "", "", "secret", "", "", 1, 2,
                iconType = "image", iconValue = secret.toURI().toString())
            val encrypted = VaultCrypto().encryptVault(listOf(record), backupPassword, metadata()).json
            val restored = VaultBackupManager(context = context).importVault(encrypted, backupPassword)
            assertEquals("generated", restored.passwords.single().iconType)
            assertEquals("", restored.passwords.single().iconValue)
            assertEquals(1, restored.warnings.size)
            assertEquals("private-data", secret.readText())
        } finally {
            secret.delete()
        }
    }

    @Test fun passwordUsesExactSpacesUnicodeAndCase() {
        val encrypted = VaultCrypto().encryptVault(listOf(record()), backupPassword, metadata()).json
        assertEquals("secret", VaultCrypto().decryptVault(encrypted, backupPassword).second.single().password)
        listOf(backupPassword.trim(), backupPassword.lowercase(), backupPassword.replace("🔐", "🔑")).forEach { wrong ->
            assertThrows(Exception::class.java) { VaultCrypto().decryptVault(encrypted, wrong) }
        }
    }

    @Test fun unicodeNormalizationDoesNotChangeTheEncryptionPassword() {
        val password = "é lanterns over the water"
        val encrypted = VaultCrypto().encryptVault(listOf(record()), password, metadata()).json
        assertThrows(Exception::class.java) { VaultCrypto().decryptVault(encrypted, "e\u0301 lanterns over the water") }
    }

    @Test fun everyMetadataFieldIsAuthenticated() {
        val encrypted = VaultCrypto().encryptVault(listOf(record()), backupPassword, metadata()).json
        listOf("deviceId", "deviceName", "createdAt", "updatedAt", "itemCount").forEach { field ->
            val changed = JSONObject(encrypted)
            val meta = changed.getJSONObject("metadata")
            meta.put(field, if (field.startsWith("device")) "tampered" else 99)
            assertThrows(Exception::class.java) { VaultCrypto().decryptVault(changed.toString(), backupPassword) }
        }
    }

    @Test fun rejectsOldFormatAndKdfDowngradeBeforeAttemptingDecryption() {
        val encrypted = VaultCrypto().encryptVault(listOf(record()), backupPassword, metadata()).json
        val old = JSONObject(encrypted).put("version", 1)
        assertThrows(IllegalArgumentException::class.java) { VaultCrypto().decryptVault(old.toString(), backupPassword) }
        val downgrade = JSONObject(encrypted)
        downgrade.getJSONObject("kdf").getJSONObject("params").put("iterations", 120_000)
        assertThrows(IllegalArgumentException::class.java) { VaultCrypto().decryptVault(downgrade.toString(), backupPassword) }
    }

    @Test fun verifiesPasswordWithoutRestoringImages() {
        val image = VaultIconAsset(iconContentHash(png), Base64.getEncoder().encodeToString(png))
        val payload = VaultPayload(listOf(record()), listOf(image))
        val encrypted = VaultCrypto().encryptVault(payload, backupPassword, metadata()).json
        val storage = object : VaultIconStorage {
            override fun openLocalIcon(reference: String): java.io.InputStream? = error("Verification must not open icon files")
            override fun storeRestoredIcon(bytes: ByteArray): String = error("Verification must not write icon files")
        }
        VaultBackupManager(iconStorage = storage).verifyBackupPassword(encrypted, backupPassword)
        assertThrows(Exception::class.java) { VaultBackupManager(iconStorage = storage).verifyBackupPassword(encrypted, "wrong password") }
    }

    private fun record() = VaultPasswordRecord("id", "Example", "user", "", "", "secret", "", "", 1, 2)
    private fun metadata() = VaultMetadata("device", "Phone", 1, 2, 7, 1, 1,
        VaultCrypto.KDF_VERSION_BACKUP_PASSWORD, VaultCrypto.FORMAT_VERSION)
}
