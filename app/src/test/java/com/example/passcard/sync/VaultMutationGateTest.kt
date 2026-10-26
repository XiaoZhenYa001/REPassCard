package com.example.passcard.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultMutationGateTest {
    @Test(timeout = 5000) fun queuedUploadReadsTheSnapshotOnlyAfterRestoreCommits() = runBlocking {
        var records = listOf("before-restore")
        val restoreStarted = CompletableDeferred<Unit>()
        val allowRestoreCommit = CompletableDeferred<Unit>()
        var snapshotRead = false
        val restore = launch(start = CoroutineStart.UNDISPATCHED) {
            VaultMutationGate.run {
                restoreStarted.complete(Unit)
                allowRestoreCommit.await()
                records = listOf("restored-records")
            }
        }
        restoreStarted.await()
        val upload = async(start = CoroutineStart.UNDISPATCHED) {
            VaultMutationGate.runWithSnapshot(
                loadSnapshot = { snapshotRead = true; records },
                block = { it.toList() }
            )
        }
        assertFalse(snapshotRead)
        allowRestoreCommit.complete(Unit)
        restore.join()
        assertEquals(listOf("restored-records"), upload.await())
        assertTrue(snapshotRead)
    }
}
