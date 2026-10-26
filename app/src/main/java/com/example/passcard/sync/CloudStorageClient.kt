package com.example.passcard.sync

data class CloudObjectMeta(
    val key: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val etag: String? = null
)

data class CloudObject(val bytes: ByteArray, val meta: CloudObjectMeta)

sealed interface CloudWriteCondition {
    data object Missing : CloudWriteCondition
    data class Matching(val etag: String) : CloudWriteCondition
}

open class CloudHttpException(val statusCode: Int, message: String) : java.io.IOException(message)
class CloudObjectNotFoundException(val objectKey: String) : CloudHttpException(404, "云端对象不存在：$objectKey")
class CloudWriteConflictException(statusCode: Int = 412) : CloudHttpException(
    statusCode, "云端备份已变化，请先从云端获取最新版本后再上传。"
)
enum class CloudConditionalWriteStage(val label: String, val englishLabel: String) {
    CLIENT("客户端条件写入", "client conditional writes"),
    OBJECT_VERSION("对象版本校验", "object version validation"),
    CREATE_PROBE("创建独立检测对象", "create the isolated probe"),
    REJECT_DUPLICATE_CREATE("阻止重复创建", "reject duplicate creation"),
    READ_CREATED_PROBE("核对首次写入内容", "verify the initial probe content"),
    REJECT_INCORRECT_VERSION("拒绝错误版本", "reject an incorrect version"),
    READ_AFTER_INCORRECT_VERSION("核对错误版本请求后的内容", "verify content after the incorrect version request"),
    REPLACE_MATCHING_VERSION("更新匹配版本", "replace the matching version"),
    READ_UPDATED_PROBE("核对更新后的内容", "verify the updated content"),
    REJECT_STALE_VERSION("拒绝过期版本", "reject a stale version"),
    READ_AFTER_STALE_VERSION("核对过期版本请求后的内容", "verify content after the stale version request")
}

enum class CloudConditionalWriteFailure {
    UNSUPPORTED_CLIENT,
    HTTP_ERROR,
    REQUEST_FAILED,
    CONDITION_IGNORED,
    CREATE_ONLY_UNAVAILABLE,
    MISSING_ETAG,
    INVALID_ETAG,
    UNCHANGED_ETAG,
    CONTENT_MISMATCH,
    VERSION_MISMATCH
}

/** Contains only fixed diagnostic labels and the HTTP status, never a response body or URL. */
open class CloudConditionalWriteCheckException(
    val stage: CloudConditionalWriteStage,
    val reason: CloudConditionalWriteFailure,
    val httpStatus: Int? = null
) : java.io.IOException(conditionalWriteDiagnostic(stage, reason, httpStatus, isZh = true)) {
    val detail: String get() = message.orEmpty()
    val detailEnglish: String get() = conditionalWriteDiagnostic(stage, reason, httpStatus, isZh = false)
}

class CloudConditionalWriteUnsupportedException(
    stage: CloudConditionalWriteStage = CloudConditionalWriteStage.CLIENT,
    reason: CloudConditionalWriteFailure = CloudConditionalWriteFailure.UNSUPPORTED_CLIENT
) : CloudConditionalWriteCheckException(stage, reason)

private fun conditionalWriteDiagnostic(
    stage: CloudConditionalWriteStage,
    reason: CloudConditionalWriteFailure,
    httpStatus: Int?,
    isZh: Boolean
): String {
    val problem = when (reason) {
        CloudConditionalWriteFailure.UNSUPPORTED_CLIENT -> if (isZh) "当前客户端没有提供条件写入保护" else "the client does not provide conditional writes"
        CloudConditionalWriteFailure.HTTP_ERROR -> when {
            httpStatus == 401 || httpStatus == 403 -> if (isZh) "请求被拒绝（HTTP $httpStatus），请检查连接配置、凭据及对象读写权限" else "the request was denied (HTTP $httpStatus); check connection settings, credentials and object permissions"
            else -> if (isZh) "服务端返回 HTTP ${httpStatus ?: "未知"}" else "the server returned HTTP ${httpStatus ?: "unknown"}"
        }
        CloudConditionalWriteFailure.REQUEST_FAILED -> if (isZh) "请求未完成，请检查网络连接和服务地址" else "the request did not complete; check the network and endpoint settings"
        CloudConditionalWriteFailure.CONDITION_IGNORED -> if (isZh) "本应被拒绝的条件写入却成功，无法防止覆盖其他版本" else "a conditional write that should have been rejected succeeded"
        CloudConditionalWriteFailure.CREATE_ONLY_UNAVAILABLE -> if (isZh) "服务未执行防覆盖写入；腾讯 COS 和阿里 OSS 的此项保护要求存储桶未开启版本控制，请检查桶设置和服务地址" else "the service did not enforce create-only writes; this protection in Tencent COS and Alibaba OSS requires an unversioned bucket; check bucket settings and the endpoint"
        CloudConditionalWriteFailure.MISSING_ETAG -> if (isZh) "成功响应缺少用于版本校验的 ETag" else "a successful response omitted the ETag required for version checks"
        CloudConditionalWriteFailure.INVALID_ETAG -> if (isZh) "返回的 ETag 无效或仅支持弱版本校验" else "the returned ETag was invalid or weak"
        CloudConditionalWriteFailure.UNCHANGED_ETAG -> if (isZh) "更新内容后 ETag 没有变化，无法识别过期版本" else "the ETag did not change after the content was updated"
        CloudConditionalWriteFailure.CONTENT_MISMATCH -> if (isZh) "读回内容与检测写入不一致" else "the content read back did not match the probe write"
        CloudConditionalWriteFailure.VERSION_MISMATCH -> if (isZh) "读回版本与写入响应的 ETag 不一致" else "the version read back did not match the write response ETag"
    }
    return if (isZh) "安全覆盖检测未通过（${stage.label}）：$problem。已停止上传。"
    else "Overwrite protection check failed (${stage.englishLabel}): $problem. Upload stopped."
}

interface CloudStorageClient {
    suspend fun testConnection(): Result<Unit>
    suspend fun uploadObject(key: String, bytes: ByteArray): Result<CloudObjectMeta>
    suspend fun uploadObject(key: String, bytes: ByteArray, condition: CloudWriteCondition): Result<CloudObjectMeta> =
        Result.failure(CloudConditionalWriteUnsupportedException())
    suspend fun readObject(key: String): Result<CloudObject> = Result.failure(
        UnsupportedOperationException("对象存储不支持读取一致的内容和版本信息")
    )
    /** Atomically create an absent object and reject duplicate creation; no compare-and-swap is required. */
    suspend fun verifyCreateOnlyWrites(prefix: String): Result<Unit> = verifyConditionalWrites(prefix)
    suspend fun verifyConditionalWrites(prefix: String): Result<Unit> =
        Result.failure(CloudConditionalWriteUnsupportedException())
    suspend fun downloadObject(key: String): Result<ByteArray>
    suspend fun getObjectMeta(key: String): Result<CloudObjectMeta>
    suspend fun listObjects(prefix: String): Result<List<CloudObjectMeta>>
    suspend fun deleteObject(key: String): Result<Unit>
}
