package com.example.passcard.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * 剪贴板工具类
 */
object ClipboardHelper {
    
    /**
     * 复制文本到剪贴板
     * @param context 上下文
     * @param text 要复制的文本
     * @param label 剪贴板标签
     * @param showToast 是否显示Toast提示
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
        
        if (showToast) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
