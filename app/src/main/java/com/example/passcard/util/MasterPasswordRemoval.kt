package com.example.passcard.util

/** The mutation is reachable only after the current password has been verified. */
internal object MasterPasswordRemoval {
    fun remove(
        currentPassword: String,
        verify: (String) -> Boolean,
        clearProtection: () -> Unit
    ): Boolean {
        if (currentPassword.isEmpty() || !verify(currentPassword)) return false
        clearProtection()
        return true
    }
}
