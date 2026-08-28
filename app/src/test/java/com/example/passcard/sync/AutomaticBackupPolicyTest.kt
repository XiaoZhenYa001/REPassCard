package com.example.passcard.sync

import com.example.passcard.data.CloudSyncSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class AutomaticBackupPolicyTest {
    @Test
    fun emptyLocalVaultIsNeverAutomaticallyUploaded() {
        assertEquals(
            AutomaticBackupDecision.SKIP_EMPTY_LOCAL,
            AutomaticBackupPolicy.decide(snapshot(local = 0, cloudRevision = 4), 4)
        )
    }

    @Test
    fun newerCloudRevisionRequiresManualResolution() {
        assertEquals(
            AutomaticBackupDecision.STOP_FOR_CLOUD_CONFLICT,
            AutomaticBackupPolicy.decide(snapshot(local = 3, cloudRevision = 6), 5)
        )
    }

    @Test
    fun unchangedCloudAllowsUpload() {
        assertEquals(
            AutomaticBackupDecision.UPLOAD,
            AutomaticBackupPolicy.decide(snapshot(local = 3, cloudRevision = 5), 5)
        )
    }

    private fun snapshot(local: Int, cloudRevision: Long?) = CloudSyncSnapshot(
        localItemCount = local,
        localVaultRevision = 1,
        localHasData = local > 0,
        cloudItemCount = cloudRevision?.let { 2 },
        cloudVaultRevision = cloudRevision,
        cloudHasData = cloudRevision != null
    )
}
