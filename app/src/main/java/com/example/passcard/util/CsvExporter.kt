package com.example.passcard.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.OutputStreamWriter
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    fun exportToCsv(
        context: Context,
        passwords: List<ExportPasswordEntry>,
        fileName: String = "passwords_export"
    ): Result<Uri> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val csvFileName = "${fileName}_$timestamp.csv"
            val exportFile = ExportStorage.createExportFile(
                context = context,
                fileName = csvFileName,
                mimeType = "text/csv"
            ).getOrThrow()

            try {
                OutputStreamWriter(exportFile.outputStream, Charsets.UTF_8).use { writer ->
                    writeCsv(writer, passwords)
                }
                exportFile.onSuccess()
                Result.success(exportFile.uri)
            } catch (e: Exception) {
                exportFile.onFailure()
                throw e
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun writeCsv(writer: Writer, passwords: List<ExportPasswordEntry>) {
        writer.write("\uFEFF")
        writer.write("服务,用户名,手机号,邮箱,密码,备注,分类,icon_type,icon_value\n")
        passwords.forEach { entry ->
            writer.write(listOf(entry.service, entry.username, entry.phone, entry.email,
                entry.password, entry.note, entry.category, entry.iconType, entry.iconValue)
                .joinToString(",") { escapeCsvField(it) })
            writer.write("\n")
        }
    }

    fun createShareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Export Passwords")
            putExtra(Intent.EXTRA_TEXT, "REPassCard Password Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun escapeCsvField(field: String): String {
        return when {
            field.isEmpty() -> ""
            field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r") -> {
                "\"${field.replace("\"", "\"\"")}\""
            }
            else -> field
        }
    }
}

data class ExportPasswordEntry(
    val service: String,
    val username: String,
    val phone: String = "",
    val email: String = "",
    val password: String,
    val note: String = "",
    val category: String = "",
    val iconType: String = "generated",
    val iconValue: String = ""
)
