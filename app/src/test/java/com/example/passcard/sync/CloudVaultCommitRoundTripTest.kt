package com.example.passcard.sync

import com.example.passcard.crypto.VaultCrypto
import com.example.passcard.crypto.VaultMetadata
import com.example.passcard.crypto.VaultPasswordRecord
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Production JSON, encryption and transfer decisions against a create-only object store. */
class CloudVaultCommitRoundTripTest {
    private val prefix = "repasscard/"
    private val password = "Cobalt lanterns above the lake 夜空"
    private val record = VaultPasswordRecord("id", "Example", "user", "", "", " exact secret 🔑 ", "", "", 1, 2)
    private val crypto = VaultCrypto()

    @Test fun createOnlyProviderSupportsRepeatedUploadsAndRestoreOnANewDevice() = runBlocking {
        val storage = CreateOnlyStorage()
        var backupId = 0
        val transfer = CloudVaultTransfer(storage, prefix, JsonCloudVaultCodec) { "backup-${++backupId}" }
        fun bytes(revision: Long, records: List<VaultPasswordRecord>): ByteArray {
            val metadata = VaultMetadata("device", "Phone", 1, 2, revision, records.size, 1,
                VaultCrypto.KDF_VERSION_BACKUP_PASSWORD, VaultCrypto.FORMAT_VERSION)
            return crypto.encryptVault(records, password, metadata).json.toByteArray()
        }
        val first = transfer.upload(0, 0, "", 1, "device", "Phone", false) { bytes(it, listOf(record)) }
        assertEquals(1L, first.head.commitSequence)
        val changed = record.copy(password = " updated secret 🔑 ")
        val second = transfer.upload(17, first.head.vaultRevision, first.manifestEtag, 1, "device", "Phone", false,
            validateRemoteVault = { crypto.decryptVault(it.toString(Charsets.UTF_8), password) }) {
            bytes(it, listOf(changed))
        }
        assertEquals(2L, second.head.commitSequence)
        assertEquals(18L, second.head.vaultRevision)
        assertEquals(2, storage.probes)
        assertFalse(storage.objects.containsKey("${prefix}vault/current/manifest.json"))
        assertEquals(2, storage.objects.keys.count { it.startsWith("${prefix}vault/commits/") })
        val freshDevice = CloudVaultTransfer(storage, prefix, JsonCloudVaultCodec)
        val events = mutableListOf<String>()
        val restored = freshDevice.restoreVault(
            decodeVault = { crypto.decryptVault(it.toString(Charsets.UTF_8), password).second },
            saveLocalBackup = { events += "rollback" },
            replaceLocalVault = { records -> assertEquals(listOf(changed), records); events += "database" },
            saveCheckpoint = { remote, _ ->
                assertEquals(second.manifestEtag, remote.manifest!!.meta.etag)
                assertEquals(2L, remote.head!!.commitSequence)
                events += "checkpoint"
            })
        assertEquals(listOf(changed), restored)
        assertEquals(listOf("rollback", "database", "checkpoint"), events)
        assertEquals(2, freshDevice.listBackupHistory().size)
        val before = storage.objects.toMap()
        assertTrue(runCatching {
            freshDevice.upload(18, second.head.vaultRevision, second.manifestEtag, 1, "device", "Phone", false,
                validateRemoteVault = { crypto.decryptVault(it.toString(Charsets.UTF_8), "a different backup password") }) {
                error("Wrong password must stop before encryption or upload")
            }
        }.isFailure)
        assertEquals(before, storage.objects)
        assertEquals(2, storage.probes)
    }

    @Test fun commitJsonRoundTripsTheSequenceAndKeepsPayloadFormatIndependent() {
        val head = sampleHead().copy(commitSequence = 42)
        val bytes = JsonCloudVaultCodec.writeManifest(head)
        assertEquals(3, JSONObject(bytes.toString(Charsets.UTF_8)).getInt("version"))
        assertEquals(head, JsonCloudVaultCodec.readManifest(bytes))
        assertEquals(2, VaultCrypto.FORMAT_VERSION)
    }

    @Test fun olderManifestWithoutCommitSequenceRemainsReadable() {
        val head = sampleHead()
        val json = JSONObject(JsonCloudVaultCodec.writeManifest(head).toString(Charsets.UTF_8))
        assertEquals(2, json.getInt("version"))
        assertFalse(json.has("sequence"))
        assertEquals(head, JsonCloudVaultCodec.readManifest(json.toString().toByteArray()))
        json.put("version", 1)
        assertEquals(head, JsonCloudVaultCodec.readManifest(json.toString().toByteArray()))
    }

    @Test fun commitRequiresPositiveIntegerSequence() {
        for (sequence in listOf<Any>(0, -1, 1.5, "1", JSONObject.NULL)) {
            val json = JSONObject(JsonCloudVaultCodec.writeManifest(sampleHead().copy(commitSequence = 1)).toString(Charsets.UTF_8))
            json.put("sequence", sequence)
            assertThrows(IllegalArgumentException::class.java) { JsonCloudVaultCodec.readManifest(json.toString().toByteArray()) }
        }
        val missing = JSONObject(JsonCloudVaultCodec.writeManifest(sampleHead().copy(commitSequence = 1)).toString(Charsets.UTF_8))
        missing.remove("sequence")
        assertThrows(Exception::class.java) { JsonCloudVaultCodec.readManifest(missing.toString().toByteArray()) }
    }

    @Test fun legacyManifestCannotMasqueradeAsASequencedCommit() {
        val json = JSONObject(JsonCloudVaultCodec.writeManifest(sampleHead()).toString(Charsets.UTF_8))
        json.put("sequence", 1)
        assertThrows(IllegalArgumentException::class.java) { JsonCloudVaultCodec.readManifest(json.toString().toByteArray()) }
    }

    private fun sampleHead() = CloudVaultHead("${prefix}vault/backups/backup.enc", 7, 1, 2,
        "etag", "device", "Phone", listOf("${prefix}vault/backups/backup.enc"))

    private class CreateOnlyStorage : CloudStorageClient {
        val objects = linkedMapOf<String, CloudObject>()
        var probes = 0
        private var version = 0
        override suspend fun testConnection() = Result.success(Unit)
        override suspend fun uploadObject(key: String, bytes: ByteArray): Result<CloudObjectMeta> = error("Unconditional PUT is forbidden")
        override suspend fun uploadObject(key: String, bytes: ByteArray, condition: CloudWriteCondition): Result<CloudObjectMeta> {
            check(condition == CloudWriteCondition.Missing) { "This provider has no matching-version writes" }
            if (objects.containsKey(key)) return Result.failure(CloudWriteConflictException())
            val meta = CloudObjectMeta(key, bytes.size.toLong(), 2, "etag-${++version}")
            objects[key] = CloudObject(bytes.copyOf(), meta)
            return Result.success(meta)
        }
        override suspend fun verifyCreateOnlyWrites(prefix: String): Result<Unit> {
            probes++
            return Result.success(Unit)
        }
        override suspend fun verifyConditionalWrites(prefix: String): Result<Unit> = error("This provider cannot update a matching version")
        override suspend fun readObject(key: String) = objects[key]?.let { Result.success(it) }
            ?: Result.failure(CloudObjectNotFoundException(key))
        override suspend fun downloadObject(key: String) = readObject(key).map { it.bytes }
        override suspend fun getObjectMeta(key: String) = readObject(key).map { it.meta }
        override suspend fun listObjects(prefix: String) = Result.success(objects.values.filter { it.meta.key.startsWith(prefix) }.map { it.meta })
        override suspend fun deleteObject(key: String): Result<Unit> {
            objects.remove(key)
            return Result.success(Unit)
        }
    }
}
