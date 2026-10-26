package com.example.passcard.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.SystemClock
import android.widget.Toast
import java.lang.ref.WeakReference
import java.util.UUID

object ClipboardHelper {
    private const val COPY_TOKEN_KEY = "com.example.passcard.CLIPBOARD_TOKEN"

    private val clearHandler = Handler(Looper.getMainLooper())
    private val clearSession = ClipboardClearSession()
    private var clearRunnable: Runnable? = null
    private var hasWindowFocus = false

    fun copyToClipboard(
        context: Context,
        text: String,
        label: String = "Password",
        showToast: Boolean = true
    ) {
        val appContext = context.applicationContext
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val copyToken = UUID.randomUUID().toString()
        val clip = ClipData.newPlainText(label, text).apply {
            description.extras = PersistableBundle().apply {
                putString(COPY_TOKEN_KEY, copyToken)
                // This key also works on API 31–32; the constant was introduced in API 33.
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        clipboard.setPrimaryClip(clip)

        if (showToast) {
            Toast.makeText(appContext, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        cancelTimer()
        clearSession.cancel()
        runCatching {
            val preferences = PreferencesManager(appContext)
            if (preferences.clipboardClearEnabled) {
                clearSession.schedule(
                    token = copyToken,
                    copiedAtMillis = SystemClock.elapsedRealtime(),
                    delayMillis = preferences.clipboardClearDelay * 1_000L
                )
                scheduleTimer(appContext)
            }
        }
    }

    /** Clipboard ownership can only be checked while a window has input focus. */
    fun onWindowFocusChanged(context: Context, hasFocus: Boolean) {
        hasWindowFocus = hasFocus
        cancelTimer()
        if (hasFocus) {
            clearIfDue(context.applicationContext)
            scheduleTimer(context.applicationContext)
        }
    }

    fun onAppBackgrounded() {
        hasWindowFocus = false
        cancelTimer()
        // Keep only the token and deadline so a foreground return can finish cleanup.
    }

    private fun scheduleTimer(context: Context) {
        if (!hasWindowFocus) return
        val delayMillis = clearSession.remainingDelayMillis(SystemClock.elapsedRealtime()) ?: return
        // If access was unavailable at the deadline, retry on the next focus event.
        if (delayMillis == 0L) return
        val weakContext = WeakReference(context)
        val runnable = Runnable {
            clearRunnable = null
            weakContext.get()?.let(::clearIfDue)
        }
        clearRunnable = runnable
        clearHandler.postDelayed(runnable, delayMillis)
    }

    private fun cancelTimer() {
        clearRunnable?.let(clearHandler::removeCallbacks)
        clearRunnable = null
    }

    private fun clearIfDue(context: Context) {
        runCatching {
            if (!PreferencesManager(context).clipboardClearEnabled) {
                clearSession.cancel()
                cancelTimer()
                return@runCatching
            }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clearSession.clearIfDue(
                nowMillis = SystemClock.elapsedRealtime(),
                hasFocus = hasWindowFocus,
                readCurrentToken = {
                    val description = clipboard.primaryClipDescription
                    // An accessible replacement clip without our marker must be preserved.
                    description?.let { it.extras?.getString(COPY_TOKEN_KEY).orEmpty() }
                },
                clearClipboard = clipboard::clearPrimaryClip
            )
        }
        // Rejected reads/clears retain the pending token for the next focus event.
    }
}
