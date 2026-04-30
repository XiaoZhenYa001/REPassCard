package com.example.passcard.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.passcard.util.PreferencesManager

/**
 * 剪贴板工具类
 * 支持自动清除功能（仅清除本次复制的密码内容）
 */
object ClipboardHelper {
    
    private var clearHandler: Handler? = null
    private var clearRunnable: Runnable? = null
    private var lastCopiedText: String? = null
    
    /**
     * 复制文本到剪贴板
     * 如果用户启用了自动清除，会在设定的延迟后自动清空（仅当剪贴板内容未被替换时）
     */
    fun copyToClipboard(
        context: Context,
        text: String,
        label: String = "Password",
        showToast: Boolean = true
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        lastCopiedText = text
        
        if (showToast) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 自动清除逻辑
        scheduleClipboardClear(context)
    }
    
    /**
     * 根据用户偏好设置调度剪贴板清除
     */
    private fun scheduleClipboardClear(context: Context) {
        // 取消之前的清除任务
        cancelPendingClear()
        
        try {
            val prefs = PreferencesManager(context)
            if (!prefs.clipboardClearEnabled) return
            
            val delayMs = prefs.clipboardClearDelay * 1000L
            
            val handler = Handler(Looper.getMainLooper())
            val runnable = Runnable {
                clearClipboardIfMatch(context)
            }
            
            handler.postDelayed(runnable, delayMs)
            clearHandler = handler
            clearRunnable = runnable
        } catch (_: Exception) {
            // 静默处理
        }
    }
    
    /**
     * 取消待执行的清除任务
     */
    private fun cancelPendingClear() {
        clearRunnable?.let { runnable ->
            clearHandler?.removeCallbacks(runnable)
        }
        clearHandler = null
        clearRunnable = null
    }
    
    /**
     * 仅当剪贴板内容仍是本次复制的密码时才清除
     * 如果用户在此期间复制了其他内容，则不会被清除
     */
    private fun clearClipboardIfMatch(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val currentClip = clipboard.primaryClip
            if (currentClip != null && currentClip.itemCount > 0) {
                val currentText = currentClip.getItemAt(0).text?.toString()
                if (currentText == lastCopiedText) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                    lastCopiedText = null
                }
            }
        } catch (_: Exception) {
            // 静默处理
        }
    }
}
