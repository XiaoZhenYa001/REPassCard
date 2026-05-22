package com.example.passcard.sync

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 从云端覆盖本地前自动备份；最多保留 10 份（设计文档）。
 */
object LocalVaultBackupStorage {
    private fun dir(context: Context) = File(context.filesDir, "local_backups").apply { mkdirs() }

    fun saveEncryptedVaultFile(context: Context, encryptedJson: String): Result<String> = runCatching {
        val df = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val name = "${df.format(Date())}.enc"
        val f = File(dir(context), name)
        f.writeText(encryptedJson, Charsets.UTF_8)
        prune(context, maxKeep = 10)
        f.name
    }

    private fun prune(context: Context, maxKeep: Int) {
        val d = dir(context)
        val files = d.listFiles()?.filter { it.isFile && it.name.endsWith(".enc") }?.sortedBy { it.name } ?: return
        if (files.size <= maxKeep) return
        files.take(files.size - maxKeep).forEach { it.delete() }
    }
}
