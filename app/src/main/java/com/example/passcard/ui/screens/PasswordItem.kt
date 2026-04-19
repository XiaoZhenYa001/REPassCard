package com.example.passcard.ui.screens

/**
 * 密码列表数据模型
 */
data class PasswordItem(
    val id: String,
    val name: String,
    val username: String,
    val phone: String = "",
    val email: String = "",
    val password: String,
    val category: String = "",
    val note: String = ""
)
