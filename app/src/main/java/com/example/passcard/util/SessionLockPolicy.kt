package com.example.passcard.util

object SessionLockPolicy {
    fun shouldLock(
        hasMasterPassword: Boolean,
        backgroundedAtMillis: Long?,
        resumedAtMillis: Long,
        timeoutSeconds: Int
    ): Boolean {
        if (!hasMasterPassword || backgroundedAtMillis == null) return false

        val timeoutMillis = timeoutSeconds.coerceIn(0, 300) * 1_000L
        val elapsedMillis = (resumedAtMillis - backgroundedAtMillis).coerceAtLeast(0L)
        return elapsedMillis >= timeoutMillis
    }
}
