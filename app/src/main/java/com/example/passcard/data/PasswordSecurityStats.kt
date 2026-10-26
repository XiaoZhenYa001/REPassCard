package com.example.passcard.data

data class PasswordSecurityStats(
    val totalCount: Int = 0,
    val weakCount: Int = 0,
    val reusedCount: Int = 0,
    val compromisedCount: Int = 0,
    val passwordlessCount: Int = 0
) {
    val assessedCount: Int get() = (totalCount - passwordlessCount).coerceAtLeast(0)
    val hasScore: Boolean get() = assessedCount > 0

    val score: Int
        get() {
            if (!hasScore) return 0
            val weakPenalty = weakCount.toFloat() / assessedCount.toFloat() * 55f
            val reusedPenalty = reusedCount.toFloat() / assessedCount.toFloat() * 35f
            val compromisedPenalty = compromisedCount.toFloat() / assessedCount.toFloat() * 20f
            return (100f - weakPenalty - reusedPenalty - compromisedPenalty).toInt().coerceIn(0, 100)
        }
}
