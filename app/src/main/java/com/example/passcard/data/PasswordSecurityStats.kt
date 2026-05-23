package com.example.passcard.data

data class PasswordSecurityStats(
    val totalCount: Int = 0,
    val weakCount: Int = 0,
    val reusedCount: Int = 0,
    val compromisedCount: Int = 0
) {
    val score: Int
        get() {
            if (totalCount <= 0) return 0
            val weakPenalty = weakCount.toFloat() / totalCount.toFloat() * 55f
            val reusedPenalty = reusedCount.toFloat() / totalCount.toFloat() * 35f
            val compromisedPenalty = compromisedCount.toFloat() / totalCount.toFloat() * 20f
            return (100f - weakPenalty - reusedPenalty - compromisedPenalty).toInt().coerceIn(0, 100)
        }
}
