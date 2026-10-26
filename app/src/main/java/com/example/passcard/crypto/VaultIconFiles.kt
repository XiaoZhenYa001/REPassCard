package com.example.passcard.crypto

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.security.MessageDigest
import java.util.UUID

/** Restored icons are immutable, app-private files with content-derived names. */
class VaultIconFiles(private val directory: File) {
    fun store(bytes: ByteArray): File {
        require(bytes.isNotEmpty() && bytes.size <= VaultIconArchive.MAX_ICON_BYTES) { "备份图标大小无效。" }
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IllegalStateException("无法创建备份图标目录。")
        }
        val target = File(directory, "icon_${iconContentHash(bytes)}.bin")
        require(target.canonicalFile.parentFile == directory.canonicalFile) { "备份图标路径无效。" }
        if (target.exists()) {
            require(target.isFile && target.length() == bytes.size.toLong() &&
                target.inputStream().use { readBoundedIcon(it).contentEquals(bytes) }) { "已恢复的图标文件损坏。" }
            return target
        }
        val pending = File(directory, ".${UUID.randomUUID()}.pending")
        try {
            FileOutputStream(pending).use { stream ->
                stream.write(bytes)
                stream.fd.sync()
            }
            if (!pending.renameTo(target)) {
                require(target.isFile && target.length() == bytes.size.toLong() &&
                    target.inputStream().use { readBoundedIcon(it).contentEquals(bytes) }) { "无法保存备份图标。" }
            }
            return target
        } finally {
            pending.delete()
        }
    }

    /** A backup's old URI may only refer to a file in the dedicated icon directory. */
    fun openOwnedReference(reference: String): InputStream? {
        val uri = runCatching { URI(reference) }.getOrNull() ?: return null
        if (uri.scheme != "file" || !uri.authority.isNullOrEmpty()) return null
        val file = runCatching { File(uri).canonicalFile }.getOrNull() ?: return null
        if (file.parentFile != directory.canonicalFile || !file.name.matches(OWNED_FILE_NAME)) return null
        return if (file.isFile) FileInputStream(file) else null
    }

    companion object {
        private val OWNED_FILE_NAME = Regex("icon_[0-9a-f]{64}\\.bin")
    }
}

internal fun iconContentHash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
    .joinToString("") { "%02x".format(it.toInt() and 0xff) }

internal fun readBoundedIcon(stream: InputStream, maxBytes: Int = VaultIconArchive.MAX_ICON_BYTES): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val read = stream.read(buffer, 0, minOf(buffer.size, maxBytes - output.size() + 1))
        if (read < 0) break
        if (read == 0) {
            val single = stream.read()
            if (single < 0) break
            output.write(single)
        } else {
            output.write(buffer, 0, read)
        }
        require(output.size() <= maxBytes) { "单张图标不能超过 2 MiB，请先优化图标。" }
    }
    require(output.size() > 0) { "图标文件为空，请重新选择图标。" }
    return output.toByteArray()
}
