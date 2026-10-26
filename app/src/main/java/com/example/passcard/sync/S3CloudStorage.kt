package com.example.passcard.sync

import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class S3CloudStorage private constructor(
    private val endpoint: String,
    private val region: String,
    private val bucketName: String,
    private val accessKey: String,
    private val secretKey: String,
    private val sessionToken: String,
    private val openConnection: (URL) -> HttpURLConnection
) : CloudStorageClient {

    constructor(
        endpoint: String,
        region: String,
        bucketName: String,
        accessKey: String,
        secretKey: String,
        sessionToken: String = ""
    ) : this(endpoint, region, bucketName, accessKey, secretKey, sessionToken,
        { it.openConnection() as HttpURLConnection })

    /** Keeps endpoint selection/signing real while allowing a local HTTP transport in protocol tests. */
    internal constructor(
        endpoint: String,
        region: String,
        bucketName: String,
        accessKey: String,
        secretKey: String,
        connectionFactory: (URL) -> HttpURLConnection,
        sessionToken: String = ""
    ) : this(endpoint, region, bucketName, accessKey, secretKey, sessionToken, connectionFactory)

    private val createOnlyHeader: String
        get() = when {
            endpointHost().endsWith(".myqcloud.com", ignoreCase = true) -> "x-cos-forbid-overwrite"
            isOssHost(endpointHost()) -> "x-oss-forbid-overwrite"
            else -> "if-none-match"
        }

    override suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cosListing = createOnlyHeader == "x-cos-forbid-overwrite"
            val query = linkedMapOf("max-keys" to "1")
            if (!cosListing) query["list-type"] = "2"
            request("GET", "", query = query).use { response ->
                response.requireSuccess("")
                parseListBucketResult(response.body, cosListing)
            }
            Unit
        }
    }

    override suspend fun uploadObject(key: String, bytes: ByteArray): Result<CloudObjectMeta> = withContext(Dispatchers.IO) {
        // A caller must select an explicit precondition; never silently perform an unprotected PUT.
        Result.failure(CloudConditionalWriteUnsupportedException())
    }

    override suspend fun uploadObject(
        key: String,
        bytes: ByteArray,
        condition: CloudWriteCondition
    ): Result<CloudObjectMeta> = withContext(Dispatchers.IO) {
        runCatching { putObject(key, bytes, condition) }
    }

    private fun putObject(
        key: String,
        bytes: ByteArray,
        condition: CloudWriteCondition,
        onSuccessfulPut: () -> Unit = {}
    ): CloudObjectMeta {
        val headers = when (condition) {
            CloudWriteCondition.Missing -> mapOf(createOnlyHeader to if (createOnlyHeader == "if-none-match") "*" else "true")
            is CloudWriteCondition.Matching -> mapOf("if-match" to quotedEtag(condition.etag))
        }
        return request("PUT", key, bytes = bytes, additionalHeaders = headers).use { response ->
            response.requireSuccess(key)
            onSuccessfulPut()
            // A later HEAD can observe another writer's version and must not become our checkpoint.
            response.metadata(key, bytes.size.toLong(), requireEtag = true)
        }
    }

    override suspend fun readObject(key: String): Result<CloudObject> = withContext(Dispatchers.IO) {
        runCatching {
            request("GET", key).use { response ->
                response.requireSuccess(key)
                CloudObject(response.body, response.metadata(key, response.body.size.toLong(), requireEtag = true))
            }
        }
    }

    override suspend fun downloadObject(key: String): Result<ByteArray> = readObject(key).map { it.bytes }

    override suspend fun getObjectMeta(key: String): Result<CloudObjectMeta> = withContext(Dispatchers.IO) {
        runCatching {
            request("HEAD", key).use { response ->
                response.requireSuccess(key)
                response.metadata(key, response.header("Content-Length")?.toLongOrNull() ?: 0L, requireEtag = true)
            }
        }
    }

    override suspend fun listObjects(prefix: String): Result<List<CloudObjectMeta>> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedPrefix = prefix.trimStart('/')
            val objects = linkedMapOf<String, CloudObjectMeta>()
            val seenTokens = mutableSetOf<String>()
            var continuationToken: String? = null
            val cosListing = createOnlyHeader == "x-cos-forbid-overwrite"
            do {
                // COS's native bucket listing uses marker/NextMarker; do not assume list-type=2 is honored.
                val query = linkedMapOf("prefix" to normalizedPrefix)
                if (!cosListing) query["list-type"] = "2"
                continuationToken?.let { query[if (cosListing) "marker" else "continuation-token"] = it }
                val page = request("GET", "", query = query).use { response ->
                    response.requireSuccess("")
                    parseListBucketResult(response.body, cosListing)
                }
                for (meta in page.objects) {
                    if (!meta.key.startsWith(normalizedPrefix) || objects.put(meta.key, meta) != null) {
                        throw IOException("云端目录返回了重复或不匹配的对象，已停止读取。")
                    }
                }
                continuationToken = if (page.isTruncated) {
                    val next = page.nextContinuationToken?.takeIf { it.isNotBlank() }
                        ?: throw IOException("云端目录缺少后续分页标记，已停止读取。")
                    if (!seenTokens.add(next)) throw IOException("云端目录分页标记重复，已停止读取。")
                    next
                } else null
            } while (continuationToken != null)
            objects.values.toList()
        }
    }

    override suspend fun deleteObject(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            request("DELETE", key).use { response ->
                response.requireSuccess(key)
            }
        }
    }

    override suspend fun verifyCreateOnlyWrites(prefix: String): Result<Unit> = withContext(Dispatchers.IO) {
        val key = "${prefix.trimEnd('/')}/.conditional-write-probe/${UUID.randomUUID()}".trimStart('/')
        var owned = false
        try {
            val firstBytes = "probe-first-${UUID.randomUUID()}".toByteArray(Charsets.UTF_8)
            val secondBytes = "probe-second-${UUID.randomUUID()}".toByteArray(Charsets.UTF_8)
            val firstEtag = probeStep(CloudConditionalWriteStage.CREATE_PROBE) {
                putObject(key, firstBytes, CloudWriteCondition.Missing) { owned = true }.etag
                    ?: throw CloudConditionalWriteUnsupportedException(reason = CloudConditionalWriteFailure.MISSING_ETAG)
            }
            probeStep(CloudConditionalWriteStage.REJECT_DUPLICATE_CREATE) {
                try {
                    requireRejectedWrite(uploadObject(key, secondBytes, CloudWriteCondition.Missing))
                } catch (error: CloudConditionalWriteUnsupportedException) {
                    if (error.reason == CloudConditionalWriteFailure.CONDITION_IGNORED && createOnlyHeader != "if-none-match") {
                        throw CloudConditionalWriteUnsupportedException(reason = CloudConditionalWriteFailure.CREATE_ONLY_UNAVAILABLE)
                    }
                    throw error
                }
            }
            probeStep(CloudConditionalWriteStage.READ_CREATED_PROBE) {
                verifyProbeContent(key, firstBytes, firstEtag)
            }
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            // Do not delete an object unless this request successfully created it.
            if (owned) deleteObject(key)
        }
    }

    override suspend fun verifyConditionalWrites(prefix: String): Result<Unit> = withContext(Dispatchers.IO) {
        val key = "${prefix.trimEnd('/')}/.conditional-write-probe/${UUID.randomUUID()}"
            .trimStart('/')
        var owned = false
        try {
            val firstBytes = "probe-first-${UUID.randomUUID()}".toByteArray(Charsets.UTF_8)
            val secondBytes = "probe-second-${UUID.randomUUID()}".toByteArray(Charsets.UTF_8)
            val thirdBytes = "probe-third-${UUID.randomUUID()}".toByteArray(Charsets.UTF_8)
            val firstEtag = probeStep(CloudConditionalWriteStage.CREATE_PROBE) {
                putObject(key, firstBytes, CloudWriteCondition.Missing) { owned = true }.etag
                    ?: throw CloudConditionalWriteUnsupportedException(reason = CloudConditionalWriteFailure.MISSING_ETAG)
            }
            probeStep(CloudConditionalWriteStage.REJECT_DUPLICATE_CREATE) {
                requireRejectedWrite(uploadObject(key, secondBytes, CloudWriteCondition.Missing))
            }
            probeStep(CloudConditionalWriteStage.READ_CREATED_PROBE) {
                verifyProbeContent(key, firstBytes, firstEtag)
            }
            probeStep(CloudConditionalWriteStage.REJECT_INCORRECT_VERSION) {
                requireRejectedWrite(uploadObject(key, secondBytes, CloudWriteCondition.Matching("invalid-${UUID.randomUUID()}")))
            }
            probeStep(CloudConditionalWriteStage.READ_AFTER_INCORRECT_VERSION) {
                verifyProbeContent(key, firstBytes, firstEtag)
            }
            val secondEtag = probeStep(CloudConditionalWriteStage.REPLACE_MATCHING_VERSION) {
                val updated = uploadObject(key, secondBytes, CloudWriteCondition.Matching(firstEtag)).getOrThrow().etag
                    ?: throw CloudConditionalWriteUnsupportedException(reason = CloudConditionalWriteFailure.MISSING_ETAG)
                if (updated == firstEtag) {
                    throw CloudConditionalWriteUnsupportedException(reason = CloudConditionalWriteFailure.UNCHANGED_ETAG)
                }
                updated
            }
            probeStep(CloudConditionalWriteStage.READ_UPDATED_PROBE) {
                verifyProbeContent(key, secondBytes, secondEtag)
            }
            probeStep(CloudConditionalWriteStage.REJECT_STALE_VERSION) {
                requireRejectedWrite(uploadObject(key, thirdBytes, CloudWriteCondition.Matching(firstEtag)))
            }
            probeStep(CloudConditionalWriteStage.READ_AFTER_STALE_VERSION) {
                verifyProbeContent(key, secondBytes, secondEtag)
            }
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            // A failed initial create is not ownership evidence, even for an extremely unlikely UUID collision.
            if (owned) deleteObject(key)
        }
    }

    private suspend fun <T> probeStep(stage: CloudConditionalWriteStage, action: suspend () -> T): T {
        try {
            return action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // Keep only safe, typed diagnostics. A provider body or transport message can contain secrets.
            throw when (error) {
                is CloudConditionalWriteUnsupportedException -> CloudConditionalWriteUnsupportedException(stage, error.reason)
                is CloudConditionalWriteCheckException -> CloudConditionalWriteCheckException(stage, error.reason, error.httpStatus)
                is CloudHttpException -> CloudConditionalWriteCheckException(stage, CloudConditionalWriteFailure.HTTP_ERROR, error.statusCode)
                else -> CloudConditionalWriteCheckException(stage, CloudConditionalWriteFailure.REQUEST_FAILED)
            }
        }
    }

    private fun requireRejectedWrite(result: Result<CloudObjectMeta>) {
        val error = result.exceptionOrNull()
        if (error is CloudWriteConflictException && error.statusCode in setOf(409, 412)) return
        if (error != null) throw error
        throw CloudConditionalWriteUnsupportedException(reason = CloudConditionalWriteFailure.CONDITION_IGNORED)
    }

    private suspend fun verifyProbeContent(key: String, expected: ByteArray, expectedEtag: String) {
        val stored = readObject(key).getOrThrow()
        if (!stored.bytes.contentEquals(expected)) {
            throw CloudConditionalWriteUnsupportedException(reason = CloudConditionalWriteFailure.CONTENT_MISMATCH)
        }
        if (stored.meta.etag != expectedEtag) {
            throw CloudConditionalWriteUnsupportedException(reason = CloudConditionalWriteFailure.VERSION_MISMATCH)
        }
    }

    private fun S3Response.metadata(key: String, sizeBytes: Long, requireEtag: Boolean): CloudObjectMeta {
        val rawEtag = header("ETag")
        if (requireEtag && rawEtag.isNullOrBlank()) {
            throw CloudConditionalWriteUnsupportedException(CloudConditionalWriteStage.OBJECT_VERSION,
                CloudConditionalWriteFailure.MISSING_ETAG)
        }
        val etag = rawEtag?.let {
            try {
                normalizedEtag(it)
            } catch (_: IllegalArgumentException) {
                throw CloudConditionalWriteUnsupportedException(CloudConditionalWriteStage.OBJECT_VERSION,
                    CloudConditionalWriteFailure.INVALID_ETAG)
            }
        }
        return CloudObjectMeta(
            key = key.trimStart('/'),
            sizeBytes = sizeBytes,
            lastModified = parseHttpDate(header("Last-Modified") ?: header("Date")),
            etag = etag
        )
    }

    private fun normalizedEtag(value: String): String {
        val clean = value.trim()
        require(clean.isNotEmpty() && clean.none { it <= ' ' || it == '\u007f' }) { "云端返回了无效的对象版本" }
        require(!clean.startsWith("W/")) { "云端对象不支持强版本校验" }
        val unquoted = if (clean.startsWith('"') && clean.endsWith('"') && clean.length > 2) {
            clean.substring(1, clean.length - 1)
        } else {
            clean
        }
        require(unquoted.isNotEmpty() && '"' !in unquoted) { "云端返回了无效的对象版本" }
        return unquoted
    }

    private fun quotedEtag(value: String): String = "\"${normalizedEtag(value)}\""

    private fun request(
        method: String,
        key: String,
        query: Map<String, String> = emptyMap(),
        bytes: ByteArray? = null,
        additionalHeaders: Map<String, String> = emptyMap()
    ): S3Response {
        val body = bytes ?: ByteArray(0)
        val bodyHash = sha256Hex(body)
        val now = Date()
        val amzDate = amzDateFormat().format(now)
        val dateStamp = dateStampFormat().format(now)
        val url = buildUrl(key, query)
        val connection = openConnection(url).apply {
            requestMethod = method
            instanceFollowRedirects = false
            connectTimeout = 20_000
            readTimeout = 30_000
            // HEAD still needs the input side to read its status and headers; only its body is skipped below.
            doInput = true
            if (bytes != null) {
                doOutput = true
                setFixedLengthStreamingMode(bytes.size)
            }
        }

        val host = url.authority
        val headers = linkedMapOf(
            "host" to host,
            "x-amz-content-sha256" to bodyHash,
            "x-amz-date" to amzDate
        )
        if (sessionToken.isNotBlank()) {
            headers["x-amz-security-token"] = sessionToken
        }
        if (bytes != null) {
            headers["content-type"] = "application/octet-stream"
        }
        headers.putAll(additionalHeaders)
        val auth = authorizationHeader(method, url, query, headers, bodyHash, amzDate, dateStamp)
        headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        connection.setRequestProperty("Authorization", auth)

        if (bytes != null) {
            connection.outputStream.use { it.write(bytes) }
        }

        val code = connection.responseCode
        val bodyBytes = when {
            method == "HEAD" -> ByteArray(0)
            code in 200..299 -> connection.inputStream.use { it.readBytes() }
            else -> connection.errorStream?.use { it.readBytes() } ?: ByteArray(0)
        }
        return S3Response(code, connection.headerFields, bodyBytes, connection)
    }

    private fun buildUrl(key: String, query: Map<String, String>): URL {
        val url = endpointUrl()
        val hostUsesBucket = url.host.startsWith("$bucketName.", ignoreCase = true)
        val useVirtualHostedStyle = hostUsesBucket || shouldUseVirtualHostedStyle(url.host)
        val requestHost = if (hostUsesBucket || !useVirtualHostedStyle) url.host else "$bucketName.${url.host}"
        val pathSegments = buildList {
            if (!useVirtualHostedStyle) add(bucketName)
            addAll(key.trimStart('/').split('/').filter { it.isNotEmpty() })
        }
        val path = pathSegments.joinToString("/") { uriEncode(it) }
        val queryString = canonicalQuery(query)
        val suffix = if (queryString.isBlank()) "" else "?$queryString"
        return URL("${url.protocol}://$requestHost${if (url.port > 0) ":${url.port}" else ""}/$path$suffix")
    }

    private fun shouldUseVirtualHostedStyle(host: String): Boolean {
        return host.endsWith(".myqcloud.com", ignoreCase = true) || isOssHost(host) ||
            host.endsWith(".amazonaws.com", ignoreCase = true) ||
            host.endsWith(".amazonaws.com.cn", ignoreCase = true) ||
            host.startsWith("s3.", ignoreCase = true)
    }

    private fun endpointUrl(): URL {
        val base = endpoint.trim().removeSuffix("/")
        return URL(if (base.startsWith("http://") || base.startsWith("https://")) base else "https://$base")
    }

    private fun endpointHost(): String = endpointUrl().host

    private fun isOssHost(host: String): Boolean =
        Regex("(^|.*\\.)oss-[a-z0-9-]+\\.aliyuncs\\.com$", RegexOption.IGNORE_CASE).matches(host)

    private fun authorizationHeader(
        method: String,
        url: URL,
        query: Map<String, String>,
        headers: Map<String, String>,
        bodyHash: String,
        amzDate: String,
        dateStamp: String
    ): String {
        val regionName = region.ifBlank { "us-east-1" }
        val service = "s3"
        val signedHeaders = headers.keys.sorted().joinToString(";")
        val canonicalHeaders = headers.toSortedMap().entries.joinToString("") { (name, value) ->
            "${name.lowercase(Locale.US)}:${value.trim()}\n"
        }
        val canonicalRequest = listOf(
            method,
            url.path.ifBlank { "/" },
            canonicalQuery(query),
            canonicalHeaders,
            signedHeaders,
            bodyHash
        ).joinToString("\n")
        val credentialScope = "$dateStamp/$regionName/$service/aws4_request"
        val stringToSign = listOf(
            "AWS4-HMAC-SHA256",
            amzDate,
            credentialScope,
            sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))
        ).joinToString("\n")
        val signingKey = signingKey(secretKey, dateStamp, regionName, service)
        val signature = hmacSha256(signingKey, stringToSign).toHex()
        return "AWS4-HMAC-SHA256 Credential=$accessKey/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
    }

    private data class ListBucketPage(
        val objects: List<CloudObjectMeta>,
        val isTruncated: Boolean,
        val nextContinuationToken: String?
    )

    private fun parseListBucketResult(bytes: ByteArray, cosListing: Boolean): ListBucketPage {
        if (bytes.isEmpty()) throw IOException("云端目录响应为空，已停止读取。")
        // Object storage returns UTF-8 XML. Parse a validated character stream so other encodings cannot hide a DTD.
        val xml = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
            .removePrefix("\uFEFF")
        if ('\u0000' in xml || xml.contains("<!DOCTYPE", ignoreCase = true) || xml.contains("<!ENTITY", ignoreCase = true)) {
            throw IOException("云端目录包含不支持的 XML 声明，已停止读取。")
        }
        // Android's parser does not support every Xerces feature flag; reject external entities independently.
        val builder = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
        builder.setEntityResolver { _, _ -> throw SAXException("External XML entities are not allowed") }
        val root = try {
            builder.parse(InputSource(StringReader(xml))).documentElement
        } catch (_: Exception) {
            throw IOException("云端目录响应格式无效，已停止读取。")
        }
        if (root.localName != "ListBucketResult") throw IOException("云端目录响应格式无效，已停止读取。")
        val objects = mutableListOf<CloudObjectMeta>()
        var truncated: Boolean? = null
        var nextToken: String? = null
        val nodes = root.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            when (node.localName) {
                "IsTruncated" -> {
                    if (truncated != null) throw IOException("云端目录包含重复的分页状态。")
                    truncated = when (node.textContent.trim()) {
                        "true" -> true
                        "false" -> false
                        else -> throw IOException("云端目录分页状态无效，已停止读取。")
                    }
                }
                "NextContinuationToken", "NextMarker" -> {
                    if (node.localName == if (cosListing) "NextMarker" else "NextContinuationToken") {
                        if (nextToken != null) throw IOException("云端目录包含重复的分页标记。")
                        nextToken = node.textContent
                    }
                }
                "Contents" -> {
                    val values = mutableMapOf<String, String>()
                    val children = node.childNodes
                    for (j in 0 until children.length) {
                        val child = children.item(j)
                        val name = child.localName ?: continue
                        if (values.put(name, child.textContent) != null) throw IOException("云端对象元数据重复，已停止读取。")
                    }
                    val key = values["Key"]?.takeIf { it.isNotEmpty() }
                        ?: throw IOException("云端目录缺少对象名称，已停止读取。")
                    val size = values["Size"]?.toLongOrNull()?.takeIf { it >= 0 }
                        ?: throw IOException("云端目录中的对象大小无效，已停止读取。")
                    objects += CloudObjectMeta(key, size, parseIsoDate(values["LastModified"]),
                        values["ETag"]?.let { normalizedEtag(it) })
                }
            }
        }
        return ListBucketPage(objects, truncated ?: throw IOException("云端目录缺少分页状态，已停止读取。"), nextToken)
    }

    private fun canonicalQuery(query: Map<String, String>): String {
        return query.entries
            .sortedWith(compareBy<Map.Entry<String, String>> { it.key }.thenBy { it.value })
            .joinToString("&") { "${uriEncode(it.key)}=${uriEncode(it.value)}" }
    }

    private fun uriEncode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun signingKey(secret: String, dateStamp: String, regionName: String, service: String): ByteArray {
        val kDate = hmacSha256(("AWS4$secret").toByteArray(Charsets.UTF_8), dateStamp)
        val kRegion = hmacSha256(kDate, regionName)
        val kService = hmacSha256(kRegion, service)
        return hmacSha256(kService, "aws4_request")
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun parseHttpDate(value: String?): Long {
        if (value.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching { httpDateFormat().parse(value)?.time ?: System.currentTimeMillis() }
            .getOrDefault(System.currentTimeMillis())
    }

    private fun parseIsoDate(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        val clean = value.removeSuffix("Z")
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(clean)?.time ?: 0L
        }.recoverCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(clean)?.time ?: 0L
        }.getOrDefault(0L)
    }

    private fun amzDateFormat() = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun dateStampFormat() = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun httpDateFormat() = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

    private class S3Response(
        val code: Int,
        private val headers: Map<String, List<String>>,
        val body: ByteArray,
        private val connection: HttpURLConnection
    ) : AutoCloseable {
        fun header(name: String): String? {
            return headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }
                ?.value
                ?.firstOrNull()
        }

        fun requireSuccess(key: String) {
            if (code in 200..299) return
            if (code == 404) throw CloudObjectNotFoundException(key)
            if (code == 409 || code == 412) throw CloudWriteConflictException(code)
            throw CloudHttpException(code, "云端请求失败（HTTP $code）。")
        }

        override fun close() {
            connection.disconnect()
        }
    }

}
