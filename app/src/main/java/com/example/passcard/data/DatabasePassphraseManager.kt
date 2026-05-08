package com.example.passcard.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import java.security.SecureRandom
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

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPassphrase = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val iv = prefs.getString(KEY_PASSPHRASE_IV, null)

        if (encryptedPassphrase != null && iv != null) {
            return decryptPassphrase(encryptedPassphrase, iv)
        }

        val passphrase = ByteArray(DATABASE_PASSPHRASE_BYTES)
        SecureRandom().nextBytes(passphrase)

        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val ciphertext = cipher.doFinal(passphrase)

        prefs.edit {
            putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            putString(KEY_PASSPHRASE_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        }

        return passphrase
    }

    private fun decryptPassphrase(encryptedPassphrase: String, iv: String): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM)
        val ivBytes = Base64.decode(iv, Base64.NO_WRAP)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_BITS, ivBytes)
        )
        return cipher.doFinal(Base64.decode(encryptedPassphrase, Base64.NO_WRAP))
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
