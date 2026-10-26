package com.example.passcard.crypto

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.InputStream

/** Reads only PassCard's own icon library; no arbitrary private-file or remote URI access. */
class AndroidVaultIconStorage(context: Context) : VaultIconStorage {
    private val context = context.applicationContext
    private val privateIcons = VaultIconFiles(File(this.context.filesDir, "vault-icons"))

    override fun openLocalIcon(reference: String): InputStream? {
        if (reference.isBlank()) return null
        privateIcons.openOwnedReference(reference)?.let { return it }
        val uri = Uri.parse(reference)
        return when (uri.scheme) {
            "file" -> openLegacyLibraryIcon(uri)
            "content" -> openMediaLibraryIcon(uri)
            else -> null
        }
    }

    override fun storeRestoredIcon(bytes: ByteArray): String = Uri.fromFile(privateIcons.store(bytes)).toString()

    @Suppress("DEPRECATION")
    private fun openLegacyLibraryIcon(uri: Uri): InputStream? {
        if (!uri.authority.isNullOrEmpty()) return null
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PassCard/images")
            .canonicalFile
        val file = File(uri.path.orEmpty()).canonicalFile
        if (file.parentFile != directory || !isImageFileName(file.name)) return null
        return if (file.isFile) file.inputStream() else null
    }

    private fun openMediaLibraryIcon(uri: Uri): InputStream? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || uri.authority != MediaStore.AUTHORITY) return null
        val segments = uri.pathSegments
        if (segments.size != 3 || segments[0] !in setOf("external", "external_primary") ||
            segments[1] != "file" || segments[2].toLongOrNull() == null) return null
        val projection = arrayOf(MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME)
        val owned = context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            cursor.moveToFirst() && cursor.getString(0) == "Documents/PassCard/images/" &&
                isImageFileName(cursor.getString(1).orEmpty())
        } ?: false
        return if (owned) context.contentResolver.openInputStream(uri) else null
    }

    private fun isImageFileName(name: String): Boolean = !name.startsWith(".") &&
        name.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT) in
        setOf("jpg", "jpeg", "png", "webp", "bmp", "heic", "heif", "svg")
}
