package com.example.passcard.ui.screens

import androidx.compose.runtime.Immutable

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
    val note: String = "",
    val iconType: String = "generated",
    val iconValue: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val revision: Long = 0L,
    val deviceId: String = "",
    val deletedAt: Long? = null
)

@Immutable
data class PasswordListSnapshot(
    val items: List<PasswordItem>
)
