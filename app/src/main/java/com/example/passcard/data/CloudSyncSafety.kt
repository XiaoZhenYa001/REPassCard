package com.example.passcard.data

import com.example.passcard.util.SyncSecurityMode

data class CloudSyncSnapshot(
    val localItemCount: Int,
    val localVaultRevision: Long,
    val localHasData: Boolean,
    val cloudItemCount: Int?,
    val cloudVaultRevision: Long?,
    val cloudHasData: Boolean
)

enum class SyncDirection {
    UPLOAD_LOCAL,
    DOWNLOAD_CLOUD,
    REQUIRES_CONFIRMATION,
    CONFLICT
}

data class SyncDecision(
    val direction: SyncDirection,
    val reason: String,
    val allowEmptyLocalUpload: Boolean = false,
    val allowOverwriteCloudWithEmptyLocal: Boolean = false
)

object CloudSyncSafety {
    fun decideInitialSync(snapshot: CloudSyncSnapshot): SyncDecision {
        return when {
            !snapshot.localHasData && !snapshot.cloudHasData -> SyncDecision(
                direction = SyncDirection.UPLOAD_LOCAL,
                reason = "Local vault and cloud vault are both empty. Initial sync is allowed.",
                allowEmptyLocalUpload = true
            )
            !snapshot.localHasData && snapshot.cloudHasData -> SyncDecision(
                direction = SyncDirection.DOWNLOAD_CLOUD,
                reason = "Cloud already contains data while local vault is empty. Download first to avoid wiping cloud data."
            )
            snapshot.localHasData && !snapshot.cloudHasData -> SyncDecision(
                direction = SyncDirection.UPLOAD_LOCAL,
                reason = "Cloud is empty and local vault has data. Upload local vault to initialize cloud."
            )
            else -> SyncDecision(
                direction = SyncDirection.CONFLICT,
                reason = "Both local and cloud vaults have data. Compare revisions before syncing."
            )
        }
    }

    fun shouldAllowUpload(snapshot: CloudSyncSnapshot, userConfirmedEmptyOverwrite: Boolean): Boolean {
        if (snapshot.localHasData) return true
        if (!snapshot.localHasData && !snapshot.cloudHasData) return true
        if (!snapshot.localHasData && snapshot.cloudHasData) return userConfirmedEmptyOverwrite
        return true
    }

    fun recommendedModeLabel(mode: SyncSecurityMode): String {
        return when (mode) {
            SyncSecurityMode.MAXIMUM_SECURITY -> "极致安全模式"
            SyncSecurityMode.CONVENIENCE -> "便捷使用模式"
        }
    }
}
