package com.example.passcard.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JsonExporter {
    fun exportToJson(
        context: Context,
        passwords: List<ExportPasswordEntry>,
        fileName: String = "passwords_export"
    ): Result<Uri> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val jsonFileName = "${fileName}_$timestamp.json"
            val exportFile = ExportStorage.createExportFile(
                context = context,
                fileName = jsonFileName,
                mimeType = "application/json"
            ).getOrThrow()

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

            try {
                OutputStreamWriter(exportFile.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(root.toString(2))
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
