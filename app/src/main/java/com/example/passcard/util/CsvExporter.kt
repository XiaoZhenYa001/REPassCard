package com.example.passcard.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * CSV 导出工具
 */
object CsvExporter {
    
    /**
     * 将密码条目导出为 CSV 文件
     * 格式: 服务,用户名,手机号,邮箱,密码,备注,分类
     */
    fun exportToCsv(
        context: Context,
        passwords: List<ExportPasswordEntry>,
        fileName: String = "passwords_export"
    ): Result<Uri> {
        return try {
            // 生成带时间戳的文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val csvFileName = "${fileName}_$timestamp.csv"
            
            // 创建导出目录
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            // 创建文件
            val file = File(exportDir, csvFileName)
            
            // 写入 CSV
            FileWriter(file).use { writer ->
                // 写入 BOM (UTF-8)
                writer.write("\uFEFF")
                
                // 写入标题行（包含分类字段）
                writer.write("服务,用户名,手机号,邮箱,密码,备注,分类\n")
                
                // 写入数据行
                passwords.forEach { entry ->
                    val line = buildString {
                        append(escapeCsvField(entry.service))
                        append(",")
                        append(escapeCsvField(entry.username))
                        append(",")
                        append(escapeCsvField(entry.phone))
                        append(",")
                        append(escapeCsvField(entry.email))
                        append(",")
                        append(escapeCsvField(entry.password))
                        append(",")
                        append(escapeCsvField(entry.note))
                        append(",")
                        append(escapeCsvField(entry.category))
                    }
                    writer.write("$line\n")
                }
            }
            
            // 获取文件 URI
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
    fun createShareIntent(uri: Uri, fileName: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Export Passwords")
            putExtra(Intent.EXTRA_TEXT, "REPassCard Password Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * CSV 字段转义
     * 处理包含逗号、引号、换行符的字段
     */
    private fun escapeCsvField(field: String): String {
        return when {
            field.isEmpty() -> ""
            field.contains(",") || field.contains("\"") || field.contains("\n") -> {
                "\"${field.replace("\"", "\"\"")}\""
            }
            else -> field
        }
    }
}

/**
 * 导出用的密码条目
 */
data class ExportPasswordEntry(
    val service: String,
    val username: String,
    val phone: String = "",
    val email: String = "",
    val password: String,
    val note: String = "",
    val category: String = ""
)
