package com.example.passcard.util

/** Retains only ownership and an elapsed-time deadline, never copied content. */
internal class ClipboardClearSession {
    private data class Pending(val token: String, val clearAtMillis: Long)
    private var pending: Pending? = null

    fun schedule(token: String, copiedAtMillis: Long, delayMillis: Long) {
        pending = Pending(token, copiedAtMillis + delayMillis.coerceAtLeast(0))
    }

    fun cancel() {
        pending = null
    }

    fun remainingDelayMillis(nowMillis: Long): Long? =
        pending?.let { (it.clearAtMillis - nowMillis).coerceAtLeast(0) }

    fun clearIfDue(
        nowMillis: Long,
        hasFocus: Boolean,
        readCurrentToken: () -> String?,
        clearClipboard: () -> Unit
    ): Boolean {
        val expected = pending ?: return false
        if (!hasFocus || nowMillis < expected.clearAtMillis) return false
        // A null result can mean access was restricted. Preserve the pending cleanup.
        val observedToken = readCurrentToken() ?: return false
        // A newer in-process copy may have superseded this attempt while reading.
        if (pending != expected) return false
        if (observedToken != expected.token) {
            if (pending == expected) pending = null
            return false
        }
        clearClipboard()
        if (pending == expected) pending = null
        return true
    }
}
