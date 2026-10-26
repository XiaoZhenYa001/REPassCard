package com.example.passcard.sync

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class VaultBackupSource { LOCAL, CLOUD }

/** History rows use file/object attributes; older entries need not expose encrypted payload metadata. */
data class VaultBackupSummary(
    val source: VaultBackupSource,
    val id: String,
    val updatedAt: Long,
    val sizeBytes: Long,
    val itemCount: Int? = null,
    val vaultRevision: Long? = null,
    val deviceName: String? = null,
    val etag: String? = null,
    val isCurrent: Boolean = false
)

/** A history restore is a local edit. It must not authorize an automatic rollback of the cloud head. */
fun nextHistoryRestoreRevision(localRevision: Long, selectedRevision: Long, remoteRevision: Long = 0): Long {
    val previous = maxOf(localRevision, selectedRevision, remoteRevision)
    require(previous >= 0 && previous < Long.MAX_VALUE) { "备份修订已超出支持范围。" }
    return previous + 1
}

/** Worker uploads cannot observe replaced records before the restore's checkpoint decision is published. */
object VaultMutationGate {
    private val mutex = Mutex()
    suspend fun <T> run(block: suspend () -> T): T = mutex.withLock { block() }

    /** Capture the vault only after any queued restore has finished. */
    suspend fun <S, T> runWithSnapshot(loadSnapshot: suspend () -> S, block: suspend (S) -> T): T =
        run { block(loadSnapshot()) }

    /** Call while holding the mutation gate: a committed database replacement needs its checkpoint. */
    suspend fun <T> commit(block: suspend () -> T): T {
        currentCoroutineContext().ensureActive()
        return withContext(NonCancellable) { block() }
    }
}
