package com.example.passcard.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-bound storage used only after the user explicitly enables unattended backups. */
object AutomaticBackupKeyStore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "repasscard_automatic_backup_key"
    private const val PREFS_NAME = "repasscard_automatic_backup"
    private const val KEY_CIPHERTEXT = "recovery_phrase_ciphertext"
    private const val KEY_IV = "recovery_phrase_iv"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    fun save(context: Context, phrase: String) {
        require(phrase.isNotBlank()) { "恢复助记词不能为空" }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val ciphertext = cipher.doFinal(phrase.toByteArray(Charsets.UTF_8))
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        }
    }

    fun read(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ciphertext = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateKey(),
                    GCMParameterSpec(GCM_TAG_BITS, Base64.decode(iv, Base64.NO_WRAP))
                )
            }
            String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    fun hasKey(context: Context): Boolean = read(context)?.isNotBlank() == true

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
