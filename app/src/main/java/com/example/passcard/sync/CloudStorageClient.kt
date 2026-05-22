package com.example.passcard.sync

data class CloudObjectMeta(
    val key: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val etag: String? = null
)

interface CloudStorageClient {
    suspend fun testConnection(): Result<Unit>
    suspend fun uploadObject(key: String, bytes: ByteArray): Result<CloudObjectMeta>
    suspend fun downloadObject(key: String): Result<ByteArray>
    suspend fun getObjectMeta(key: String): Result<CloudObjectMeta>
    suspend fun listObjects(prefix: String): Result<List<CloudObjectMeta>>
    suspend fun deleteObject(key: String): Result<Unit>
}
