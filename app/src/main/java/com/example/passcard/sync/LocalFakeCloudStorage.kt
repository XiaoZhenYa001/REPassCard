package com.example.passcard.sync

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 设计文档第 4 步：本地模拟对象存储，用于在无真实云账号时跑通上传/下载与 manifest 流程。
 */
class LocalFakeCloudStorage(
    private val rootDir: File
) : CloudStorageClient {

    init {
        rootDir.mkdirs()
    }

    private fun fileForKey(key: String): File {
        val rel = key.trimStart('/').replace('/', File.separatorChar)
        return File(rootDir, rel)
    }

    override suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            rootDir.mkdirs()
            Unit
        }
    }

    override suspend fun uploadObject(key: String, bytes: ByteArray): Result<CloudObjectMeta> = withContext(Dispatchers.IO) {
        runCatching {
            val f = fileForKey(key)
            f.parentFile?.mkdirs()
            f.writeBytes(bytes)
            CloudObjectMeta(
                key = key.trimStart('/'),
                sizeBytes = f.length(),
                lastModified = f.lastModified(),
                etag = f.lastModified().toString()
            )
        }
    }

    override suspend fun downloadObject(key: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val f = fileForKey(key)
            require(f.isFile) { "Object not found: $key" }
            f.readBytes()
        }
    }

    override suspend fun getObjectMeta(key: String): Result<CloudObjectMeta> = withContext(Dispatchers.IO) {
        runCatching {
            val f = fileForKey(key)
            require(f.isFile) { "Object not found: $key" }
            CloudObjectMeta(
                key = key.trimStart('/'),
                sizeBytes = f.length(),
                lastModified = f.lastModified(),
                etag = f.lastModified().toString()
            )
        }
    }

    override suspend fun listObjects(prefix: String): Result<List<CloudObjectMeta>> = withContext(Dispatchers.IO) {
        runCatching {
            val p = prefix.trimStart('/').trimEnd('/')
            if (!rootDir.exists()) return@runCatching emptyList()
            rootDir.walkTopDown()
                .filter { it.isFile }
                .mapNotNull { f ->
                    val rel = f.toRelativeString(rootDir).replace(File.separatorChar, '/')
                    if (rel.startsWith(p)) {
                        CloudObjectMeta(rel, f.length(), f.lastModified(), f.lastModified().toString())
                    } else null
                }
                .toList()
        }
    }

    override suspend fun deleteObject(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val f = fileForKey(key)
            if (f.isFile) f.delete()
        }
    }
}
