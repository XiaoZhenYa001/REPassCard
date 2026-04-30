package com.example.passcard.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * JSON 导出工具
 * 导出格式包含元信息和密码数组
 */
object JsonExporter {

    /**
     * 将密码条目导出为 JSON 文件
     */
    fun exportToJson(
        context: Context,
        passwords: List<ExportPasswordEntry>,
        fileName: String = "passwords_export"
    ): Result<Uri> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val jsonFileName = "${fileName}_$timestamp.json"

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val file = File(exportDir, jsonFileName)

            val root = JSONObject().apply {
                put("app", "REPassCard")
                put("version", 1)
                put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(Date()))
                put("count", passwords.size)

                val passwordsArray = JSONArray()
                passwords.forEach { entry ->
                    val obj = JSONObject().apply {
                        put("service", entry.service)
                        put("username", entry.username)
                        put("phone", entry.phone)
                        put("email", entry.email)
                        put("password", entry.password)
                        put("note", entry.note)
                        put("category", entry.category)
                    }
                    passwordsArray.put(obj)
                }
                put("passwords", passwordsArray)
            }

            FileWriter(file).use { writer ->
                writer.write(root.toString(2))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 创建分享 Intent
     */
    fun createShareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Export Passwords")
            putExtra(Intent.EXTRA_TEXT, "REPassCard Password Export (JSON)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
