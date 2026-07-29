package com.example.passcard.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream

object ExportStorage {
    private const val EXPORT_DIRECTORY = "PassCard"
    private const val MEDIASTORE_EXPORT_PATH = "Documents/$EXPORT_DIRECTORY/"

    fun createExportFile(
        context: Context,
        fileName: String,
        mimeType: String
    ): Result<ExportFile> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                createMediaStoreFile(context, fileName, mimeType)
            } else {
                createLegacyExternalFile(context, fileName, mimeType)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createMediaStoreFile(
        context: Context,
        fileName: String,
        mimeType: String
    ): Result<ExportFile> {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, MEDIASTORE_EXPORT_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: return Result.failure(IllegalStateException("Unable to create export file"))

        val stream = resolver.openOutputStream(uri)
        if (stream == null) {
            resolver.delete(uri, null, null)
            return Result.failure(IllegalStateException("Unable to open export file"))
        }

        return Result.success(
            ExportFile(
                uri = uri,
                outputStream = stream,
                onSuccess = {
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }.also { completedValues ->
                        resolver.update(uri, completedValues, null, null)
                    }
                },
                onFailure = {
                    resolver.delete(uri, null, null)
                }
            )
        )
    }

    private fun createLegacyExternalFile(
        context: Context,
        fileName: String,
        mimeType: String
    ): Result<ExportFile> {
        val exportDir = File(Environment.getExternalStorageDirectory(), EXPORT_DIRECTORY)
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            return Result.failure(IllegalStateException("Unable to create ${exportDir.absolutePath}"))
        }

        val file = File(exportDir, fileName)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Result.success(
            ExportFile(
                uri = uri,
                outputStream = file.outputStream(),
                mimeType = mimeType,
                onFailure = { file.delete() }
            )
        )
    }
}

data class ExportFile(
    val uri: Uri,
    val outputStream: OutputStream,
    val mimeType: String? = null,
    val onSuccess: () -> Unit = {},
    val onFailure: () -> Unit = {}
)
