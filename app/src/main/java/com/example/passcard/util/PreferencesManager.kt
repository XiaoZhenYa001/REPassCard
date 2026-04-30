package com.example.passcard.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.security.MessageDigest

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
        private const val KEY_MASTER_PASSWORD_HASH = "master_password_hash"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        
        // Default Values
        private const val DEFAULT_THEME = "LIGHT"
        private const val DEFAULT_LANGUAGE = "CHINESE"
        private const val DEFAULT_SOUND_ENABLED = true
        private const val DEFAULT_CLIPBOARD_CLEAR_ENABLED = false
        private const val DEFAULT_CLIPBOARD_CLEAR_DELAY = 30
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
        set(value) = prefs.edit { putInt(KEY_CLIPBOARD_CLEAR_DELAY, value) }
    
    // ---- 主密码 ----
    
    /** 是否已设置主密码 */
    val hasMasterPassword: Boolean
        get() = prefs.getString(KEY_MASTER_PASSWORD_HASH, null) != null
    
    /** 设置主密码（存储 SHA-256 哈希） */
    fun setMasterPassword(password: String) {
        prefs.edit { putString(KEY_MASTER_PASSWORD_HASH, hashPassword(password)) }
    }
    
    /** 验证主密码 */
    fun verifyMasterPassword(password: String): Boolean {
        val storedHash = prefs.getString(KEY_MASTER_PASSWORD_HASH, null) ?: return false
        return hashPassword(password) == storedHash
    }
    
    /** 清除主密码 */
    fun clearMasterPassword() {
        prefs.edit { remove(KEY_MASTER_PASSWORD_HASH) }
    }
    
    // ---- 指纹解锁 ----
    
    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_BIOMETRIC_ENABLED, value) }
    
    // ---- 工具 ----
    
    fun clear() {
        prefs.edit { clear() }
    }
    
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
