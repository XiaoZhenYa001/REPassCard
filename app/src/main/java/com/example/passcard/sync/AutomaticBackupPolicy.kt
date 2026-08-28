package com.example.passcard.sync

import com.example.passcard.data.CloudSyncSnapshot

enum class AutomaticBackupDecision {
    UPLOAD,
    SKIP_EMPTY_LOCAL,
    STOP_FOR_CLOUD_CONFLICT
}

object AutomaticBackupPolicy {
    fun decide(snapshot: CloudSyncSnapshot, lastSyncedCloudRevision: Long): AutomaticBackupDecision {
        if (!snapshot.localHasData) return AutomaticBackupDecision.SKIP_EMPTY_LOCAL
        val cloudRevision = snapshot.cloudVaultRevision ?: return AutomaticBackupDecision.UPLOAD
        return if (snapshot.cloudHasData && cloudRevision > lastSyncedCloudRevision) {
            AutomaticBackupDecision.STOP_FOR_CLOUD_CONFLICT
        } else {
            AutomaticBackupDecision.UPLOAD
        }
    }
}
