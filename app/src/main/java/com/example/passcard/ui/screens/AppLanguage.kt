package com.example.passcard.ui.screens

import androidx.compose.runtime.*

/**
 * 应用语言
 */
enum class AppLanguage(val displayName: String, val code: String) {
    CHINESE("中文", "zh"),
    ENGLISH("English", "en")
}

/**
 * 获取当前语言
 */
@Composable
fun rememberAppLanguage(preferencesManager: com.example.passcard.util.PreferencesManager?): AppLanguage {
    val savedLanguage = preferencesManager?.language ?: "CHINESE"
    return remember(savedLanguage) {
        when (savedLanguage) {
            "ENGLISH" -> AppLanguage.ENGLISH
            else -> AppLanguage.CHINESE
        }
    }
}

/**
 * 字符串资源 - 根据语言返回对应文本
 */
object AppStrings {
    @Composable
    fun welcomeBack(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "欢迎回来"
        AppLanguage.ENGLISH -> "Welcome Back"
    }

    @Composable
    fun searchPasswords(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "搜索密码..."
        AppLanguage.ENGLISH -> "Search passwords..."
    }

    @Composable
    fun passwordCount(count: Int, language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "$count 个密码"
        AppLanguage.ENGLISH -> "$count Passwords"
    }

    @Composable
    fun securityScore(score: Int, language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "$score% 安全"
        AppLanguage.ENGLISH -> "$score% Secure"
    }

    @Composable
    fun recentLogins(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "最近登录"
        AppLanguage.ENGLISH -> "Recent Logins"
    }

    @Composable
    fun viewAll(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "查看全部"
        AppLanguage.ENGLISH -> "View All"
    }

    @Composable
    fun settings(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "设置"
        AppLanguage.ENGLISH -> "Settings"
    }

    @Composable
    fun account(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "账户"
        AppLanguage.ENGLISH -> "Account"
    }

    @Composable
    fun masterPassword(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "主密码"
        AppLanguage.ENGLISH -> "Master Password"
    }

    @Composable
    fun appSettings(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "应用设置"
        AppLanguage.ENGLISH -> "App Settings"
    }

    @Composable
    fun theme(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "主题外观"
        AppLanguage.ENGLISH -> "Theme"
    }

    @Composable
    fun languageLabel(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "语言"
        AppLanguage.ENGLISH -> "Language"
    }

    @Composable
    fun soundFeedback(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "声音反馈"
        AppLanguage.ENGLISH -> "Sound Feedback"
    }

    @Composable
    fun dataManagement(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "数据管理"
        AppLanguage.ENGLISH -> "Data Management"
    }

    @Composable
    fun exportPasswords(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "导出密码"
        AppLanguage.ENGLISH -> "Export Passwords"
    }

    @Composable
    fun importPasswords(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "导入密码"
        AppLanguage.ENGLISH -> "Import Passwords"
    }

    @Composable
    fun more(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "更多"
        AppLanguage.ENGLISH -> "More"
    }

    @Composable
    fun help(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "使用帮助"
        AppLanguage.ENGLISH -> "Help"
    }

    @Composable
    fun privacyPolicy(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "隐私条款"
        AppLanguage.ENGLISH -> "Privacy Policy"
    }

    @Composable
    fun aboutUs(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "关于我们"
        AppLanguage.ENGLISH -> "About Us"
    }

    @Composable
    fun securityCenter(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "安全中心"
        AppLanguage.ENGLISH -> "Security Center"
    }

    @Composable
    fun attentionNeeded(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "需要注意"
        AppLanguage.ENGLISH -> "Attention Needed"
    }

    @Composable
    fun securitySuggestions(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "安全建议"
        AppLanguage.ENGLISH -> "Security Suggestions"
    }

    @Composable
    fun compromisedPasswords(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "泄露密码"
        AppLanguage.ENGLISH -> "Compromised"
    }

    @Composable
    fun weakPasswords(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "弱密码"
        AppLanguage.ENGLISH -> "Weak Passwords"
    }

    @Composable
    fun enable2FA(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "启用两步验证"
        AppLanguage.ENGLISH -> "Enable 2FA"
    }

    @Composable
    fun addLogin(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "添加登录"
        AppLanguage.ENGLISH -> "Add Login"
    }

    @Composable
    fun editLogin(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "编辑登录"
        AppLanguage.ENGLISH -> "Edit Login"
    }

    @Composable
    fun save(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "保存"
        AppLanguage.ENGLISH -> "Save"
    }

    @Composable
    fun deletePassword(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "删除密码"
        AppLanguage.ENGLISH -> "Delete Password"
    }

    @Composable
    fun name(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "名称"
        AppLanguage.ENGLISH -> "Name"
    }

    @Composable
    fun username(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "用户名"
        AppLanguage.ENGLISH -> "Username"
    }

    @Composable
    fun phone(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "手机号"
        AppLanguage.ENGLISH -> "Phone"
    }

    @Composable
    fun email(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "邮箱"
        AppLanguage.ENGLISH -> "Email"
    }

    @Composable
    fun passwordLabel(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "密码"
        AppLanguage.ENGLISH -> "Password"
    }

    @Composable
    fun category(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "分类"
        AppLanguage.ENGLISH -> "Category"
    }

    @Composable
    fun note(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "备注"
        AppLanguage.ENGLISH -> "Note"
    }

    @Composable
    fun changeIcon(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "更换图标"
        AppLanguage.ENGLISH -> "Change Icon"
    }

    @Composable
    fun allPasswords(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "所有密码"
        AppLanguage.ENGLISH -> "All Passwords"
    }

    @Composable
    fun itemsCount(count: Int, language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "$count 项"
        AppLanguage.ENGLISH -> "$count items"
    }

    @Composable
    fun passwordCopied(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "密码已复制"
        AppLanguage.ENGLISH -> "Password copied"
    }

    @Composable
    fun lightTheme(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "浅色"
        AppLanguage.ENGLISH -> "Light"
    }

    @Composable
    fun darkTheme(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "深色"
        AppLanguage.ENGLISH -> "Dark"
    }

    @Composable
    fun systemTheme(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "跟随系统"
        AppLanguage.ENGLISH -> "System"
    }

    @Composable
    fun home(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "首页"
        AppLanguage.ENGLISH -> "Home"
    }

    @Composable
    fun security(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "安全"
        AppLanguage.ENGLISH -> "Security"
    }

    @Composable
    fun totalPasswords(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "密码总数"
        AppLanguage.ENGLISH -> "Total Passwords"
    }

    @Composable
    fun reused(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "重复使用"
        AppLanguage.ENGLISH -> "Reused"
    }

    @Composable
    fun compromisedDesc(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "个账户在数据泄露中发现"
        AppLanguage.ENGLISH -> "accounts found in breaches"
    }

    @Composable
    fun weakPasswordsDesc(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "个账户需要更强的密码"
        AppLanguage.ENGLISH -> "accounts need stronger passwords"
    }

    @Composable
    fun enable2FADesc(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "为您的主密码库账户添加额外的安全保护。"
        AppLanguage.ENGLISH -> "Add extra security to your vault account."
    }

    @Composable
    fun securityScoreLabel(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "安全评分"
        AppLanguage.ENGLISH -> "Security Score"
    }

    @Composable
    fun securityScoreDesc(language: AppLanguage = AppLanguage.CHINESE): String = when (language) {
        AppLanguage.CHINESE -> "您的密码健康状况良好，但有几项需要修复。"
        AppLanguage.ENGLISH -> "Your password health is good, but some items need attention."
    }
}

/**
 * 获取分类列表（根据语言）
 */
fun getCategories(language: AppLanguage): List<String> {
    return when (language) {
        AppLanguage.CHINESE -> listOf(
            "全部", "社交媒体", "工作", "金融", "购物", "娱乐", "AI", "游戏", "教育", "其他"
        )
        AppLanguage.ENGLISH -> listOf(
            "All", "Social Media", "Work", "Finance", "Shopping", "Entertainment", "AI", "Gaming", "Education", "Other"
        )
    }
}

/**
 * 获取常用分类（用于首页显示）
 */
fun getCommonCategories(language: AppLanguage): List<String> {
    return when (language) {
        AppLanguage.CHINESE -> listOf("全部", "社交媒体", "工作", "金融", "购物", "娱乐", "AI")
        AppLanguage.ENGLISH -> listOf("All", "Social Media", "Work", "Finance", "Shopping", "Entertainment", "AI")
    }
}
