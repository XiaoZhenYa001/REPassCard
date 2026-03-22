package com.example.passcard.ui.screens

import androidx.compose.runtime.*

/**
 * 应用语言
 */
enum class AppLanguage(val displayName: String) {
    CHINESE("中文"),
    ENGLISH("English")
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
