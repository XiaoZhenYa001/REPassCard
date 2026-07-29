package com.example.passcard.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.security.MessageDigest
import java.util.UUID

/**
 * 用户偏好设置管理器
 * 使用 SharedPreferences 持久化存储用户设置
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "repasscard_prefs"
        
        // Keys
        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_CLIPBOARD_CLEAR_ENABLED = "clipboard_clear_enabled"
        private const val KEY_CLIPBOARD_CLEAR_DELAY = "clipboard_clear_delay"
        private const val KEY_AUTO_LOCK_DELAY_SECONDS = "auto_lock_delay_seconds"
        private const val KEY_MASTER_PASSWORD_HASH = "master_password_hash"
        private const val KEY_MASTER_PASSWORD_KDF_VERSION = "master_password_kdf_version"
        private const val KEY_MASTER_PASSWORD_KDF_ITERATIONS = "master_password_kdf_iterations"
        private const val KEY_MASTER_PASSWORD_SALT = "master_password_salt"
        private const val KEY_MASTER_PASSWORD_VERIFIER = "master_password_verifier"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_SYNC_SECURITY_MODE = "sync_security_mode"
        private const val KEY_OBJECT_PREFIX = "cloud_object_prefix"
        private const val KEY_S3_ENDPOINT = "s3_endpoint"
        private const val KEY_S3_REGION = "s3_region"
        private const val KEY_S3_BUCKET = "s3_bucket"
        private const val KEY_S3_ACCESS_KEY = "s3_access_key"
        private const val KEY_S3_SECRET_KEY = "s3_secret_key"
        private const val KEY_S3_ACCESS_KEY_ENCRYPTED = "s3_access_key_encrypted"
        private const val KEY_S3_ACCESS_KEY_IV = "s3_access_key_iv"
        private const val KEY_S3_SECRET_KEY_ENCRYPTED = "s3_secret_key_encrypted"
        private const val KEY_S3_SECRET_KEY_IV = "s3_secret_key_iv"
        private const val KEY_S3_SESSION_TOKEN_ENCRYPTED = "s3_session_token_encrypted"
        private const val KEY_S3_SESSION_TOKEN_IV = "s3_session_token_iv"
        private const val KEY_VAULT_REVISION = "vault_revision"
        private const val KEY_LAST_SYNCED_CLOUD_VAULT_REVISION = "last_synced_cloud_vault_revision"
        private const val KEY_LAST_CLOUD_UPDATED_AT = "last_cloud_updated_at"
        private const val KEY_LAST_CLOUD_ETAG = "last_cloud_etag"
        private const val KEY_LAST_LOCAL_SYNC_AT = "last_local_sync_at"
        private const val KEY_INSTALLATION_ID = "installation_id"
        private const val KEY_RANDOM_PASSWORD_LENGTH = "random_password_length"
        private const val KEY_RANDOM_PASSWORD_UPPERCASE = "random_password_uppercase"
        private const val KEY_RANDOM_PASSWORD_LOWERCASE = "random_password_lowercase"
        private const val KEY_RANDOM_PASSWORD_NUMBERS = "random_password_numbers"
        private const val KEY_RANDOM_PASSWORD_SYMBOLS = "random_password_symbols"
        
        // Default Values
        private const val DEFAULT_THEME = "LIGHT"
        private const val DEFAULT_LANGUAGE = "CHINESE"
        private const val DEFAULT_SOUND_ENABLED = true
        private const val DEFAULT_CLIPBOARD_CLEAR_ENABLED = true
        private const val DEFAULT_CLIPBOARD_CLEAR_DELAY = 30
        private const val DEFAULT_AUTO_LOCK_DELAY_SECONDS = 30
    }
    
    var theme: String
        get() = prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
        set(value) = prefs.edit { putString(KEY_THEME, value) }
    
    var language: String
        get() = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value) }
    
    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
        set(value) = prefs.edit { putBoolean(KEY_SOUND_ENABLED, value) }
    
    var clipboardClearEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLIPBOARD_CLEAR_ENABLED, DEFAULT_CLIPBOARD_CLEAR_ENABLED)
        set(value) = prefs.edit { putBoolean(KEY_CLIPBOARD_CLEAR_ENABLED, value) }
    
    var clipboardClearDelay: Int
        get() = prefs.getInt(KEY_CLIPBOARD_CLEAR_DELAY, DEFAULT_CLIPBOARD_CLEAR_DELAY)
            .coerceIn(15, 300)
        set(value) = prefs.edit { putInt(KEY_CLIPBOARD_CLEAR_DELAY, value.coerceIn(15, 300)) }

    var autoLockDelaySeconds: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_DELAY_SECONDS, DEFAULT_AUTO_LOCK_DELAY_SECONDS)
            .coerceIn(0, 300)
        set(value) = prefs.edit { putInt(KEY_AUTO_LOCK_DELAY_SECONDS, value.coerceIn(0, 300)) }
    
    // ---- 主密码 ----
    
    val hasMasterPassword: Boolean
        get() = prefs.contains(KEY_MASTER_PASSWORD_VERIFIER) ||
            prefs.contains(KEY_MASTER_PASSWORD_HASH)
    
    fun setMasterPassword(password: String) {
        val record = MasterPasswordKdf.create(password)
        prefs.edit {
            putInt(KEY_MASTER_PASSWORD_KDF_VERSION, record.version)
            putInt(KEY_MASTER_PASSWORD_KDF_ITERATIONS, record.iterations)
            putString(KEY_MASTER_PASSWORD_SALT, record.saltBase64)
            putString(KEY_MASTER_PASSWORD_VERIFIER, record.verifierBase64)
            remove(KEY_MASTER_PASSWORD_HASH)
        }
    }
    
    fun verifyMasterPassword(password: String): Boolean {
        if (prefs.contains(KEY_MASTER_PASSWORD_VERIFIER)) {
            val record = readMasterPasswordRecord() ?: return false
            return MasterPasswordKdf.verify(password, record)
        }

        val legacyHash = prefs.getString(KEY_MASTER_PASSWORD_HASH, null) ?: return false
        val matchesLegacy = MessageDigest.isEqual(
            legacyHash.toByteArray(Charsets.US_ASCII),
            hashLegacyPassword(password).toByteArray(Charsets.US_ASCII)
        )
        if (matchesLegacy) {
            setMasterPassword(password)
        }
        return matchesLegacy
    }
    
    fun clearMasterPassword() {
        prefs.edit {
            remove(KEY_MASTER_PASSWORD_HASH)
            remove(KEY_MASTER_PASSWORD_KDF_VERSION)
            remove(KEY_MASTER_PASSWORD_KDF_ITERATIONS)
            remove(KEY_MASTER_PASSWORD_SALT)
            remove(KEY_MASTER_PASSWORD_VERIFIER)
        }
    }
    
    // ---- 指纹解锁 ----
    
    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_BIOMETRIC_ENABLED, value) }

    var syncSecurityMode: SyncSecurityMode
        get() = runCatching {
            SyncSecurityMode.valueOf(
                prefs.getString(KEY_SYNC_SECURITY_MODE, SyncSecurityMode.MAXIMUM_SECURITY.name)
                    ?: SyncSecurityMode.MAXIMUM_SECURITY.name
            )
        }.getOrDefault(SyncSecurityMode.MAXIMUM_SECURITY)
        set(value) = prefs.edit { putString(KEY_SYNC_SECURITY_MODE, value.name) }

    var objectPrefix: String
        get() = normalizePrefix(prefs.getString(KEY_OBJECT_PREFIX, "repasscard/") ?: "repasscard/")
        set(value) = prefs.edit { putString(KEY_OBJECT_PREFIX, normalizePrefix(value)) }

    var s3Endpoint: String
        get() = prefs.getString(KEY_S3_ENDPOINT, "") ?: ""
        set(value) = prefs.edit { putString(KEY_S3_ENDPOINT, value.trim()) }

    var s3Region: String
        get() = prefs.getString(KEY_S3_REGION, "") ?: ""
        set(value) = prefs.edit { putString(KEY_S3_REGION, value.trim()) }

    var s3Bucket: String
        get() = prefs.getString(KEY_S3_BUCKET, "") ?: ""
        set(value) = prefs.edit { putString(KEY_S3_BUCKET, value.trim()) }

    var s3AccessKey: String
        get() = getEncryptedString(
            encryptedKey = KEY_S3_ACCESS_KEY_ENCRYPTED,
            ivKey = KEY_S3_ACCESS_KEY_IV,
            legacyPlaintextKey = KEY_S3_ACCESS_KEY
        )
        set(value) = putEncryptedString(
            value = value.trim(),
            encryptedKey = KEY_S3_ACCESS_KEY_ENCRYPTED,
            ivKey = KEY_S3_ACCESS_KEY_IV,
            legacyPlaintextKey = KEY_S3_ACCESS_KEY
        )

    var s3SecretKey: String
        get() = getEncryptedString(
            encryptedKey = KEY_S3_SECRET_KEY_ENCRYPTED,
            ivKey = KEY_S3_SECRET_KEY_IV,
            legacyPlaintextKey = KEY_S3_SECRET_KEY
        )
        set(value) = putEncryptedString(
            value = value.trim(),
            encryptedKey = KEY_S3_SECRET_KEY_ENCRYPTED,
            ivKey = KEY_S3_SECRET_KEY_IV,
            legacyPlaintextKey = KEY_S3_SECRET_KEY
        )

    var s3SessionToken: String
        get() = getEncryptedString(
            encryptedKey = KEY_S3_SESSION_TOKEN_ENCRYPTED,
            ivKey = KEY_S3_SESSION_TOKEN_IV,
            legacyPlaintextKey = null
        )
        set(value) = putEncryptedString(
            value = value.trim(),
            encryptedKey = KEY_S3_SESSION_TOKEN_ENCRYPTED,
            ivKey = KEY_S3_SESSION_TOKEN_IV,
            legacyPlaintextKey = null
        )

    var vaultRevision: Long
        get() = prefs.getLong(KEY_VAULT_REVISION, 0L)
        set(value) = prefs.edit { putLong(KEY_VAULT_REVISION, value.coerceAtLeast(0L)) }

    var lastSyncedCloudVaultRevision: Long
        get() = prefs.getLong(KEY_LAST_SYNCED_CLOUD_VAULT_REVISION, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_SYNCED_CLOUD_VAULT_REVISION, value.coerceAtLeast(0L)) }

    var lastCloudUpdatedAt: Long
        get() = prefs.getLong(KEY_LAST_CLOUD_UPDATED_AT, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_CLOUD_UPDATED_AT, value.coerceAtLeast(0L)) }

    var lastCloudEtag: String
        get() = prefs.getString(KEY_LAST_CLOUD_ETAG, "") ?: ""
        set(value) = prefs.edit { putString(KEY_LAST_CLOUD_ETAG, value) }

    var lastLocalSyncAt: Long
        get() = prefs.getLong(KEY_LAST_LOCAL_SYNC_AT, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_LOCAL_SYNC_AT, value.coerceAtLeast(0L)) }

    @Synchronized
    fun getOrCreateInstallationId(): String {
        prefs.getString(KEY_INSTALLATION_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }
        return UUID.randomUUID().toString().also { generated ->
            prefs.edit { putString(KEY_INSTALLATION_ID, generated) }
        }
    }

    var randomPasswordSpec: RandomPasswordSpec
        get() = RandomPasswordSpec(
            length = prefs.getInt(KEY_RANDOM_PASSWORD_LENGTH, RandomPasswordSpec.DEFAULT_LENGTH),
            includeUppercase = prefs.getBoolean(KEY_RANDOM_PASSWORD_UPPERCASE, true),
            includeLowercase = prefs.getBoolean(KEY_RANDOM_PASSWORD_LOWERCASE, true),
            includeNumbers = prefs.getBoolean(KEY_RANDOM_PASSWORD_NUMBERS, true),
            includeSymbols = prefs.getBoolean(KEY_RANDOM_PASSWORD_SYMBOLS, true)
        ).normalized()
        set(value) {
            val normalized = value.normalized()
            prefs.edit {
                putInt(KEY_RANDOM_PASSWORD_LENGTH, normalized.length)
                putBoolean(KEY_RANDOM_PASSWORD_UPPERCASE, normalized.includeUppercase)
                putBoolean(KEY_RANDOM_PASSWORD_LOWERCASE, normalized.includeLowercase)
                putBoolean(KEY_RANDOM_PASSWORD_NUMBERS, normalized.includeNumbers)
                putBoolean(KEY_RANDOM_PASSWORD_SYMBOLS, normalized.includeSymbols)
            }
        }

    fun nextVaultRevision(): Long {
        val next = vaultRevision + 1L
        vaultRevision = next
        return next
    }
    
    // ---- 工具 ----
    
    fun clear() {
        prefs.edit { clear() }
    }
    
    private fun readMasterPasswordRecord(): MasterPasswordRecord? {
        val salt = prefs.getString(KEY_MASTER_PASSWORD_SALT, null) ?: return null
        val verifier = prefs.getString(KEY_MASTER_PASSWORD_VERIFIER, null) ?: return null
        return MasterPasswordRecord(
            version = prefs.getInt(KEY_MASTER_PASSWORD_KDF_VERSION, -1),
            iterations = prefs.getInt(KEY_MASTER_PASSWORD_KDF_ITERATIONS, -1),
            saltBase64 = salt,
            verifierBase64 = verifier
        )
    }

    private fun hashLegacyPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun normalizePrefix(value: String): String {
        val trimmed = value.trim().trimStart('/')
        if (trimmed.isBlank()) return "repasscard/"
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private fun getEncryptedString(
        encryptedKey: String,
        ivKey: String,
        legacyPlaintextKey: String?
    ): String {
        val encrypted = prefs.getString(encryptedKey, null)
        val iv = prefs.getString(ivKey, null)
        if (!encrypted.isNullOrBlank() && !iv.isNullOrBlank()) {
            return runCatching { KeystoreStringCipher.decrypt(encrypted, iv) }.getOrDefault("")
        }

        val legacy = legacyPlaintextKey?.let { prefs.getString(it, null) }.orEmpty()
        if (legacy.isNotBlank()) {
            putEncryptedString(legacy, encryptedKey, ivKey, legacyPlaintextKey)
        }
        return legacy
    }

    private fun putEncryptedString(
        value: String,
        encryptedKey: String,
        ivKey: String,
        legacyPlaintextKey: String?
    ) {
        if (value.isBlank()) {
            prefs.edit {
                remove(encryptedKey)
                remove(ivKey)
                if (legacyPlaintextKey != null) remove(legacyPlaintextKey)
            }
            return
        }

        val encrypted = KeystoreStringCipher.encrypt(value)
        prefs.edit {
            putString(encryptedKey, encrypted.ciphertext)
            putString(ivKey, encrypted.iv)
            if (legacyPlaintextKey != null) remove(legacyPlaintextKey)
        }
    }
}

enum class SyncSecurityMode {
    MAXIMUM_SECURITY,
    CONVENIENCE
}
