package com.example.passcard.sync

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 从云端覆盖本地前自动备份；最多保留 10 份（设计文档）。
 */
object LocalVaultBackupStorage {
    private fun store(context: Context) = LocalVaultBackupStore(File(context.filesDir, "local_backups"))

    fun saveEncryptedVaultFile(context: Context, encryptedJson: String): Result<String> = store(context).save(encryptedJson)
    fun list(context: Context): Result<List<VaultBackupSummary>> = store(context).list()
    fun read(context: Context, summary: VaultBackupSummary): Result<ByteArray> = store(context).read(summary)
}

/** Filesystem implementation is separate so atomic writes and filename restrictions can be JVM tested. */
class LocalVaultBackupStore(
    private val directory: File,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val maxKeep: Int = 10
) {
    fun save(encryptedJson: String): Result<String> = synchronized(MUTATION_LOCK) { runCatching {
        require(maxKeep > 0)
        ensureDirectory()
        val bytes = encryptedJson.toByteArray(Charsets.UTF_8)
        require(bytes.size.toLong() in 1..MAX_BACKUP_BYTES) { "本地备份为空或超出支持的大小。" }
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now()))
        val name = "${date}_${newId()}.enc"
        val target = safeFile(name)
        check(!target.exists()) { "本地备份名称冲突，原备份已保留。" }
        val temporary = File.createTempFile(".backup-", ".tmp", directory)
        try {
            FileOutputStream(temporary).use { stream ->
                stream.write(bytes)
                stream.fd.sync()
            }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            target.setLastModified(now())
        } finally {
            temporary.delete()
        }
        (listOf(target) + listedFiles().filter { it != target }).drop(maxKeep).forEach { file ->
            check(file.delete()) { "本地备份已保存，但无法清理较早的备份。" }
        }
        name
    } }

    fun list(): Result<List<VaultBackupSummary>> = runCatching {
        ensureDirectory()
        listedFiles().take(maxKeep).map { file ->
            VaultBackupSummary(VaultBackupSource.LOCAL, file.name, file.lastModified(), file.length(),
                etag = fileVersion(file))
        }
    }

    fun read(summary: VaultBackupSummary): Result<ByteArray> = runCatching {
        require(summary.source == VaultBackupSource.LOCAL) { "无效的本地备份来源。" }
        val file = safeFile(summary.id)
        check(file.isFile && !Files.isSymbolicLink(file.toPath())) { "所选本地备份已不存在，请刷新列表。" }
        check(summary.etag == fileVersion(file)) { "所选本地备份已变化，请刷新列表。" }
        require(file.length() in 1..MAX_BACKUP_BYTES) { "本地备份为空或超出支持的大小。" }
        val bytes = file.inputStream().use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_BACKUP_BYTES) { "本地备份超出支持的大小。" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        check(summary.etag == fileVersion(file) && bytes.size.toLong() == summary.sizeBytes) {
            "读取期间本地备份已变化，请刷新列表。"
        }
        bytes
    }

    private fun ensureDirectory() {
        check(directory.isDirectory || directory.mkdirs()) { "无法访问本地备份目录。" }
        check(!Files.isSymbolicLink(directory.toPath())) { "无效的本地备份目录。" }
    }

    private fun safeFile(name: String): File {
        require(FILE_NAME.matches(name)) { "无效的本地备份名称。" }
        val file = File(directory, name)
        require(file.canonicalFile.parentFile == directory.canonicalFile &&
            !Files.isSymbolicLink(file.toPath())) { "无效的本地备份路径。" }
        return file
    }

    private fun listedFiles(): List<File> {
        val files = directory.listFiles() ?: error("无法读取本地备份列表。")
        return files.filter { it.isFile && FILE_NAME.matches(it.name) && !Files.isSymbolicLink(it.toPath()) }
            .sortedWith(compareByDescending<File> { it.lastModified() }.thenByDescending { it.name })
    }

    private fun fileVersion(file: File) = "${file.lastModified()}:${file.length()}"

    companion object {
        private val MUTATION_LOCK = Any()
        private val FILE_NAME = Regex("[0-9]{8}_[0-9]{6}(?:_[A-Za-z0-9-]+)?\\.enc")
        const val MAX_BACKUP_BYTES = 64L * 1024L * 1024L
    }
}
