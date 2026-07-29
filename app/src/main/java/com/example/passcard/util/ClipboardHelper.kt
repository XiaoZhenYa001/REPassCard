package com.example.passcard.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.widget.Toast
import java.util.UUID

object ClipboardHelper {
    private const val COPY_TOKEN_KEY = "com.example.passcard.CLIPBOARD_TOKEN"

    private val clearHandler = Handler(Looper.getMainLooper())
    private var clearRunnable: Runnable? = null

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        }
        clipboard.setPrimaryClip(clip)

        if (showToast) {
            clearHandler.post {
                Toast.makeText(appContext, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        scheduleClipboardClear(appContext, copyToken)
    }

    private fun scheduleClipboardClear(context: Context, copyToken: String) {
        cancelPendingClear()

        runCatching {
            val preferences = PreferencesManager(context)
            if (!preferences.clipboardClearEnabled) return

            val runnable = Runnable {
                clearClipboardIfMatch(context, copyToken)
            }
            clearRunnable = runnable
            clearHandler.postDelayed(runnable, preferences.clipboardClearDelay * 1_000L)
        }.onFailure {
            cancelPendingClear()
        }
    }

    private fun cancelPendingClear() {
        clearRunnable?.let(clearHandler::removeCallbacks)
        clearRunnable = null
    }

    private fun clearClipboardIfMatch(context: Context, expectedToken: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val currentToken = clipboard.primaryClip
                ?.description
                ?.extras
                ?.getString(COPY_TOKEN_KEY)
            if (currentToken == expectedToken) {
                clipboard.clearPrimaryClip()
            }
        } catch (_: Exception) {
            // Clipboard access can be rejected while the app is backgrounded.
        } finally {
            clearRunnable = null
        }
    }
}
