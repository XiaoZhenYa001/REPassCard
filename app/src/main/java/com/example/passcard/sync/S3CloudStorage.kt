package com.example.passcard.sync

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class S3CloudStorage(
    private val endpoint: String,
    private val region: String,
    private val bucketName: String,
    private val accessKey: String,
    private val secretKey: String,
    private val sessionToken: String = ""
) : CloudStorageClient {

    override suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            listObjects("").getOrThrow()
            Unit
        }
    }

    override suspend fun uploadObject(key: String, bytes: ByteArray): Result<CloudObjectMeta> = withContext(Dispatchers.IO) {
        runCatching {
            request("PUT", key, bytes = bytes).use { response ->
                response.requireSuccess()
            }
            getObjectMeta(key).getOrThrow()
        }
    }

    override suspend fun downloadObject(key: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            request("GET", key).use { response ->
                response.requireSuccess()
                response.body
            }
        }
    }

    override suspend fun getObjectMeta(key: String): Result<CloudObjectMeta> = withContext(Dispatchers.IO) {
        runCatching {
            request("HEAD", key).use { response ->
                response.requireSuccess()
                CloudObjectMeta(
                    key = key.trimStart('/'),
                    sizeBytes = response.header("Content-Length")?.toLongOrNull() ?: 0L,
                    lastModified = parseHttpDate(response.header("Last-Modified")),
                    etag = response.header("ETag")?.trim('"')
                )
            }
        }
    }

    override suspend fun listObjects(prefix: String): Result<List<CloudObjectMeta>> = withContext(Dispatchers.IO) {
        runCatching {
            val query = linkedMapOf(
                "list-type" to "2",
                "prefix" to prefix.trimStart('/')
            )
            request("GET", "", query = query).use { response ->
                response.requireSuccess()
                parseListBucketResult(response.body)
            }
        }
    }

    override suspend fun deleteObject(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            request("DELETE", key).use { response ->
                response.requireSuccess()
            }
        }
    }

    private fun request(
        method: String,
        key: String,
        query: Map<String, String> = emptyMap(),
        bytes: ByteArray? = null
    ): S3Response {
        val body = bytes ?: ByteArray(0)
        val bodyHash = sha256Hex(body)
        val now = Date()
        val amzDate = amzDateFormat().format(now)
        val dateStamp = dateStampFormat().format(now)
        val url = buildUrl(key, query)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 30_000
            doInput = method != "HEAD"
            if (bytes != null) {
                doOutput = true
                setFixedLengthStreamingMode(bytes.size)
            }
        }

        val host = url.host
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
        val base = endpoint.trim().removeSuffix("/")
        val normalizedBase = if (base.startsWith("http://") || base.startsWith("https://")) base else "https://$base"
        val url = URL(normalizedBase)
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
        return host.endsWith(".myqcloud.com", ignoreCase = true) ||
            host.contains(".amazonaws.com", ignoreCase = true) ||
            host.startsWith("s3.", ignoreCase = true)
    }

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

    private fun parseListBucketResult(bytes: ByteArray): List<CloudObjectMeta> {
        if (bytes.isEmpty()) return emptyList()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bytes.inputStream())
        val nodes = doc.getElementsByTagName("Contents")
        return buildList {
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val children = node.childNodes
                var key = ""
                var size = 0L
                var modified = 0L
                var etag: String? = null
                for (j in 0 until children.length) {
                    val child = children.item(j)
                    when (child.nodeName) {
                        "Key" -> key = child.textContent
                        "Size" -> size = child.textContent.toLongOrNull() ?: 0L
                        "LastModified" -> modified = parseIsoDate(child.textContent)
                        "ETag" -> etag = child.textContent.trim('"')
                    }
                }
                if (key.isNotBlank()) add(CloudObjectMeta(key, size, modified, etag))
            }
        }
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

        fun requireSuccess() {
            check(code in 200..299) {
                parseErrorBody(body).ifBlank { "HTTP $code" }
            }
        }

        override fun close() {
            connection.disconnect()
        }
    }

    companion object {
        private fun parseErrorBody(body: ByteArray): String {
            val text = body.toString(Charsets.UTF_8).trim()
            if (text.isBlank()) return ""
            return runCatching {
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(body.inputStream())
                val code = doc.getElementsByTagName("Code").item(0)?.textContent.orEmpty()
                val message = doc.getElementsByTagName("Message").item(0)?.textContent.orEmpty()
                val requestId = doc.getElementsByTagName("RequestId").item(0)?.textContent.orEmpty()
                listOf(
                    "Code: $code",
                    "Message: $message",
                    "RequestId: $requestId"
                ).filter { !it.endsWith(": ") }.joinToString("\n")
            }.getOrElse {
                text.take(2_000)
            }
        }
    }
}
