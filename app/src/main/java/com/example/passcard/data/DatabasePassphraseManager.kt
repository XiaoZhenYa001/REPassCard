package com.example.passcard.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DatabasePassphraseManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "repasscard_database_passphrase_key"
    private const val PREFS_NAME = "repasscard_database_crypto"
    private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_passphrase"
    private const val KEY_PASSPHRASE_IV = "passphrase_iv"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val DATABASE_PASSPHRASE_BYTES = 32

    fun getOrCreatePassphrase(context: Context, hasExistingDatabase: Boolean): ByteArray {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPassphrase = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val iv = prefs.getString(KEY_PASSPHRASE_IV, null)

        if ((encryptedPassphrase == null) != (iv == null)) {
            throw IllegalStateException("Database encryption metadata is incomplete")
        }

        if (encryptedPassphrase != null && iv != null) {
            val passphrase = decryptPassphrase(encryptedPassphrase, iv)
            if (passphrase.size != DATABASE_PASSPHRASE_BYTES) {
                Arrays.fill(passphrase, 0)
                throw IllegalStateException("Database passphrase has an invalid length")
            }
            return passphrase
        }

        if (hasExistingDatabase) {
            throw IllegalStateException("Database encryption metadata is missing")
        }

        val passphrase = ByteArray(DATABASE_PASSPHRASE_BYTES)
        SecureRandom().nextBytes(passphrase)

        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val ciphertext = cipher.doFinal(passphrase)
        return try {
            val persisted = prefs.edit()
                .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(KEY_PASSPHRASE_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .commit()
            if (!persisted) {
                throw IllegalStateException("Unable to persist database encryption metadata")
            }
            passphrase
        } catch (error: Exception) {
            Arrays.fill(passphrase, 0)
            throw error
        } finally {
            Arrays.fill(ciphertext, 0)
        }
    }

    private fun decryptPassphrase(encryptedPassphrase: String, iv: String): ByteArray {
        val ivBytes = Base64.decode(iv, Base64.NO_WRAP)
        return try {
            val ciphertext = Base64.decode(encryptedPassphrase, Base64.NO_WRAP)
            try {
                val cipher = Cipher.getInstance(AES_GCM)
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateSecretKey(),
                    GCMParameterSpec(GCM_TAG_BITS, ivBytes)
                )
                cipher.doFinal(ciphertext)
            } finally {
                Arrays.fill(ciphertext, 0)
            }
        } finally {
            Arrays.fill(ivBytes, 0)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
}
