package com.example.passcard.data

data class PasswordSecurityStats(
    val totalCount: Int = 0,
    val weakCount: Int = 0,
    val reusedCount: Int = 0,
    val compromisedCount: Int = 0
) {
    val score: Int
        get() = (100 - weakCount * 12 - reusedCount * 8 - compromisedCount * 20).coerceIn(0, 100)
}
