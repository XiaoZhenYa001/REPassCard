package com.example.passcard.data

import android.content.Context
import com.example.passcard.crypto.BackupPasswordPolicy
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
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
 * 使用独立且要求每次强生物识别授权的 Android Keystore 密钥，保存本机备份密码。
 * 请将已初始化的 [Cipher] 交给 [androidx.biometric.BiometricPrompt]，成功后调用对应包裹或解包接口。
 */
object BiometricKeyStore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "repasscard_backup_password_biometric_v2"
    private const val PREFS_NAME = "repasscard_backup_password_biometric_v2"
    private const val KEY_WRAPPED_SYNC_KEY = "wrapped_backup_password"
    private const val KEY_WRAPPED_SYNC_KEY_IV = "wrapped_backup_password_iv"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    fun hasWrappedSyncKey(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_WRAPPED_SYNC_KEY, null) != null && prefs.getString(KEY_WRAPPED_SYNC_KEY_IV, null) != null
    }

    fun getEncryptionCipher(): Cipher? {
        return runCatching {
            val cipher = Cipher.getInstance(AES_GCM)
            try {
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
                cipher
            } catch (_: KeyPermanentlyInvalidatedException) {
                // A newly enrolled biometric requires explicit re-enrollment with a fresh dedicated key.
                deleteMasterKey()
                Cipher.getInstance(AES_GCM).apply { init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey()) }
            }
        }.getOrNull()
    }

    fun wrapStringWithCipher(context: Context, cipher: Cipher, backupPassword: String) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        BackupPasswordPolicy.requireUsablePassword(backupPassword)
        val passwordBytes = backupPassword.toByteArray(Charsets.UTF_8)
        val ciphertext = try { cipher.doFinal(passwordBytes) } finally { passwordBytes.fill(0) }
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
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val masterKey = (keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
                ?: return@runCatching null
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                masterKey,
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
            try { String(decryptedBytes, Charsets.UTF_8).also(BackupPasswordPolicy::requireUsablePassword) }
            finally { decryptedBytes.fill(0) }
        }.getOrNull()
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_WRAPPED_SYNC_KEY)
            remove(KEY_WRAPPED_SYNC_KEY_IV)
        }
        runCatching { deleteMasterKey() }
    }

    private fun deleteMasterKey() {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(MASTER_KEY_ALIAS)
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
