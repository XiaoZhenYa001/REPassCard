package com.example.passcard.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

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
        
        // Default Values
        private const val DEFAULT_THEME = "LIGHT"
        private const val DEFAULT_LANGUAGE = "CHINESE"
        private const val DEFAULT_SOUND_ENABLED = true
    }
    
    /**
     * 获取当前主题
     */
    var theme: String
        get() = prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
        set(value) = prefs.edit { putString(KEY_THEME, value) }
    
    /**
     * 获取当前语言
     */
    var language: String
        get() = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value) }
    
    /**
     * 获取声音反馈设置
     */
    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
        set(value) = prefs.edit { putBoolean(KEY_SOUND_ENABLED, value) }
    
    /**
     * 清除所有设置
     */
    fun clear() {
        prefs.edit { clear() }
    }
}
