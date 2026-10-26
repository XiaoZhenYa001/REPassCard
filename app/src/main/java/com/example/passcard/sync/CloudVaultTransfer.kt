package com.example.passcard.sync

import java.util.UUID

data class CloudVaultHead(
    val objectKey: String,
    val vaultRevision: Long,
    val itemCount: Int,
    val updatedAt: Long,
    val etag: String,
    val deviceId: String,
    val deviceName: String,
    val backups: List<String> = emptyList(),
    val commitSequence: Long = 0
)

/** JSON and encryption stay at the repository boundary; transfer decisions need no Android runtime. */
interface CloudVaultCodec {
    fun readManifest(bytes: ByteArray): CloudVaultHead
    fun writeManifest(head: CloudVaultHead): ByteArray
    fun readVaultMetadata(bytes: ByteArray, objectKey: String, etag: String): CloudVaultHead
}

data class CloudVaultRemote(val manifest: CloudObject?, val head: CloudVaultHead?, val vault: CloudObject?)
data class CloudVaultUpload(val head: CloudVaultHead, val manifestEtag: String)
data class CloudVaultHistoryEntry(val summary: VaultBackupSummary, val bytes: ByteArray, val metadata: CloudVaultHead,
    val remoteRevision: Long)

class CloudVaultTransfer(
    private val client: CloudStorageClient,
    private val prefix: String,
    private val codec: CloudVaultCodec,
    private val newBackupId: () -> String = { "${System.currentTimeMillis()}_${UUID.randomUUID()}" }
) {
    private val manifestKey = "${prefix}vault/current/manifest.json"
    private val currentKey = "${prefix}vault/current/vault.enc"

    private val backupsPrefix = "${prefix}vault/backups/"
    private val commitsPrefix = "${prefix}vault/commits/"

    suspend fun readRemote(): CloudVaultRemote {
        val published = readPublishedManifest()
        if (published == null) {
            val legacy = readOrMissing(currentKey) ?: return CloudVaultRemote(null, null, null)
            val head = codec.readVaultMetadata(legacy.bytes, currentKey, requireEtag(legacy.meta.etag))
            validateHead(head)
            return CloudVaultRemote(null, head, legacy)
        }
        val (manifest, head) = published
        val vault = client.readObject(head.objectKey).getOrThrow()
        val etag = requireEtag(vault.meta.etag)
        check(etag == requireEtag(head.etag)) { "云端备份内容与清单版本不一致，已停止操作。" }
        val metadata = codec.readVaultMetadata(vault.bytes, head.objectKey, etag)
        validateHead(metadata)
        check(metadata.vaultRevision == head.vaultRevision && metadata.itemCount == head.itemCount) {
            "云端备份内容与清单修订不一致，已停止操作。"
        }
        return CloudVaultRemote(manifest, head, vault)
    }

    /** Only committed references are listed. HEAD keeps history browsing independent of vault size. */
    suspend fun listBackupHistory(): List<VaultBackupSummary> {
        val published = readPublishedManifest()
        if (published == null) {
            val meta = metaOrMissing(currentKey) ?: return emptyList()
            validateHistoryMeta(meta, currentKey)
            return listOf(historySummary(meta, null, isCurrent = true))
        }
        val (_, head) = published
        val keys = (listOf(head.objectKey) + head.backups).distinct().take(7)
        return keys.map { key ->
            val meta = client.getObjectMeta(key).getOrThrow()
            validateHistoryMeta(meta, key)
            if (key == head.objectKey) check(requireEtag(meta.etag) == requireEtag(head.etag)) {
                "云端备份内容与清单版本不一致，请刷新列表。"
            }
            historySummary(meta, head.takeIf { key == head.objectKey }, key == head.objectKey)
        }.sortedByDescending { it.updatedAt }
    }

    /** Recheck membership and object version when the user chooses a history row. */
    suspend fun readHistoryBackup(summary: VaultBackupSummary): CloudVaultHistoryEntry {
        require(summary.source == VaultBackupSource.CLOUD) { "无效的云端备份来源。" }
        val currentHead = readPublishedManifest()?.second
        val committedKeys = currentHead?.let { (listOf(it.objectKey) + it.backups).distinct().take(7) }
            ?: listOf(currentKey)
        check(summary.id in committedKeys) { "所选备份已不在已提交的云端历史中，请刷新列表。" }
        val selectedMeta = client.getObjectMeta(summary.id).getOrThrow()
        validateHistoryMeta(selectedMeta, summary.id)
        require(selectedMeta.sizeBytes <= LocalVaultBackupStore.MAX_BACKUP_BYTES) { "云端备份超出支持的大小。" }
        check(requireEtag(selectedMeta.etag) == requireEtag(summary.etag) && selectedMeta.sizeBytes == summary.sizeBytes) {
            "所选云端备份已变化，请刷新列表。"
        }
        val vault = client.readObject(summary.id).getOrThrow()
        check(vault.meta.key == summary.id && vault.bytes.size.toLong() == summary.sizeBytes) {
            "读取期间云端备份已变化，请刷新列表。"
        }
        val etag = requireEtag(vault.meta.etag)
        check(etag == requireEtag(summary.etag)) { "所选云端备份已变化，请刷新列表。" }
        val metadata = codec.readVaultMetadata(vault.bytes, summary.id, etag)
        validateHead(metadata)
        if (currentHead?.objectKey == summary.id) {
            check(etag == requireEtag(currentHead.etag) && metadata.vaultRevision == currentHead.vaultRevision &&
                metadata.itemCount == currentHead.itemCount) { "云端备份内容与清单修订不一致，已停止恢复。" }
        }
        return CloudVaultHistoryEntry(summary, vault.bytes, metadata, currentHead?.vaultRevision ?: metadata.vaultRevision)
    }

    suspend fun <T> restoreHistoryBackup(
        summary: VaultBackupSummary,
        decodeVault: (ByteArray) -> T,
        saveLocalBackup: suspend () -> Unit,
        replaceLocalVault: suspend (T) -> Unit,
        validateRestored: (CloudVaultHistoryEntry, T) -> Unit = { _, _ -> },
        invalidateCheckpoint: (CloudVaultHistoryEntry, T) -> Unit
    ): T {
        val selected = readHistoryBackup(summary)
        val restored = decodeVault(selected.bytes)
        validateRestored(selected, restored)
        saveLocalBackup()
        VaultMutationGate.commit {
            replaceLocalVault(restored)
            invalidateCheckpoint(selected, restored)
        }
        return restored
    }

    private fun validateHistoryMeta(meta: CloudObjectMeta, key: String) {
        require(meta.key == key && meta.sizeBytes > 0 && meta.lastModified >= 0) { "云端备份属性无效。" }
        requireEtag(meta.etag)
    }

    private fun historySummary(meta: CloudObjectMeta, head: CloudVaultHead?, isCurrent: Boolean) = VaultBackupSummary(
        source = VaultBackupSource.CLOUD, id = meta.key, updatedAt = meta.lastModified, sizeBytes = meta.sizeBytes,
        itemCount = head?.itemCount, vaultRevision = head?.vaultRevision, deviceName = head?.deviceName,
        etag = requireEtag(meta.etag), isCurrent = isCurrent
    )

    private suspend fun metaOrMissing(key: String): CloudObjectMeta? {
        val result = client.getObjectMeta(key)
        if (result.isSuccess) return result.getOrThrow()
        val error = result.exceptionOrNull()!!
        if (error is CloudObjectNotFoundException) return null
        throw error
    }

    suspend fun upload(
        localRevision: Long,
        lastSyncedRevision: Long,
        lastSyncedManifestEtag: String,
        itemCount: Int,
        deviceId: String,
        deviceName: String,
        confirmedEmptyOverwrite: Boolean,
        validateRemoteVault: (ByteArray) -> Unit = {},
        buildVault: (Long) -> ByteArray
    ): CloudVaultUpload {
        require(localRevision >= 0 && itemCount >= 0)
        val remote = readRemote()
        check(itemCount > 0 || remote.head == null || confirmedEmptyOverwrite) {
            "不允许在未确认的情况下用空库覆盖云端。"
        }
        remote.head?.let { head ->
            val version = requireEtag(remote.manifest?.meta?.etag ?: remote.vault?.meta?.etag)
            if (lastSyncedRevision != head.vaultRevision || lastSyncedManifestEtag.isBlank() ||
                requireEtag(lastSyncedManifestEtag) != version) {
                throw CloudWriteConflictException()
            }
        }
        // The caller verifies the encryption password against the exact current vault before any probe or write.
        // This also protects unattended uploads from passwords cached before a restore or another device's change.
        remote.vault?.let { validateRemoteVault(it.bytes) }
        client.verifyCreateOnlyWrites(prefix).getOrThrow()
        val previousRevision = maxOf(localRevision, remote.head?.vaultRevision ?: 0L)
        check(previousRevision < Long.MAX_VALUE) { "备份修订已超出支持范围。" }
        val revision = previousRevision + 1
        val previousSequence = remote.head?.commitSequence ?: 0L
        check(previousSequence < Long.MAX_VALUE) { "备份提交序号已超出支持范围。" }
        val sequence = previousSequence + 1
        val bytes = buildVault(revision)
        val backupKey = "$backupsPrefix${newBackupId()}.enc"
        require(isBackupKey(backupKey)) { "无效的备份对象路径。" }
        val vaultMeta = client.uploadObject(backupKey, bytes, CloudWriteCondition.Missing).getOrThrow()
        val etag = requireEtag(vaultMeta.etag)
        val history = (listOf(backupKey) + listOfNotNull(remote.head?.objectKey)
            .filter(::isBackupKey) + remote.head?.backups.orEmpty()).distinct()
        val head = CloudVaultHead(backupKey, revision, itemCount, vaultMeta.lastModified,
            etag, deviceId, deviceName, history.take(7), commitSequence = sequence)
        // Each sequence is an immutable publication slot. Concurrent writers must contend for the same next slot,
        // even when their local revisions differ. Never skip a rejected slot or remove a possibly published vault.
        val manifestMeta = client.uploadObject(commitKey(sequence), codec.writeManifest(head),
            CloudWriteCondition.Missing).getOrThrow()
        val manifestEtag = requireEtag(manifestMeta.etag)
        // Delete only objects known to be committed history. A remote listing can contain another writer's pending vault.
        history.drop(7).filter(::isBackupKey).forEach { client.deleteObject(it) }
        return CloudVaultUpload(head, manifestEtag)
    }

    suspend fun <T> restoreVault(
        decodeVault: (ByteArray) -> T,
        saveLocalBackup: suspend () -> Unit,
        replaceLocalVault: suspend (T) -> Unit,
        saveCheckpoint: (CloudVaultRemote, T) -> Unit
    ): T {
        val remote = readRemote()
        val vault = remote.vault ?: throw IllegalStateException("云端还没有可恢复的备份。")
        val restored = decodeVault(vault.bytes)
        saveLocalBackup()
        VaultMutationGate.commit {
            replaceLocalVault(restored)
            // A failed database replacement must leave the sync checkpoint unchanged.
            saveCheckpoint(remote, restored)
        }
        return restored
    }

    /** Commit objects are permanent fences; only the newest commit's seven vault references are retained. */
    private suspend fun readPublishedManifest(): Pair<CloudObject, CloudVaultHead>? {
        val commits = client.listObjects(commitsPrefix).getOrThrow()
        val sequences = commits.map { meta ->
            val suffix = meta.key.removePrefix(commitsPrefix)
            val sequence = suffix.removeSuffix(".json").toLongOrNull()
            require(meta.key.startsWith(commitsPrefix) && sequence != null && sequence > 0 &&
                meta.key == commitKey(sequence)) { "云端备份提交路径无效，已停止操作。" }
            sequence to meta.key
        }
        require(sequences.map { it.first }.distinct().size == sequences.size) {
            "云端备份提交列表包含重复序号，已停止操作。"
        }
        val latest = sequences.maxByOrNull { it.first }
        var published = latest?.let { (sequence, key) ->
            // A listed commit that disappears, cannot be read, or is corrupt must not expose an older head.
            decodeManifest(client.readObject(key).getOrThrow(), key, sequence)
        }
        var sequence = latest?.first ?: 0L
        // COS listings can lag behind a successful PUT. Direct reads bridge that gap and preserve immediate
        // read-after-upload behavior. A concurrent publication after the final 404 still contends for one slot.
        repeat(256) {
            if (sequence == Long.MAX_VALUE) return published
            val nextSequence = sequence + 1
            val nextKey = commitKey(nextSequence)
            val next = readOrMissing(nextKey)
            if (next == null) {
                return published ?: readOrMissing(manifestKey)?.let { decodeManifest(it, manifestKey, 0) }
            }
            published = decodeManifest(next, nextKey, nextSequence)
            sequence = nextSequence
        }
        error("云端备份更新较多，请稍后刷新重试。")
    }

    private fun decodeManifest(manifest: CloudObject, key: String, sequence: Long): Pair<CloudObject, CloudVaultHead> {
        require(manifest.meta.key == key) { "云端备份提交对象路径不一致。" }
        requireEtag(manifest.meta.etag)
        val head = codec.readManifest(manifest.bytes)
        validateHead(head)
        require(head.commitSequence == sequence) { "云端备份提交序号与清单不一致，已停止操作。" }
        return manifest to head
    }

    private fun commitKey(sequence: Long): String = "$commitsPrefix${sequence.toString().padStart(20, '0')}.json"

    private suspend fun readOrMissing(key: String): CloudObject? {
        val result = client.readObject(key)
        if (result.isSuccess) return result.getOrThrow()
        val error = result.exceptionOrNull()!!
        if (error is CloudObjectNotFoundException) return null
        throw error
    }

    private fun validateHead(head: CloudVaultHead) {
        require(head.vaultRevision > 0 && head.commitSequence >= 0 && head.itemCount >= 0 && head.updatedAt >= 0) {
            "云端备份清单包含无效修订或统计。"
        }
        require(head.objectKey == currentKey || isBackupKey(head.objectKey)) { "云端备份清单包含无效对象路径。" }
        requireEtag(head.etag)
        require(head.commitSequence == 0L ||
            (isBackupKey(head.objectKey) && head.backups.firstOrNull() == head.objectKey)) {
            "云端备份提交必须引用独立备份及其完整历史。"
        }
        require(head.backups.all(::isBackupKey)) { "云端备份历史包含无效对象路径。" }
        require(head.backups.size <= 7 && head.backups.distinct().size == head.backups.size) {
            "云端备份历史包含过多或重复对象。"
        }
    }

    private fun isBackupKey(key: String): Boolean = key.startsWith(backupsPrefix) &&
        key.removePrefix(backupsPrefix).matches(Regex("[A-Za-z0-9_-]+\\.enc"))

    private fun requireEtag(value: String?): String {
        val etag = value?.trim()?.removeSurrounding("\"")
        require(!etag.isNullOrEmpty() && !etag.startsWith("W/") &&
            etag.none { it <= ' ' || it == '\u007f' || it == '"' }) { "云端对象缺少有效版本，已停止操作。" }
        return etag
    }
}
