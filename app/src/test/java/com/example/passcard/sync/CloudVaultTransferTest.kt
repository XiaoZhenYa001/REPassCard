package com.example.passcard.sync

import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import org.junit.Assert.*
import org.junit.Test

class CloudVaultTransferTest {
    private val prefix = "test/"
    private val manifestKey = "${prefix}vault/current/manifest.json"
    private val commitsPrefix = "${prefix}vault/commits/"
    private fun commitKey(sequence: Long) = "$commitsPrefix${sequence.toString().padStart(20, '0')}.json"
    private val currentKey = "${prefix}vault/current/vault.enc"
    private val backupKey = "${prefix}vault/backups/new-backup.enc"
    private val codec = FakeCodec()
    private val storage = FakeStorage()
    private val transfer = CloudVaultTransfer(storage, prefix, codec) { "new-backup" }
    private var checkpointRevision = 0L
    private var checkpointEtag = ""

    @Test fun onlyNotFoundAllowsAnEmptyRemote() = runBlocking {
        val remote = transfer.readRemote()
        assertNull(remote.head)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun permissionAndNetworkReadFailuresNeverTriggerWrites() = runBlocking {
        for (error in listOf(CloudHttpException(403, "forbidden"), IOException("network"))) {
            storage.readErrors[manifestKey] = error
            assertSame(error, runCatching { upload() }.exceptionOrNull())
            assertTrue(storage.writes.isEmpty())
            assertEquals(0, storage.probes)
        }
    }

    @Test fun brokenManifestDoesNotFallBackToTheLegacyVault() = runBlocking {
        storage.put(currentKey, vaultBytes(4))
        storage.put(manifestKey, "broken".toByteArray())
        assertTrue(runCatching { upload() }.isFailure)
        assertFalse(currentKey in storage.reads)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun missingManifestOrVaultEtagStopsUpload() = runBlocking {
        seed()
        val manifest = storage.objects.getValue(manifestKey)
        storage.objects[manifestKey] = manifest.copy(meta = manifest.meta.copy(etag = null))
        assertTrue(runCatching { upload() }.isFailure)
        assertTrue(storage.writes.isEmpty())
        storage.objects[manifestKey] = manifest
        val key = codec.readManifest(manifest.bytes).objectKey
        val vault = storage.objects.getValue(key)
        storage.objects[key] = vault.copy(meta = vault.meta.copy(etag = null))
        assertTrue(runCatching { upload() }.isFailure)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun invalidObjectPointerCannotEscapeTheConfiguredVault() = runBlocking {
        seed()
        val head = codec.readManifest(storage.objects.getValue(manifestKey).bytes)
        storage.put(manifestKey, codec.writeManifest(head.copy(objectKey = "other/vault.enc")))
        assertTrue(runCatching { upload() }.isFailure)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun inconsistentBodyVersionAndMetadataStopUpload() = runBlocking {
        seed()
        val key = codec.readManifest(storage.objects.getValue(manifestKey).bytes).objectKey
        val vault = storage.objects.getValue(key)
        storage.objects[key] = vault.copy(bytes = vaultBytes(99))
        assertTrue(runCatching { upload() }.isFailure)
        storage.objects[key] = vault.copy(meta = vault.meta.copy(etag = "changed-body"))
        assertTrue(runCatching { upload() }.isFailure)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun incorrectBackupPasswordStopsBeforeProbeBuildOrWrites() = runBlocking {
        seed()
        val before = storage.objects.toMap()
        val previousRevision = checkpointRevision
        val previousEtag = checkpointEtag
        val wrongPassword = javax.crypto.AEADBadTagException("Incorrect backup password")
        var built = false
        var checkpointWritten = false
        val failure = runCatching {
            transfer.upload(localRevision = checkpointRevision, lastSyncedRevision = checkpointRevision,
                lastSyncedManifestEtag = checkpointEtag, itemCount = 1, deviceId = "device", deviceName = "Device",
                confirmedEmptyOverwrite = false, validateRemoteVault = { throw wrongPassword },
                buildVault = { built = true; vaultBytes(it) })
            checkpointWritten = true
        }.exceptionOrNull()
        assertSame(wrongPassword, failure)
        assertFalse(built)
        assertFalse(checkpointWritten)
        assertEquals(0, storage.probes)
        assertTrue(storage.writes.isEmpty())
        assertTrue(storage.deleted.isEmpty())
        assertEquals(before, storage.objects)
        assertEquals(previousRevision, checkpointRevision)
        assertEquals(previousEtag, checkpointEtag)
    }

    @Test fun emptyCloudDoesNotRequireAnExistingEncryptionPassword() = runBlocking {
        var validated = false
        transfer.upload(localRevision = 0, lastSyncedRevision = 0, lastSyncedManifestEtag = "", itemCount = 1,
            deviceId = "device", deviceName = "Device", confirmedEmptyOverwrite = false,
            validateRemoteVault = { validated = true; error("There is no remote vault to decrypt") },
            buildVault = { vaultBytes(it) })
        assertFalse(validated)
        assertEquals(1, storage.probes)
        assertEquals(listOf(backupKey, commitKey(1)), storage.writes.map { it.first })
    }

    @Test fun existingPasswordIsValidatedAgainstCurrentBytesBeforeProbeAndEncryption() = runBlocking {
        seed()
        var validated = false
        transfer.upload(localRevision = checkpointRevision, lastSyncedRevision = checkpointRevision,
            lastSyncedManifestEtag = checkpointEtag, itemCount = 1, deviceId = "device", deviceName = "Device",
            confirmedEmptyOverwrite = false,
            validateRemoteVault = { bytes ->
                assertArrayEquals(vaultBytes(5), bytes)
                assertEquals(0, storage.probes)
                assertTrue(storage.writes.isEmpty())
                validated = true
            },
            buildVault = { revision ->
                assertTrue(validated)
                assertEquals(1, storage.probes)
                vaultBytes(revision)
            })
        assertTrue(validated)
        assertEquals(2, storage.writes.size)
    }

    @Test fun staleCheckpointStopsBeforePasswordVerification() = runBlocking {
        seed()
        var validated = false
        val failure = runCatching {
            transfer.upload(localRevision = checkpointRevision, lastSyncedRevision = checkpointRevision,
                lastSyncedManifestEtag = "stale-manifest-version", itemCount = 1, deviceId = "device", deviceName = "Device",
                confirmedEmptyOverwrite = false, validateRemoteVault = { validated = true },
                buildVault = { vaultBytes(it) })
        }.exceptionOrNull()
        assertTrue(failure is CloudWriteConflictException)
        assertFalse(validated)
        assertEquals(0, storage.probes)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun freshCloudPublishesOneImmutableVaultThroughConditionalCreation() = runBlocking {
        val uploaded = upload()
        assertEquals(1L, uploaded.head.vaultRevision)
        assertEquals(backupKey, uploaded.head.objectKey)
        assertEquals(listOf(backupKey, commitKey(1)), storage.writes.map { it.first })
        assertTrue(storage.writes.all { it.second == CloudWriteCondition.Missing })
        assertFalse(currentKey in storage.objects)
        assertEquals(storage.objects.getValue(commitKey(1)).meta.etag, uploaded.manifestEtag)
    }

    @Test fun knownUnchangedLegacyCloudMigratesThroughTheFirstImmutableCommit() = runBlocking {
        seed()
        val uploaded = upload()
        assertEquals(6L, uploaded.head.vaultRevision)
        assertEquals(CloudWriteCondition.Missing, storage.writes.last().second)
        assertEquals(commitKey(1), storage.writes.last().first)
        assertEquals(1L, uploaded.head.commitSequence)
    }

    @Test fun unknownOrStaleCheckpointCannotOverwriteExistingCloud() = runBlocking {
        seed()
        val knownEtag = checkpointEtag
        checkpointEtag = ""
        assertTrue(runCatching { upload() }.exceptionOrNull() is CloudWriteConflictException)
        checkpointEtag = knownEtag
        checkpointRevision = 4
        assertTrue(runCatching { upload() }.exceptionOrNull() is CloudWriteConflictException)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun sameRevisionWithDifferentManifestEtagIsAConflict() = runBlocking {
        seed()
        val manifest = storage.objects.getValue(manifestKey)
        storage.put(manifestKey, manifest.bytes)
        assertTrue(runCatching { upload() }.exceptionOrNull() is CloudWriteConflictException)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun concurrentManifestCommitSurvivesTheLosingUpload() = runBlocking {
        seed()
        val winningBytes = "winning manifest".toByteArray()
        storage.beforeManifestWrite = { storage.put(commitKey(1), winningBytes) }
        assertTrue(runCatching { upload() }.exceptionOrNull() is CloudWriteConflictException)
        assertArrayEquals(winningBytes, storage.objects.getValue(commitKey(1)).bytes)
        assertTrue(backupKey in storage.objects)
        assertTrue(storage.deleted.isEmpty())
    }

    @Test fun failedCapabilityCheckPreventsVaultUpload() = runBlocking {
        storage.probeError = CloudConditionalWriteUnsupportedException()
        assertTrue(runCatching { upload() }.exceptionOrNull() is CloudConditionalWriteUnsupportedException)
        assertEquals(1, storage.probes)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun failedEncryptionOrBackupWriteCannotPublishAManifest() = runBlocking {
        val error = IOException("encrypt")
        assertSame(error, runCatching { upload { throw error } }.exceptionOrNull())
        assertTrue(storage.writes.isEmpty())
        storage.writeErrors[backupKey] = IOException("upload")
        assertTrue(runCatching { upload() }.isFailure)
        assertFalse(commitKey(1) in storage.objects)
        assertFalse(storage.writes.any { it.first.startsWith(commitsPrefix) })
    }

    @Test fun legacyFixedVaultCanBeReadButUnknownDeviceCannotUpload() = runBlocking {
        storage.put(currentKey, vaultBytes(3))
        assertEquals(3L, transfer.readRemote().head!!.vaultRevision)
        assertTrue(runCatching { upload() }.exceptionOrNull() is CloudWriteConflictException)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun legacyRestoreEstablishesTheVersionNeededForSafeMigration() = runBlocking {
        val legacy = storage.put(currentKey, vaultBytes(3))
        transfer.restoreVault(
            decodeVault = { "records" }, saveLocalBackup = {}, replaceLocalVault = {},
            saveCheckpoint = { remote, _ ->
                checkpointRevision = remote.head!!.vaultRevision
                checkpointEtag = remote.vault!!.meta.etag!!
            }
        )
        val uploaded = upload()
        assertEquals(4L, uploaded.head.vaultRevision)
        assertEquals(CloudWriteCondition.Missing, storage.writes.last().second)
        assertEquals(legacy, storage.objects[currentKey])
        assertEquals(uploaded.head, transfer.readRemote().head)
    }

    @Test fun cleanupIgnoresAnotherWritersPendingBackup() = runBlocking {
        val histories = (1..7).map { "${prefix}vault/backups/old-$it.enc" }
        seed(histories)
        val pending = "${prefix}vault/backups/pending.enc"
        storage.put(pending, vaultBytes(6))
        upload()
        assertTrue(pending in storage.objects)
        assertEquals(listOf(histories.last()), storage.deleted)
        assertEquals(1, storage.listCalls)
    }

    @Test fun restoreWaitsForDatabaseCommitBeforeSavingCheckpoint() = runBlocking {
        seed()
        val events = mutableListOf<String>()
        val restored = transfer.restoreVault(
            decodeVault = { events += "decode"; "records" },
            saveLocalBackup = { events += "backup" },
            replaceLocalVault = { events += "database" },
            saveCheckpoint = { _, _ -> events += "checkpoint" }
        )
        assertEquals("records", restored)
        assertEquals(listOf("decode", "backup", "database", "checkpoint"), events)
    }

    @Test fun failedDatabaseReplacementNeverAdvancesCheckpoint() = runBlocking {
        seed()
        var checkpointWritten = false
        val error = IOException("database")
        val result = runCatching {
            transfer.restoreVault(
                decodeVault = { "records" }, saveLocalBackup = {},
                replaceLocalVault = { throw error },
                saveCheckpoint = { _, _ -> checkpointWritten = true }
            )
        }
        // Coroutine stack recovery may copy the exception when crossing withContext.
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(error.message, result.exceptionOrNull()?.message)
        assertFalse(checkpointWritten)
    }

    @Test fun failedDecryptionNeverTouchesLocalRecords() = runBlocking {
        seed()
        val events = mutableListOf<String>()
        assertTrue(runCatching {
            transfer.restoreVault<String>(
                decodeVault = { throw IOException("decrypt") },
                saveLocalBackup = { events += "backup" },
                replaceLocalVault = { events += "database" },
                saveCheckpoint = { _, _ -> events += "checkpoint" }
            )
        }.isFailure)
        assertTrue(events.isEmpty())
    }

    @Test fun historyUsesOnlyCommittedReferencesAndHeadRequests() = runBlocking {
        val old = "${prefix}vault/backups/older.enc"
        seed(listOf("${prefix}vault/backups/old.enc", old))
        storage.put("${prefix}vault/backups/pending.enc", vaultBytes(6))
        storage.reads.clear()
        val rows = transfer.listBackupHistory()
        assertEquals(2, rows.size)
        assertEquals(listOf(commitKey(1), manifestKey), storage.reads)
        assertEquals(2, storage.headReads.size)
        assertEquals(1, storage.listCalls)
        assertEquals(5L, rows.single { it.isCurrent }.vaultRevision)
        assertNull(rows.single { !it.isCurrent }.vaultRevision)
        assertTrue(rows.none { it.id.endsWith("pending.enc") })
    }

    @Test fun historyListingDoesNotTreatPermissionErrorsAsAnEmptyHistory() = runBlocking {
        storage.readErrors[manifestKey] = CloudHttpException(403, "forbidden")
        assertTrue(runCatching { transfer.listBackupHistory() }.exceptionOrNull() is CloudHttpException)
        storage.readErrors.clear()
        storage.readErrors[currentKey] = CloudHttpException(403, "legacy forbidden")
        assertTrue(runCatching { transfer.listBackupHistory() }.exceptionOrNull() is CloudHttpException)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun oversizedHistoryIsRejectedBeforeDownloadingThePayload() = runBlocking {
        seed()
        val key = codec.readManifest(storage.objects.getValue(manifestKey).bytes).objectKey
        val original = storage.objects.getValue(key)
        storage.objects[key] = original.copy(meta = original.meta.copy(sizeBytes = LocalVaultBackupStore.MAX_BACKUP_BYTES + 1))
        val selected = transfer.listBackupHistory().single()
        storage.reads.clear()
        assertTrue(runCatching { transfer.readHistoryBackup(selected) }.isFailure)
        assertEquals(listOf(commitKey(1), manifestKey), storage.reads)
    }

    @Test fun legacyCurrentBackupRemainsSelectableWithUnknownStatistics() = runBlocking {
        storage.put(currentKey, vaultBytes(3))
        val row = transfer.listBackupHistory().single()
        assertEquals(currentKey, row.id)
        assertTrue(row.isCurrent)
        assertNull(row.vaultRevision)
        assertEquals(3L, transfer.readHistoryBackup(row).metadata.vaultRevision)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun uncommittedOrPrunedHistoryCannotBeRestored() = runBlocking {
        seed()
        val pending = "${prefix}vault/backups/pending.enc"
        val obj = storage.put(pending, vaultBytes(3))
        val row = VaultBackupSummary(VaultBackupSource.CLOUD, pending, 1000, obj.bytes.size.toLong(), etag = obj.meta.etag)
        assertTrue(runCatching { transfer.readHistoryBackup(row) }.isFailure)
        val selected = transfer.listBackupHistory().single()
        val head = codec.readManifest(storage.objects.getValue(manifestKey).bytes)
        val newKey = "${prefix}vault/backups/latest.enc"
        val latest = storage.put(newKey, vaultBytes(6))
        storage.put(manifestKey, codec.writeManifest(head.copy(objectKey = newKey, vaultRevision = 6,
            etag = latest.meta.etag!!, backups = listOf(newKey))))
        assertTrue(runCatching { transfer.readHistoryBackup(selected) }.isFailure)
    }

    @Test fun changedHistoryObjectCannotBeRestoredUsingTheListedVersion() = runBlocking {
        seed()
        val row = transfer.listBackupHistory().single()
        storage.put(row.id, vaultBytes(8))
        assertTrue(runCatching { transfer.readHistoryBackup(row) }.isFailure)
    }

    @Test fun historicalRestoreCreatesRollbackBeforeDatabaseAndInvalidatesOnlyAfterCommit() = runBlocking {
        seed()
        val row = transfer.listBackupHistory().single()
        val events = mutableListOf<String>()
        transfer.restoreHistoryBackup(row,
            decodeVault = { events += "decode"; "records" },
            saveLocalBackup = { events += "backup" },
            replaceLocalVault = { events += "database" },
            invalidateCheckpoint = { entry, _ ->
                assertEquals(5L, entry.remoteRevision)
                events += "invalidate"
            })
        assertEquals(listOf("decode", "backup", "database", "invalidate"), events)
        assertTrue(storage.writes.isEmpty())
        assertTrue(storage.deleted.isEmpty())
    }

    @Test fun failedRollbackBackupOrDatabaseCommitNeverInvalidatesCheckpoint() = runBlocking {
        seed()
        val row = transfer.listBackupHistory().single()
        for (failBackup in listOf(true, false)) {
            val events = mutableListOf<String>()
            assertTrue(runCatching {
                transfer.restoreHistoryBackup(row, decodeVault = { "records" },
                    saveLocalBackup = { events += "backup"; if (failBackup) throw IOException("backup") },
                    replaceLocalVault = { events += "database"; throw IOException("database") },
                    invalidateCheckpoint = { _, _ -> events += "invalidate" })
            }.isFailure)
            assertFalse("invalidate" in events)
            assertEquals(if (failBackup) listOf("backup") else listOf("backup", "database"), events)
        }
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun queuedAutomaticUploadCannotPublishHistoryBeforeCheckpointInvalidation() = runBlocking {
        seed()
        val selected = transfer.listBackupHistory().single()
        val originalManifest = storage.objects.getValue(manifestKey)
        val databaseCommitted = CompletableDeferred<Unit>()
        val finishRestore = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val restore = launch(start = CoroutineStart.UNDISPATCHED) {
            VaultMutationGate.run {
                transfer.restoreHistoryBackup<String>(selected, decodeVault = { "restored-history" }, saveLocalBackup = {},
                    replaceLocalVault = {
                        events += "database"
                        databaseCommitted.complete(Unit)
                        finishRestore.await()
                    },
                    invalidateCheckpoint = { _, _ ->
                        checkpointEtag = ""
                        checkpointRevision = 0
                        events += "invalidate"
                    })
            }
        }
        databaseCommitted.await()
        val automaticUpload = async(start = CoroutineStart.UNDISPATCHED) {
            VaultMutationGate.run {
                events += "upload-check"
                runCatching { upload() }
            }
        }
        assertEquals(listOf("database"), events)
        finishRestore.complete(Unit)
        restore.join()
        assertTrue(automaticUpload.await().exceptionOrNull() is CloudWriteConflictException)
        assertEquals(listOf("database", "invalidate", "upload-check"), events)
        assertEquals(originalManifest, storage.objects.getValue(manifestKey))
        assertTrue(storage.writes.isEmpty())
    }

    @Test(timeout = 5000) fun cancelledHistoryRestoreFinishesCheckpointBeforeQueuedUpload() = runBlocking {
        seed()
        val selected = transfer.listBackupHistory().single()
        val originalManifest = storage.objects.getValue(manifestKey)
        val databaseCommitted = CompletableDeferred<Unit>()
        val finishCommit = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val restore = launch(start = CoroutineStart.UNDISPATCHED) {
            VaultMutationGate.run {
                transfer.restoreHistoryBackup<String>(selected, decodeVault = { "restored-history" }, saveLocalBackup = {},
                    replaceLocalVault = {
                        events += "database"
                        databaseCommitted.complete(Unit)
                        finishCommit.await()
                    },
                    invalidateCheckpoint = { _, _ ->
                        checkpointEtag = ""
                        checkpointRevision = 0
                        events += "invalidate"
                    })
            }
        }
        databaseCommitted.await()
        restore.cancel()
        val automaticUpload = async(start = CoroutineStart.UNDISPATCHED) {
            VaultMutationGate.run {
                events += "upload-check"
                runCatching { upload() }
            }
        }
        assertEquals(listOf("database"), events)
        finishCommit.complete(Unit)
        restore.join()
        assertTrue(automaticUpload.await().exceptionOrNull() is CloudWriteConflictException)
        assertEquals(listOf("database", "invalidate", "upload-check"), events)
        assertEquals(originalManifest, storage.objects.getValue(manifestKey))
        assertTrue(storage.writes.isEmpty())
    }

    @Test(timeout = 5000) fun cancelledLatestRestoreStillCheckpointsCommittedDatabase() = runBlocking {
        seed()
        val databaseCommitted = CompletableDeferred<Unit>()
        val finishCommit = CompletableDeferred<Unit>()
        var checkpointWritten = false
        val restore = launch(start = CoroutineStart.UNDISPATCHED) {
            VaultMutationGate.run {
                transfer.restoreVault<String>(decodeVault = { "records" }, saveLocalBackup = {},
                    replaceLocalVault = {
                        databaseCommitted.complete(Unit)
                        finishCommit.await()
                    },
                    saveCheckpoint = { _, _ -> checkpointWritten = true })
            }
        }
        databaseCommitted.await()
        restore.cancel()
        assertFalse(checkpointWritten)
        finishCommit.complete(Unit)
        restore.join()
        assertTrue(checkpointWritten)
    }

    @Test(timeout = 5000) fun cancelledBeforeCommitDoesNotReplaceDatabaseOrCheckpoint() = runBlocking {
        seed()
        val selected = transfer.listBackupHistory().single()
        var databaseWritten = false
        var checkpointWritten = false
        val restore = launch(start = CoroutineStart.UNDISPATCHED) {
            VaultMutationGate.run {
                transfer.restoreHistoryBackup<String>(selected, decodeVault = { "records" },
                    saveLocalBackup = { currentCoroutineContext()[Job]!!.cancel() },
                    replaceLocalVault = { databaseWritten = true },
                    invalidateCheckpoint = { _, _ -> checkpointWritten = true })
            }
        }
        restore.join()
        assertFalse(databaseWritten)
        assertFalse(checkpointWritten)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun consecutiveUploadsUseIndependentCommitSequenceAndRetainSevenRestorableVaults() = runBlocking {
        var id = 0
        val writer = CloudVaultTransfer(storage, prefix, codec) { "upload-${++id}" }
        var uploaded = writer.upload(500, 0, "", 1, "device", "Device", false) { vaultBytes(it) }
        repeat(8) {
            uploaded = writer.upload(uploaded.head.vaultRevision, uploaded.head.vaultRevision,
                uploaded.manifestEtag, 1, "device", "Device", false) { vaultBytes(it) }
        }
        assertEquals(509L, uploaded.head.vaultRevision)
        assertEquals(9L, uploaded.head.commitSequence)
        assertEquals(9, storage.objects.keys.count { it.startsWith(commitsPrefix) })
        assertEquals(7, storage.objects.keys.count { it.startsWith("${prefix}vault/backups/") })
        assertFalse(manifestKey in storage.objects)
        assertTrue(storage.writes.all { it.second == CloudWriteCondition.Missing })
        val freshClient = CloudVaultTransfer(storage, prefix, codec)
        val history = freshClient.listBackupHistory()
        assertEquals(7, history.size)
        assertEquals(uploaded.head.objectKey, history.single { it.isCurrent }.id)
        assertEquals(509L, freshClient.readRemote().head!!.vaultRevision)
        var restoredCheckpoint = ""
        val restored = freshClient.restoreVault(
            decodeVault = { it.toString(Charsets.UTF_8) }, saveLocalBackup = {}, replaceLocalVault = {},
            saveCheckpoint = { remote, _ -> restoredCheckpoint = remote.manifest!!.meta.etag!! })
        assertEquals("509,1", restored)
        assertEquals(uploaded.manifestEtag, restoredCheckpoint)
        history.forEach { row -> assertEquals(row.id, freshClient.readHistoryBackup(row).metadata.objectKey) }
    }

    @Test fun concurrentWritersWithDifferentLocalRevisionsContendForTheSameNextCommit() = runBlocking {
        val bothArrived = CompletableDeferred<Unit>()
        var arrivals = 0
        storage.beforeCommitWrite = { key ->
            assertEquals(commitKey(1), key)
            arrivals++
            if (arrivals == 2) bothArrived.complete(Unit)
            bothArrived.await()
        }
        val first = CloudVaultTransfer(storage, prefix, codec) { "writer-a" }
        val second = CloudVaultTransfer(storage, prefix, codec) { "writer-b" }
        val firstUpload = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { first.upload(0, 0, "", 1, "a", "A", false) { vaultBytes(it) } }
        }
        val secondUpload = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { second.upload(100, 0, "", 1, "b", "B", false) { vaultBytes(it) } }
        }
        val outcomes = listOf(firstUpload.await(), secondUpload.await())
        assertEquals(1, outcomes.count { it.isSuccess })
        assertTrue(outcomes.single { it.isFailure }.exceptionOrNull() is CloudWriteConflictException)
        val winner = outcomes.single { it.isSuccess }.getOrThrow()
        assertEquals(1L, winner.head.commitSequence)
        assertEquals(winner.head, transfer.readRemote().head)
        assertEquals(listOf(commitKey(1), commitKey(1)),
            storage.writes.map { it.first }.filter { it.startsWith(commitsPrefix) })
        assertEquals(2, storage.objects.keys.count { it.startsWith("${prefix}vault/backups/") })
        assertTrue(storage.deleted.isEmpty())
    }

    @Test fun staleListingCannotPublishPastAnotherWritersOccupiedCommit() = runBlocking {
        val first = seedCommit(1, 5, "first")
        val visible = listOf(first.meta)
        val second = seedCommit(2, 6, "winner")
        checkpointRevision = 5
        checkpointEtag = first.meta.etag!!
        storage.listOverride = visible
        assertTrue(runCatching { upload() }.exceptionOrNull() is CloudWriteConflictException)
        assertEquals(second, storage.objects.getValue(commitKey(2)))
        assertFalse(commitKey(3) in storage.objects)
        assertTrue(storage.writes.isEmpty())
        assertEquals(0, storage.probes)
        assertTrue(storage.deleted.isEmpty())
    }

    @Test fun staleEmptyListingCannotReplaceTheFirstPublishedCommit() = runBlocking {
        val winner = seedCommit(1, 5, "winner")
        storage.listOverride = emptyList()
        assertTrue(runCatching { upload() }.exceptionOrNull() is CloudWriteConflictException)
        assertEquals(winner, storage.objects.getValue(commitKey(1)))
        assertFalse(commitKey(2) in storage.objects)
        assertTrue(storage.deleted.isEmpty())
    }

    @Test fun directCommitReadsBridgeAStaleListingAfterSuccessfulUpload() = runBlocking {
        val first = seedCommit(1, 5, "first")
        val second = seedCommit(2, 6, "second")
        storage.listOverride = listOf(first.meta)
        val remote = transfer.readRemote()
        assertEquals(2L, remote.head!!.commitSequence)
        assertEquals(second.meta.etag, remote.manifest!!.meta.etag)
        assertEquals(listOf(commitKey(1), commitKey(2), commitKey(3), remote.head!!.objectKey), storage.reads)
        storage.listOverride = emptyList()
        assertEquals(2L, transfer.readRemote().head!!.commitSequence)
        assertEquals(6L, transfer.listBackupHistory().single().vaultRevision)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun boundedCommitCatchUpFailsRatherThanReturningAnOlderHead() = runBlocking {
        for (sequence in 1L..256L) seedCommit(sequence, sequence, "backup-$sequence")
        storage.listOverride = emptyList()
        assertTrue(runCatching { transfer.readRemote() }.exceptionOrNull() is IllegalStateException)
        assertEquals(256, storage.reads.size)
        assertTrue(storage.writes.isEmpty())
        assertTrue(storage.headReads.isEmpty())
    }

    @Test fun failedCommitPublicationLeavesPreviousHeadAndBothVaultsUntouched() = runBlocking {
        val previous = seedCommit(1, 5, "previous")
        checkpointRevision = 5
        checkpointEtag = previous.meta.etag!!
        storage.writeErrors[commitKey(2)] = IOException("commit failed")
        assertTrue(runCatching { upload() }.exceptionOrNull() is IOException)
        assertEquals(previous, storage.objects.getValue(commitKey(1)))
        assertFalse(commitKey(2) in storage.objects)
        assertTrue(backupKey in storage.objects)
        assertEquals(5L, transfer.readRemote().head!!.vaultRevision)
        assertTrue(storage.deleted.isEmpty())
    }

    @Test fun lostSuccessfulCommitResponseNeverDeletesThePublishedVault() = runBlocking {
        val failure = IOException("response lost")
        storage.publishedResponseError = failure
        assertSame(failure, runCatching { upload() }.exceptionOrNull())
        assertTrue(commitKey(1) in storage.objects)
        assertTrue(backupKey in storage.objects)
        assertTrue(storage.deleted.isEmpty())
        assertEquals(backupKey, transfer.readRemote().head!!.objectKey)
        assertTrue(runCatching { upload() }.exceptionOrNull() is CloudWriteConflictException)
        assertEquals(1, storage.objects.keys.count { it.startsWith(commitsPrefix) })
    }

    @Test fun commitListingFailureNeverFallsBackToLegacyOrWrites() = runBlocking {
        seed()
        for (failure in listOf(CloudHttpException(403, "forbidden"), IOException("listing unavailable"))) {
            storage.listError = failure
            assertSame(failure, runCatching { transfer.readRemote() }.exceptionOrNull())
            assertSame(failure, runCatching { transfer.listBackupHistory() }.exceptionOrNull())
            assertSame(failure, runCatching { upload() }.exceptionOrNull())
            assertTrue(storage.reads.isEmpty())
            assertTrue(storage.writes.isEmpty())
            assertEquals(0, storage.probes)
        }
    }

    @Test fun malformedOrDuplicateCommitPathsNeverExposeTheLegacyHead() = runBlocking {
        seed()
        val malformed = listOf(
            "${commitsPrefix}1.json", "${commitsPrefix}00000000000000000000.json",
            "${commitsPrefix}000000000000000000-1.json", "${commitsPrefix}99999999999999999999.json",
            "${commitsPrefix}00000000000000000001.txt", "${commitsPrefix}nested/00000000000000000001.json",
            "outside/00000000000000000001.json")
        for (key in malformed) {
            storage.listOverride = listOf(CloudObjectMeta(key, 1, 1000, "listed-version"))
            assertTrue(key, runCatching { upload() }.exceptionOrNull() is IllegalArgumentException)
            assertTrue(storage.reads.isEmpty())
        }
        val duplicate = CloudObjectMeta(commitKey(1), 1, 1000, "listed-version")
        storage.listOverride = listOf(duplicate, duplicate)
        assertTrue(runCatching { transfer.readRemote() }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(storage.reads.isEmpty())
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun missingLatestCommitDoesNotFallBackToAnOlderCommitOrLegacy() = runBlocking {
        seed()
        seedCommit(1, 6, "first")
        val latest = seedCommit(2, 7, "latest")
        storage.readErrors[commitKey(2)] = CloudObjectNotFoundException(commitKey(2))
        assertTrue(runCatching { transfer.readRemote() }.exceptionOrNull() is CloudObjectNotFoundException)
        assertTrue(runCatching { transfer.listBackupHistory() }.exceptionOrNull() is CloudObjectNotFoundException)
        assertEquals(listOf(latest.meta.key, latest.meta.key), storage.reads)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun corruptLatestCommitDoesNotFallBackToAnOlderCommitOrLegacy() = runBlocking {
        seed()
        seedCommit(1, 6, "first")
        storage.put(commitKey(2), "corrupt".toByteArray())
        assertTrue(runCatching { transfer.readRemote() }.isFailure)
        assertEquals(listOf(commitKey(2)), storage.reads)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun latestCommitSequenceMustMatchItsFilename() = runBlocking {
        seed()
        val first = seedCommit(1, 6, "first")
        storage.put(commitKey(2), first.bytes)
        assertTrue(runCatching { transfer.readRemote() }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(listOf(commitKey(2)), storage.reads)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun latestCommitWithMissingVaultNeverRestoresAnOlderBackup() = runBlocking {
        seed()
        seedCommit(1, 6, "missing")
        val missingKey = "${prefix}vault/backups/missing.enc"
        storage.objects.remove(missingKey)
        assertTrue(runCatching { transfer.readRemote() }.exceptionOrNull() is CloudObjectNotFoundException)
        assertEquals(listOf(commitKey(1), commitKey(2), missingKey), storage.reads)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun immutableCommitCannotReferenceTheMutableLegacyVault() = runBlocking {
        val legacy = storage.put(currentKey, vaultBytes(5))
        val head = codec.readVaultMetadata(legacy.bytes, currentKey, legacy.meta.etag!!).copy(commitSequence = 1)
        storage.put(commitKey(1), codec.writeManifest(head))
        assertTrue(runCatching { transfer.readRemote() }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(listOf(commitKey(1)), storage.reads)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun commitHistoryListingDownloadsOnlyTheLatestCommitAndHeadsItsVaultReferences() = runBlocking {
        val first = seedCommit(1, 5, "first")
        val second = seedCommit(2, 6, "second")
        val head = codec.readManifest(second.bytes)
        storage.put(commitKey(2), codec.writeManifest(head.copy(backups = listOf(head.objectKey,
            codec.readManifest(first.bytes).objectKey))))
        storage.put("${prefix}vault/backups/pending.enc", vaultBytes(7))
        val rows = transfer.listBackupHistory()
        assertEquals(2, rows.size)
        assertEquals(listOf(commitKey(2), commitKey(3)), storage.reads)
        assertEquals(2, storage.headReads.size)
        assertEquals(1, storage.listCalls)
        assertEquals(6L, rows.single { it.isCurrent }.vaultRevision)
        assertTrue(rows.none { it.id.endsWith("pending.enc") })
    }

    @Test fun legacyManifestRemainsReadOnlyAfterMigration() = runBlocking {
        seed()
        val legacy = storage.objects.getValue(manifestKey)
        val uploaded = upload()
        assertEquals(legacy, storage.objects.getValue(manifestKey))
        assertEquals(1L, uploaded.head.commitSequence)
        assertEquals(uploaded.head, transfer.readRemote().head)
        assertFalse(storage.writes.any { it.first == manifestKey })
        storage.put(manifestKey, "obsolete manifest".toByteArray())
        assertEquals(uploaded.head, transfer.readRemote().head)
    }

    private fun seedCommit(sequence: Long, revision: Long, name: String): CloudObject {
        val key = "${prefix}vault/backups/$name.enc"
        val vault = storage.put(key, vaultBytes(revision))
        val head = codec.readVaultMetadata(vault.bytes, key, vault.meta.etag!!)
            .copy(backups = listOf(key), commitSequence = sequence)
        return storage.put(commitKey(sequence), codec.writeManifest(head))
    }

    private suspend fun upload(build: (Long) -> ByteArray = { vaultBytes(it) }) = transfer.upload(
        localRevision = checkpointRevision, lastSyncedRevision = checkpointRevision,
        lastSyncedManifestEtag = checkpointEtag, itemCount = 1,
        deviceId = "device", deviceName = "Device", confirmedEmptyOverwrite = false, buildVault = build
    )

    private fun seed(history: List<String> = listOf("${prefix}vault/backups/old.enc")) {
        history.forEach { storage.put(it, vaultBytes(5)) }
        val vault = storage.objects.getValue(history.first())
        val head = codec.readVaultMetadata(vault.bytes, vault.meta.key, vault.meta.etag!!).copy(backups = history)
        val manifest = storage.put(manifestKey, codec.writeManifest(head))
        checkpointRevision = head.vaultRevision
        checkpointEtag = manifest.meta.etag!!
    }

    private fun vaultBytes(revision: Long) = "$revision,1".toByteArray()

    private class FakeCodec : CloudVaultCodec {
        private val manifests = mutableMapOf<String, CloudVaultHead>()
        override fun writeManifest(head: CloudVaultHead): ByteArray {
            val token = "manifest-${manifests.size}"
            manifests[token] = head
            return token.toByteArray()
        }
        override fun readManifest(bytes: ByteArray) = manifests[bytes.toString(Charsets.UTF_8)]
            ?: throw IllegalArgumentException("corrupt manifest")
        override fun readVaultMetadata(bytes: ByteArray, objectKey: String, etag: String): CloudVaultHead {
            val fields = bytes.toString(Charsets.UTF_8).split(',')
            return CloudVaultHead(objectKey, fields[0].toLong(), fields[1].toInt(), 1000, etag, "device", "Device")
        }
    }

    private inner class FakeStorage : CloudStorageClient {
        val objects = mutableMapOf<String, CloudObject>()
        val readErrors = mutableMapOf<String, Throwable>()
        val writeErrors = mutableMapOf<String, Throwable>()
        val reads = mutableListOf<String>()
        val headReads = mutableListOf<String>()
        val writes = mutableListOf<Pair<String, CloudWriteCondition>>()
        val deleted = mutableListOf<String>()
        var probes = 0
        var listCalls = 0
        var probeError: Throwable? = null
        var listError: Throwable? = null
        var listOverride: List<CloudObjectMeta>? = null
        var publishedResponseError: Throwable? = null
        var beforeCommitWrite: (suspend (String) -> Unit)? = null
        var beforeManifestWrite: (() -> Unit)? = null
        private var version = 0
        fun put(key: String, bytes: ByteArray): CloudObject = CloudObject(
            bytes, CloudObjectMeta(key, bytes.size.toLong(), 1000, "version-${++version}")
        ).also { objects[key] = it }
        override suspend fun testConnection() = Result.success(Unit)
        override suspend fun readObject(key: String): Result<CloudObject> {
            reads += key
            readErrors[key]?.let { return Result.failure(it) }
            return objects[key]?.let { Result.success(it) } ?: Result.failure(CloudObjectNotFoundException(key))
        }
        override suspend fun uploadObject(key: String, bytes: ByteArray): Result<CloudObjectMeta> =
            error("Unconditional writes are forbidden")
        override suspend fun uploadObject(key: String, bytes: ByteArray, condition: CloudWriteCondition): Result<CloudObjectMeta> {
            writes += key to condition
            writeErrors[key]?.let { return Result.failure(it) }
            if (key.startsWith(commitsPrefix)) {
                beforeCommitWrite?.invoke(key)
                beforeManifestWrite?.invoke()
            }
            val previous = objects[key]
            val allowed = when (condition) {
                CloudWriteCondition.Missing -> previous == null
                is CloudWriteCondition.Matching -> previous?.meta?.etag == condition.etag
            }
            if (!allowed) return Result.failure(CloudWriteConflictException())
            val meta = put(key, bytes).meta
            if (key.startsWith(commitsPrefix)) publishedResponseError?.let { return Result.failure(it) }
            return Result.success(meta)
        }
        override suspend fun verifyConditionalWrites(prefix: String): Result<Unit> =
            error("The transfer only requires atomic creation, never overwrite CAS")
        override suspend fun verifyCreateOnlyWrites(prefix: String): Result<Unit> {
            probes++
            return probeError?.let { Result.failure(it) } ?: Result.success(Unit)
        }
        override suspend fun downloadObject(key: String) = readObject(key).map { it.bytes }
        override suspend fun getObjectMeta(key: String): Result<CloudObjectMeta> {
            headReads += key
            readErrors[key]?.let { return Result.failure(it) }
            return objects[key]?.let { Result.success(it.meta) } ?: Result.failure(CloudObjectNotFoundException(key))
        }
        override suspend fun listObjects(prefix: String): Result<List<CloudObjectMeta>> {
            listCalls++
            listError?.let { return Result.failure(it) }
            listOverride?.let { return Result.success(it) }
            return Result.success(objects.values.filter { it.meta.key.startsWith(prefix) }.map { it.meta })
        }
        override suspend fun deleteObject(key: String): Result<Unit> {
            deleted += key
            objects.remove(key)
            return Result.success(Unit)
        }
    }
}
