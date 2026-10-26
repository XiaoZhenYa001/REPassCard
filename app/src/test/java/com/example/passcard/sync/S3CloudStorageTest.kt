package com.example.passcard.sync

import java.io.BufferedInputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Exercises the production HTTP adapter against a controlled object-storage endpoint. */
class S3CloudStorageTest {
    private lateinit var server: ServerSocket
    private val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "test-s3").apply { isDaemon = true } }
    private val objects = ConcurrentHashMap<String, StoredObject>()
    private val requests = CopyOnWriteArrayList<RecordedRequest>()
    private val failures = ConcurrentHashMap<String, Int>()
    private val versions = AtomicInteger()
    private val logicalUrls = CopyOnWriteArrayList<URL>()
    private val listPages = ConcurrentHashMap<String, ListResponse>()
    @Volatile
    private var redirectLocation: String? = null
    @Volatile
    private var cosOnlyConditions = false
    @Volatile
    private var ignoreCreateCondition = false
    @Volatile
    private var ignoreMatchingCondition = false
    @Volatile
    private var rejectCorrectMatch = false
    @Volatile
    private var raceAfterPutKey: String? = null
    @Volatile
    private var fixedEtag = false
    @Volatile
    private var duplicateCreateFailureCode: Int? = null
    @Volatile
    private var incorrectVersionFailureCode: Int? = null
    @Volatile
    private var initialCreateFailureCode: Int? = null
    @Volatile
    private var omitPutEtag = false

    @Before
    fun setUp() {
        server = ServerSocket(0, 10, java.net.InetAddress.getByName("127.0.0.1"))
        worker.submit {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (_: java.io.IOException) { break }
                socket.use { handle(TestExchange(it)) }
            }
        }
    }

    @After
    fun tearDown() {
        server.close()
        worker.shutdownNow()
    }

    @Test
    fun conditionalCreateDoesNotReplaceAnExistingObject() = runBlocking {
        val client = storage()
        val first = client.uploadObject("vault.enc", bytes("first"), CloudWriteCondition.Missing).getOrThrow()
        val result = client.uploadObject("vault.enc", bytes("second"), CloudWriteCondition.Missing)

        assertTrue(first.etag.orEmpty().isNotBlank())
        assertTrue(result.exceptionOrNull() is CloudWriteConflictException)
        assertArrayEquals(bytes("first"), objects.getValue(path("vault.enc")).bytes)
        val put = requests.last { it.method == "PUT" }
        assertEquals("*", put.ifNoneMatch)
        assertTrue(put.authorization.contains("if-none-match"))
    }

    @Test
    fun matchingWriteRejectsAChangeMadeAfterReading() = runBlocking {
        val client = storage()
        val first = client.uploadObject("vault.enc", bytes("first"), CloudWriteCondition.Missing).getOrThrow()
        put("vault.enc", bytes("other device"))
        val result = client.uploadObject("vault.enc", bytes("stale"), CloudWriteCondition.Matching(first.etag!!))

        assertTrue(result.exceptionOrNull() is CloudWriteConflictException)
        assertArrayEquals(bytes("other device"), objects.getValue(path("vault.enc")).bytes)
        val put = requests.last { it.method == "PUT" }
        assertTrue(put.ifMatch.orEmpty().isNotBlank())
        assertTrue(put.authorization.contains("if-match"))
    }

    @Test
    fun matchingWriteUpdatesTheExpectedVersion() = runBlocking {
        val client = storage()
        val first = client.uploadObject("vault.enc", bytes("first"), CloudWriteCondition.Missing).getOrThrow()
        client.uploadObject("vault.enc", bytes("updated"), CloudWriteCondition.Matching(first.etag!!)).getOrThrow()

        assertArrayEquals(bytes("updated"), objects.getValue(path("vault.enc")).bytes)
    }

    @Test
    fun readReturnsBytesAndVersionFromOneGetRequest() = runBlocking {
        val stored = put("vault.enc", bytes("versioned content"))
        val value = storage().readObject("vault.enc").getOrThrow()

        assertArrayEquals(stored.bytes, value.bytes)
        assertEquals(stored.etag, value.meta.etag?.trim('"'))
        assertEquals(1, requests.count { it.method == "GET" })
        assertEquals(0, requests.count { it.method == "HEAD" })
    }

    @Test
    fun headReadsObjectMetadataWithoutDownloadingTheBody() = runBlocking {
        val stored = put("vault.enc", bytes("head metadata content"))
        val meta = storage().getObjectMeta("vault.enc").getOrThrow()

        assertEquals(stored.bytes.size.toLong(), meta.sizeBytes)
        assertEquals(stored.etag, meta.etag)
        assertEquals("vault.enc", meta.key)
        assertEquals(1, requests.size)
        assertEquals("HEAD", requests.single().method)
    }

    @Test
    fun successfulWriteReturnsItsOwnVersionEvenIfAnotherWriterUpdatesImmediately() = runBlocking {
        raceAfterPutKey = path("manifest.json")
        val saved = storage().uploadObject("manifest.json", bytes("first commit"), CloudWriteCondition.Missing).getOrThrow()

        assertEquals("version-1", saved.etag?.trim('"'))
        assertArrayEquals(bytes("next commit"), objects.getValue(path("manifest.json")).bytes)
        assertEquals(0, requests.count { it.method == "HEAD" })
    }

    @Test
    fun onlyNotFoundIsReportedAsAnAbsentObject() = runBlocking {
        assertTrue(storage().readObject("missing.enc").exceptionOrNull() is CloudObjectNotFoundException)
        failures[path("forbidden.enc")] = 403
        val forbidden = storage().readObject("forbidden.enc").exceptionOrNull()
        assertTrue(forbidden is CloudHttpException)
        assertFalse(forbidden is CloudObjectNotFoundException)
        assertEquals(403, (forbidden as CloudHttpException).statusCode)
        failures[path("unavailable.enc")] = 503
        val unavailable = storage().readObject("unavailable.enc").exceptionOrNull()
        assertTrue(unavailable is CloudHttpException)
        assertEquals(503, (unavailable as CloudHttpException).statusCode)
    }

    @Test
    fun successfulCapabilityCheckUsesAndRemovesOnlyItsOwnProbe() = runBlocking {
        val existing = put("vault/current/vault.enc", bytes("user data"))
        storage().verifyConditionalWrites("vault/").getOrThrow()

        assertEquals(setOf(path("vault/current/vault.enc")), objects.keys.toSet())
        assertArrayEquals(existing.bytes, objects.getValue(path("vault/current/vault.enc")).bytes)
        assertTrue(requests.any { it.method == "PUT" && it.ifNoneMatch == "*" })
        assertTrue(requests.any { it.method == "PUT" && !it.ifMatch.isNullOrBlank() })
        assertTrue(requests.filter { it.method == "PUT" }.all { it.path != path("vault/current/vault.enc") })
    }

    @Test
    fun providerIgnoringConditionalCreateFailsTheCapabilityCheck() = runBlocking {
        ignoreCreateCondition = true
        val result = storage().verifyConditionalWrites("vault/")

        assertTrue(result.exceptionOrNull() is CloudConditionalWriteUnsupportedException)
        assertTrue(objects.isEmpty())
    }

    @Test
    fun providerIgnoringMatchingConditionFailsTheCapabilityCheck() = runBlocking {
        ignoreMatchingCondition = true
        val result = storage().verifyConditionalWrites("vault/")

        assertTrue(result.exceptionOrNull() is CloudConditionalWriteUnsupportedException)
        assertTrue(objects.isEmpty())
    }

    @Test
    fun providerRejectingCorrectMatchesCannotClaimToSupportProtection() = runBlocking {
        rejectCorrectMatch = true
        val result = storage().verifyConditionalWrites("vault/")

        assertTrue(result.isFailure)
        assertTrue(objects.isEmpty())
    }

    @Test
    fun providerReturningAnUnchangingVersionFailsTheCapabilityCheck() = runBlocking {
        fixedEtag = true
        val result = storage().verifyConditionalWrites("vault/")

        assertTrue(result.exceptionOrNull() is CloudConditionalWriteUnsupportedException)
        assertTrue(objects.isEmpty())
    }

    @Test
    fun forbiddenDuplicateCreateIsReportedWithItsStatusInsteadOfUnsupportedStorage() = runBlocking {
        duplicateCreateFailureCode = 403
        val error = storage().verifyConditionalWrites("vault/").exceptionOrNull()!!

        assertFalse(error is CloudConditionalWriteUnsupportedException)
        assertTrue(error.message.orEmpty().contains("403"))
        assertFalse(error.message.orEmpty().contains("secret-body-marker"))
        assertTrue(objects.isEmpty())
    }

    @Test
    fun rejectedIncorrectVersionPreservesTheHttpStatusInsteadOfUnsupportedStorage() = runBlocking {
        incorrectVersionFailureCode = 501
        val error = storage().verifyConditionalWrites("vault/").exceptionOrNull()!!

        assertFalse(error is CloudConditionalWriteUnsupportedException)
        assertTrue(error.message.orEmpty().contains("501"))
        assertFalse(error.message.orEmpty().contains("secret-body-marker"))
        assertTrue(objects.isEmpty())
    }

    @Test
    fun forbiddenInitialCreateProvidesASafeDiagnosticWithoutEchoingResponseContent() = runBlocking {
        initialCreateFailureCode = 403
        val error = storage().verifyConditionalWrites("vault/").exceptionOrNull()!!

        assertTrue(error.message.orEmpty().contains("403"))
        assertFalse(error.message.orEmpty().contains("secret-body-marker"))
        assertEquals(0, requests.count { it.method == "DELETE" })
        assertTrue(objects.isEmpty())
    }

    @Test
    fun missingPutVersionIdentifiesTheFailedInitialCreateStepAndCleansUpTheOwnedProbe() = runBlocking {
        omitPutEtag = true
        val error = storage().verifyConditionalWrites("vault/").exceptionOrNull()!!

        assertTrue(error.message.orEmpty().contains("ETag"))
        assertTrue(error.message.orEmpty().contains("创建"))
        assertEquals(1, requests.count { it.method == "DELETE" })
        assertTrue(objects.isEmpty())
    }

    @Test
    fun unreachableEndpointReturnsASafeCreationStepDiagnostic() = runBlocking {
        server.close()
        val error = storage().verifyConditionalWrites("private-prefix/").exceptionOrNull()!!

        assertTrue(error is CloudConditionalWriteCheckException)
        val diagnostic = error as CloudConditionalWriteCheckException
        assertEquals(CloudConditionalWriteStage.CREATE_PROBE, diagnostic.stage)
        assertEquals(CloudConditionalWriteFailure.REQUEST_FAILED, diagnostic.reason)
        assertFalse(diagnostic.detail.contains("127.0.0.1"))
        assertFalse(diagnostic.detailEnglish.contains("private-prefix"))
    }

    @Test
    fun createOnlyCheckDoesNotRequireMatchingWrites() = runBlocking {
        rejectCorrectMatch = true
        storage().verifyCreateOnlyWrites("vault/").getOrThrow()

        assertTrue(objects.isEmpty())
        assertEquals(2, requests.count { it.method == "PUT" })
        assertTrue(requests.filter { it.method == "PUT" }.all { it.ifNoneMatch == "*" && it.ifMatch == null })
    }

    @Test
    fun cosCreatesIndependentVersionsAndRejectsDuplicateKeysWithItsSignedHeader() = runBlocking {
        val client = providerStorage("https://cos.ap-guangzhou.myqcloud.com")
        client.verifyCreateOnlyWrites("vault/").getOrThrow()
        client.uploadObject("vault/backups/first.enc", bytes("first"), CloudWriteCondition.Missing).getOrThrow()
        client.uploadObject("vault/backups/second.enc", bytes("second"), CloudWriteCondition.Missing).getOrThrow()
        val duplicate = client.uploadObject("vault/backups/first.enc", bytes("replacement"), CloudWriteCondition.Missing)

        assertEquals(409, (duplicate.exceptionOrNull() as CloudWriteConflictException).statusCode)
        assertArrayEquals(bytes("first"), client.downloadObject("vault/backups/first.enc").getOrThrow())
        assertArrayEquals(bytes("second"), client.downloadObject("vault/backups/second.enc").getOrThrow())
        val writes = requests.filter { it.method == "PUT" }
        assertTrue(writes.all { it.cosForbidOverwrite == "true" && it.ifNoneMatch == null && it.ifMatch == null })
        assertTrue(writes.all { it.authorization.contains("x-cos-forbid-overwrite") })
        assertTrue(logicalUrls.all { it.host == "test-bucket.cos.ap-guangzhou.myqcloud.com" })
        assertTrue(logicalUrls.all { !it.path.startsWith("/test-bucket/") })
        assertFalse(objects.keys.any { ".conditional-write-probe/" in it })
    }

    @Test
    fun bucketQualifiedCosEndpointDoesNotRepeatTheBucket() = runBlocking {
        providerStorage("https://test-bucket.cos.ap-guangzhou.myqcloud.com")
            .uploadObject("vault.enc", bytes("first"), CloudWriteCondition.Missing).getOrThrow()

        assertEquals("test-bucket.cos.ap-guangzhou.myqcloud.com", logicalUrls.single().host)
        assertEquals("/vault.enc", logicalUrls.single().path)
    }

    @Test
    fun ossUsesItsOwnSignedProtectionAndVirtualHostedEndpoint() = runBlocking {
        val client = providerStorage("https://oss-cn-hangzhou.aliyuncs.com")
        client.verifyCreateOnlyWrites("vault/").getOrThrow()
        client.uploadObject("first.enc", bytes("first"), CloudWriteCondition.Missing).getOrThrow()
        client.uploadObject("second.enc", bytes("second"), CloudWriteCondition.Missing).getOrThrow()

        val writes = requests.filter { it.method == "PUT" }
        assertTrue(writes.all { it.ossForbidOverwrite == "true" && it.cosForbidOverwrite == null && it.ifNoneMatch == null })
        assertTrue(writes.all { it.authorization.contains("x-oss-forbid-overwrite") })
        assertTrue(logicalUrls.all { it.host == "test-bucket.oss-cn-hangzhou.aliyuncs.com" })
        assertArrayEquals(bytes("first"), client.downloadObject("first.enc").getOrThrow())
        assertArrayEquals(bytes("second"), client.downloadObject("second.enc").getOrThrow())
    }

    @Test
    fun providerNameOutsideTheOfficialDomainDoesNotSelectCosHeaders() = runBlocking {
        providerStorage("https://cos.ap-guangzhou.myqcloud.com.example.invalid")
            .uploadObject("vault.enc", bytes("first"), CloudWriteCondition.Missing).getOrThrow()

        val put = requests.single { it.method == "PUT" }
        assertEquals("*", put.ifNoneMatch)
        assertEquals(null, put.cosForbidOverwrite)
        assertEquals(null, put.ossForbidOverwrite)
    }

    @Test
    fun cosIgnoringCreateOnlyProtectionStopsWithBucketConfigurationGuidance() = runBlocking {
        ignoreCreateCondition = true // Models COS with versioning, where forbid-overwrite is ineffective.
        val client = providerStorage("https://cos.ap-guangzhou.myqcloud.com")
        val error = client.verifyCreateOnlyWrites("vault/").exceptionOrNull() as CloudConditionalWriteCheckException

        assertEquals(CloudConditionalWriteStage.REJECT_DUPLICATE_CREATE, error.stage)
        assertEquals(CloudConditionalWriteFailure.CREATE_ONLY_UNAVAILABLE, error.reason)
        assertTrue(error.detail.contains("未开启版本控制"))
        assertTrue(error.detailEnglish.contains("unversioned bucket"))
        assertTrue(objects.isEmpty())
    }

    @Test
    fun genericProviderIgnoringCreateOnlyProtectionStillFailsClosed() = runBlocking {
        ignoreCreateCondition = true
        val error = storage().verifyCreateOnlyWrites("vault/").exceptionOrNull() as CloudConditionalWriteCheckException

        assertEquals(CloudConditionalWriteFailure.CONDITION_IGNORED, error.reason)
        assertTrue(objects.isEmpty())
    }

    @Test
    fun createOnlyProbeDoesNotDeleteAnObjectWhenItsInitialRequestIsRejected() = runBlocking {
        initialCreateFailureCode = 403
        val error = providerStorage("https://cos.ap-guangzhou.myqcloud.com")
            .verifyCreateOnlyWrites("vault/").exceptionOrNull() as CloudConditionalWriteCheckException

        assertEquals(403, error.httpStatus)
        assertEquals(0, requests.count { it.method == "DELETE" })
        assertFalse(error.detail.contains("secret-body-marker"))
    }

    @Test
    fun createOnlyProbeCleansUpItsOwnObjectWhenThePutVersionIsMissing() = runBlocking {
        omitPutEtag = true
        val error = storage().verifyCreateOnlyWrites("vault/").exceptionOrNull() as CloudConditionalWriteCheckException

        assertEquals(CloudConditionalWriteFailure.MISSING_ETAG, error.reason)
        assertEquals(1, requests.count { it.method == "DELETE" })
        assertTrue(objects.isEmpty())
    }

    @Test
    fun listObjectsIncludesEveryPageAndEncodesTheContinuationToken() = runBlocking {
        val token = "next/+ page="
        listPages[""] = ListResponse(200, listXml((1..1000).map { "vault/commits/$it.json" }, true, token))
        listPages[token] = ListResponse(200, listXml(listOf("vault/commits/1001.json"), false))
        val entries = storage().listObjects("vault/commits/").getOrThrow()

        assertEquals(1001, entries.size)
        assertEquals("vault/commits/1001.json", entries.last().key)
        assertEquals(2, requests.size)
        assertTrue(requests.last().query.orEmpty().contains("continuation-token=next%2F%2B%20page%3D"))
    }

    @Test
    fun aFailedLaterListPageDoesNotReturnAPartialCloudHistory() = runBlocking {
        listPages[""] = ListResponse(200, listXml(listOf("vault/first.json"), true, "next"))
        listPages["next"] = ListResponse(503, "<Error/>")
        val result = storage().listObjects("vault/")

        assertTrue(result.isFailure)
        assertEquals(503, (result.exceptionOrNull() as CloudHttpException).statusCode)
    }

    @Test
    fun aRepeatedContinuationTokenStopsInsteadOfLoopingOrTruncatingHistory() = runBlocking {
        listPages[""] = ListResponse(200, listXml(listOf("vault/first.json"), true, "repeat"))
        listPages["repeat"] = ListResponse(200, listXml(listOf("vault/second.json"), true, "repeat"))

        assertTrue(storage().listObjects("vault/").isFailure)
        assertEquals(2, requests.size)
    }

    @Test
    fun aTruncatedListWithoutAContinuationTokenCannotClaimToBeComplete() = runBlocking {
        listPages[""] = ListResponse(200, listXml(listOf("vault/first.json"), true))

        assertTrue(storage().listObjects("vault/").isFailure)
        assertEquals(1, requests.size)
    }

    @Test
    fun malformedListResponsesCannotAppearAsAnEmptyBucket() = runBlocking {
        for (xml in listOf("", "<Error/>", "<ListBucketResult/>",
            "<ListBucketResult><IsTruncated>maybe</IsTruncated></ListBucketResult>")) {
            listPages[""] = ListResponse(200, xml)
            assertTrue(storage().listObjects("vault/").isFailure)
        }
    }

    @Test
    fun duplicateOrOutOfPrefixObjectsInvalidateTheWholeListing() = runBlocking {
        listPages[""] = ListResponse(200, listXml(listOf("vault/first.json", "vault/first.json"), false))
        assertTrue(storage().listObjects("vault/").isFailure)
        listPages[""] = ListResponse(200, listXml(listOf("unrelated/first.json"), false))
        assertTrue(storage().listObjects("vault/").isFailure)
    }

    @Test
    fun signedRequestsDoNotFollowRedirectsToAnotherEndpoint() = runBlocking {
        redirectLocation = "http://127.0.0.1:${server.localPort}${path("forwarded.enc")}"
        val result = storage().readObject("redirect.enc")

        assertEquals(302, (result.exceptionOrNull() as CloudHttpException).statusCode)
        assertEquals(1, requests.size)
        assertFalse(requests.any { it.path == path("forwarded.enc") })
    }

    @Test
    fun cosListsMoreThanOnePageWithNativeMarkers() = runBlocking {
        val marker = "vault/commits/1000 +.json"
        listPages[""] = ListResponse(200, listXml((1..1000).map { "vault/commits/$it.json" }, true, marker, cos = true))
        listPages[marker] = ListResponse(200, listXml(listOf("vault/commits/1001.json"), false, cos = true))
        val entries = providerStorage("https://cos.ap-guangzhou.myqcloud.com").listObjects("vault/commits/").getOrThrow()

        assertEquals(1001, entries.size)
        assertEquals(2, requests.size)
        assertTrue(requests.all { !it.query.orEmpty().contains("list-type") && !it.query.orEmpty().contains("continuation-token") })
        assertTrue(requests.last().query.orEmpty().contains("marker=vault%2Fcommits%2F1000%20%2B.json"))
    }

    @Test
    fun cosCannotAcceptATruncatedListingWithoutItsNativeNextMarker() = runBlocking {
        listPages[""] = ListResponse(200, listXml(listOf("vault/first.json"), true, "wrong-v2-token"))

        assertTrue(providerStorage("https://cos.ap-guangzhou.myqcloud.com").listObjects("vault/").isFailure)
        assertEquals(1, requests.size)
    }

    @Test
    fun connectionCheckReadsOnlyOneSmallPageForCosAndGenericStorage() = runBlocking {
        listPages[""] = ListResponse(200, listXml(listOf("other/first"), true, "next"))
        storage().testConnection().getOrThrow()
        providerStorage("https://cos.ap-guangzhou.myqcloud.com").testConnection().getOrThrow()

        assertEquals(2, requests.size)
        assertTrue(requests.all { it.query.orEmpty().contains("max-keys=1") })
        assertTrue(requests.first().query.orEmpty().contains("list-type=2"))
        assertFalse(requests.last().query.orEmpty().contains("list-type"))
    }

    @Test
    fun listXmlRejectsDoctypeAndEntityDeclarationsWithoutAndroidSpecificFeatures() = runBlocking {
        listPages[""] = ListResponse(200, "<!DOCTYPE ListBucketResult [<!ENTITY unsafe SYSTEM 'https://example.invalid/private'>]>" +
            "<ListBucketResult><IsTruncated>false</IsTruncated></ListBucketResult>")

        assertTrue(storage().listObjects("vault/").isFailure)
        assertEquals(1, requests.size)
    }

    @Test
    fun cosHttpAdapterPublishesTwoImmutableBackupsAndRestoresFromANewClient() = runBlocking {
        cosOnlyConditions = true
        val firstClient = providerStorage("https://cos.ap-guangzhou.myqcloud.com")
        var backupId = 0
        val transfer = CloudVaultTransfer(firstClient, "vault-test/", WireVaultCodec) { "wire_${++backupId}" }
        val first = transfer.upload(0, 0, "", 1, "phone", "Phone", false) { revision -> bytes("$revision,1") }
        val second = transfer.upload(first.head.vaultRevision, first.head.vaultRevision, first.manifestEtag,
            2, "phone", "Phone", false) { revision -> bytes("$revision,2") }
        val restoredTransfer = CloudVaultTransfer(providerStorage("https://cos.ap-guangzhou.myqcloud.com"),
            "vault-test/", WireVaultCodec)
        var local = "old"
        var checkpoint = ""
        var rollbackSaved = false
        val restored = restoredTransfer.restoreVault(
            decodeVault = { String(it, Charsets.UTF_8) },
            saveLocalBackup = { rollbackSaved = true },
            replaceLocalVault = { local = it },
            saveCheckpoint = { remote, _ -> checkpoint = remote.manifest!!.meta.etag!! }
        )

        assertEquals("2,2", restored)
        assertEquals("2,2", local)
        assertTrue(rollbackSaved)
        assertEquals(second.manifestEtag, checkpoint)
        assertEquals(2, restoredTransfer.listBackupHistory().size)
        assertArrayEquals(bytes("1,1"), firstClient.downloadObject(first.head.objectKey).getOrThrow())
        assertEquals(2, objects.keys.count { "/vault/commits/" in it })
        assertFalse(objects.keys.any { "/vault/current/" in it })
        assertTrue(requests.filter { it.method == "PUT" }.all { it.cosForbidOverwrite == "true" && it.ifMatch == null })
    }

    private object WireVaultCodec : CloudVaultCodec {
        override fun writeManifest(head: CloudVaultHead): ByteArray = listOf(head.objectKey, head.vaultRevision,
            head.itemCount, head.updatedAt, head.etag, head.deviceId, head.deviceName, head.backups.joinToString(","),
            head.commitSequence).joinToString("\t").toByteArray(Charsets.UTF_8)

        override fun readManifest(bytes: ByteArray): CloudVaultHead {
            val fields = String(bytes, Charsets.UTF_8).split('\t')
            return CloudVaultHead(fields[0], fields[1].toLong(), fields[2].toInt(), fields[3].toLong(), fields[4],
                fields[5], fields[6], fields[7].split(',').filter { it.isNotEmpty() }, fields[8].toLong())
        }

        override fun readVaultMetadata(bytes: ByteArray, objectKey: String, etag: String): CloudVaultHead {
            val fields = String(bytes, Charsets.UTF_8).split(',')
            return CloudVaultHead(objectKey, fields[0].toLong(), fields[1].toInt(), 1, etag, "phone", "Phone")
        }
    }

    private fun providerStorage(endpoint: String) = S3CloudStorage(
        endpoint = endpoint,
        region = "test-region",
        bucketName = "test-bucket",
        accessKey = "test-access-key",
        secretKey = "test-secret-key",
        connectionFactory = { logicalUrl ->
            logicalUrls += logicalUrl
            val localPath = if (logicalUrl.path.startsWith("/test-bucket/")) logicalUrl.file
                else "/test-bucket${logicalUrl.file}"
            URL("http://127.0.0.1:${server.localPort}$localPath").openConnection() as HttpURLConnection
        }
    )

    private fun listXml(keys: List<String>, truncated: Boolean, token: String? = null, cos: Boolean = false): String = buildString {
        append("<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">")
        append("<IsTruncated>$truncated</IsTruncated>")
        val tokenTag = if (cos) "NextMarker" else "NextContinuationToken"
        token?.let { append("<$tokenTag>${xmlEscape(it)}</$tokenTag>") }
        keys.forEach { key ->
            append("<Contents><Key>${xmlEscape(key)}</Key><Size>1</Size>")
            append("<LastModified>2026-10-22T00:00:00Z</LastModified><ETag>\"etag\"</ETag></Contents>")
        }
        append("</ListBucketResult>")
    }

    private fun xmlEscape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun storage() = S3CloudStorage(
        endpoint = "http://127.0.0.1:${server.localPort}",
        region = "test-region",
        bucketName = "test-bucket",
        accessKey = "test-access-key",
        secretKey = "test-secret-key"
    )

    private fun path(key: String) = "/test-bucket/$key"
    private fun bytes(value: String) = value.toByteArray(Charsets.UTF_8)

    private fun put(key: String, bytes: ByteArray): StoredObject {
        val value = StoredObject(bytes, "version-${versions.incrementAndGet()}")
        objects[path(key)] = value
        return value
    }

    private fun handle(exchange: TestExchange) {
        val path = exchange.requestURI.path
        val method = exchange.requestMethod
        val noneMatch = exchange.requestHeaders.getFirst("If-None-Match")
        val match = exchange.requestHeaders.getFirst("If-Match")
        val cosForbid = exchange.requestHeaders.getFirst("x-cos-forbid-overwrite")
        val ossForbid = exchange.requestHeaders.getFirst("x-oss-forbid-overwrite")
        val createOnly = if (cosOnlyConditions) cosForbid == "true"
            else noneMatch == "*" || cosForbid == "true" || ossForbid == "true"
        requests += RecordedRequest(method, path, noneMatch, match, exchange.requestHeaders.getFirst("Authorization").orEmpty(),
            cosForbid, ossForbid, exchange.requestURI.rawQuery)
        try {
            failures[path]?.let { code -> respond(exchange, code); return }
            redirectLocation?.takeIf { path == this.path("redirect.enc") }?.let { location ->
                exchange.responseHeaders.set("Location", location)
                respond(exchange, 302)
                return
            }
            if (method == "GET" && (path == this.path("") || path == this.path("").trimEnd('/'))) {
                val query = exchange.requestURI.rawQuery.orEmpty().split('&').associate { part ->
                    val pair = part.split('=', limit = 2)
                    URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
                }
                val token = query["continuation-token"] ?: query["marker"].orEmpty()
                val keys = objects.keys.map { it.removePrefix("/test-bucket/") }
                    .filter { it.startsWith(query["prefix"].orEmpty()) && it > token }.sorted()
                val limit = query["max-keys"]?.toIntOrNull() ?: 1000
                val pageKeys = keys.take(limit)
                val page = listPages[token] ?: ListResponse(200, listXml(pageKeys, keys.size > limit,
                    pageKeys.lastOrNull().takeIf { keys.size > limit }, cos = query["list-type"] != "2"))
                respond(exchange, page.code, bytes(page.xml))
                return
            }
            when (method) {
                "PUT" -> {
                    val previous = objects[path]
                    val forcedProbeFailure = when {
                        initialCreateFailureCode != null && createOnly && previous == null -> initialCreateFailureCode
                        duplicateCreateFailureCode != null && createOnly && previous != null -> duplicateCreateFailureCode
                        incorrectVersionFailureCode != null && match?.contains("invalid-") == true -> incorrectVersionFailureCode
                        else -> null
                    }
                    if (forcedProbeFailure != null) {
                        respond(exchange, forcedProbeFailure, bytes("<Error><Code>Denied</Code><Message>secret-body-marker https://example.invalid/?credential=hidden</Message></Error>"))
                    } else if (createOnly && previous != null && !ignoreCreateCondition) {
                        respond(exchange, if (cosForbid == "true" || ossForbid == "true") 409 else 412)
                    } else if (cosOnlyConditions && match != null) {
                        respond(exchange, 501)
                    } else if (match != null && !ignoreMatchingCondition &&
                        (rejectCorrectMatch || previous == null || match.trim('"') != previous.etag)) {
                        respond(exchange, 412)
                    } else {
                        val value = StoredObject(exchange.requestBody.readBytes(), if (fixedEtag) "fixed-version" else "version-${versions.incrementAndGet()}")
                        objects[path] = value
                        if (!omitPutEtag) exchange.responseHeaders.set("ETag", "\"${value.etag}\"")
                        if (path == raceAfterPutKey) {
                            objects[path] = StoredObject(bytes("next commit"), "version-${versions.incrementAndGet()}")
                        }
                        respond(exchange, 200)
                    }
                }
                "GET", "HEAD" -> {
                    val value = objects[path]
                    if (value == null) {
                        respond(exchange, 404)
                    } else {
                        exchange.responseHeaders.set("ETag", "\"${value.etag}\"")
                        exchange.responseHeaders.set("Last-Modified", "Tue, 13 Oct 2026 00:00:00 GMT")
                        exchange.responseHeaders.set("Content-Length", value.bytes.size.toString())
                        respond(exchange, 200, if (method == "GET") value.bytes else null)
                    }
                }
                "DELETE" -> {
                    objects.remove(path)
                    respond(exchange, 204)
                }
                else -> respond(exchange, 405)
            }
        } finally {
            exchange.close()
        }
    }

    private fun respond(exchange: TestExchange, code: Int, bytes: ByteArray? = null) {
        exchange.respond(code, bytes)
    }

    /** Standard-JDK HTTP fixture, avoiding Android's restricted JDK module surface. */
    private class TestExchange(private val socket: Socket) {
        private val input = BufferedInputStream(socket.getInputStream())
        private val start = readLine().split(' ')
        val requestMethod = start[0]
        val requestURI = java.net.URI(start[1])
        val requestHeaders = Headers()
        val responseHeaders = Headers()
        val requestBody: java.io.InputStream

        init {
            while (true) {
                val line = readLine()
                if (line.isEmpty()) break
                val colon = line.indexOf(':')
                requestHeaders.set(line.substring(0, colon), line.substring(colon + 1).trim())
            }
            val size = requestHeaders.getFirst("Content-Length")?.toInt() ?: 0
            val payload = ByteArray(size)
            java.io.DataInputStream(input).readFully(payload)
            requestBody = payload.inputStream()
        }

        private fun readLine(): String {
            val line = StringBuilder()
            while (true) {
                val value = input.read()
                if (value < 0 || value == 10) break
                if (value != 13) line.append(value.toChar())
            }
            return line.toString()
        }

        fun respond(code: Int, bytes: ByteArray?) {
            if (responseHeaders.getFirst("Content-Length") == null) {
                responseHeaders.set("Content-Length", (bytes?.size ?: 0).toString())
            }
            responseHeaders.set("Connection", "close")
            val headers = buildString {
                append("HTTP/1.1 $code Test\r\n")
                responseHeaders.values.forEach { (key, value) -> append("$key: $value\r\n") }
                append("\r\n")
            }
            socket.getOutputStream().apply {
                write(headers.toByteArray(Charsets.US_ASCII))
                if (bytes != null) write(bytes)
                flush()
            }
        }

        fun close() = Unit
    }

    private class Headers {
        val values = linkedMapOf<String, String>()
        fun set(key: String, value: String) { values[key.lowercase()] = value }
        fun getFirst(key: String): String? = values[key.lowercase()]
    }

    private data class StoredObject(val bytes: ByteArray, val etag: String)
    private data class ListResponse(val code: Int, val xml: String)
    private data class RecordedRequest(
        val method: String,
        val path: String,
        val ifNoneMatch: String?,
        val ifMatch: String?,
        val authorization: String,
        val cosForbidOverwrite: String?,
        val ossForbidOverwrite: String?,
        val query: String?
    )
}
