package com.example.passcard.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 便捷模式下，使用 Android Keystore 包裹同步相关密钥材料。
 * 密钥要求生物识别（或等价用户认证）后才能用于解密；请在 [unwrapSyncKey] 前完成 [androidx.biometric.BiometricPrompt] 与 [Cipher] 初始化流程。
 */
object BiometricKeyStore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "repasscard_sync_master_key"
    private const val PREFS_NAME = "repasscard_biometric_sync"
    private const val KEY_WRAPPED_SYNC_KEY = "wrapped_sync_key"
    private const val KEY_WRAPPED_SYNC_KEY_IV = "wrapped_sync_key_iv"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    fun hasWrappedSyncKey(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_WRAPPED_SYNC_KEY, null) != null && prefs.getString(KEY_WRAPPED_SYNC_KEY_IV, null) != null
    }

    fun getEncryptionCipher(): Cipher? {
        return runCatching {
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
            cipher
        }.getOrNull()
    }

    fun wrapStringWithCipher(context: Context, cipher: Cipher, phrase: String) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ciphertext = cipher.doFinal(phrase.toByteArray(Charsets.UTF_8))
        prefs.edit {
            putString(KEY_WRAPPED_SYNC_KEY, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            putString(KEY_WRAPPED_SYNC_KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        }
    }

    fun getDecryptionCipher(context: Context): Cipher? {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val iv = prefs.getString(KEY_WRAPPED_SYNC_KEY_IV, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateMasterKey(),
                GCMParameterSpec(GCM_TAG_BITS, Base64.decode(iv, Base64.NO_WRAP))
            )
            cipher
        }.getOrNull()
    }

    fun unwrapStringWithCipher(context: Context, cipher: Cipher): String? {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encrypted = prefs.getString(KEY_WRAPPED_SYNC_KEY, null) ?: return null
        return runCatching {
            val decryptedBytes = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
            String(decryptedBytes, Charsets.UTF_8)
        }.getOrNull()
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_WRAPPED_SYNC_KEY)
            remove(KEY_WRAPPED_SYNC_KEY_IV)
        }
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val specBuilder = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            specBuilder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            specBuilder.setUserAuthenticationValidityDurationSeconds(0)
        }

        keyGenerator.init(specBuilder.build())
        return keyGenerator.generateKey()
    }
}
