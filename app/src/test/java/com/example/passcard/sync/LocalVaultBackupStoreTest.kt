package com.example.passcard.sync

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalVaultBackupStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun sameSecondSavesKeepSeparateCompleteFiles() {
        val directory = temporary.newFolder()
        var id = 0
        val store = LocalVaultBackupStore(directory, now = { 1000 }, newId = { "backup-${++id}" })
        val first = store.save("encrypted-first").getOrThrow()
        val second = store.save("encrypted-second").getOrThrow()
        assertNotEquals(first, second)
        assertEquals("encrypted-first", File(directory, first).readText())
        assertEquals("encrypted-second", File(directory, second).readText())
        assertEquals(2, store.list().getOrThrow().size)
        assertFalse(directory.listFiles()!!.any { it.name.endsWith(".tmp") })
    }

    @Test fun collisionCannotReplaceThePreviousBackup() {
        val directory = temporary.newFolder()
        val store = LocalVaultBackupStore(directory, now = { 1000 }, newId = { "same-id" })
        val name = store.save("original").getOrThrow()
        assertTrue(store.save("replacement").isFailure)
        assertEquals("original", File(directory, name).readText())
    }

    @Test fun clockRollbackCannotPruneTheRollbackCopyThatWasJustSaved() {
        val directory = temporary.newFolder()
        var time = 20_000L
        var id = 0
        val store = LocalVaultBackupStore(directory, now = { time }, newId = { "backup-${++id}" }, maxKeep = 2)
        store.save("previous-1").getOrThrow()
        time = 30_000L
        store.save("previous-2").getOrThrow()
        time = 1000L
        val rollback = store.save("current-before-restore").getOrThrow()
        assertEquals("current-before-restore", File(directory, rollback).readText())
        assertEquals(2, store.list().getOrThrow().size)
        assertTrue(store.list().getOrThrow().any { it.id == rollback })
    }

    @Test fun onlyTheLatestTenCompleteBackupsAreKept() {
        val directory = temporary.newFolder()
        var time = 0L
        val store = LocalVaultBackupStore(directory, now = { time }, newId = { "backup-$time" })
        val names = (1..12).map {
            time = it * 1000L
            store.save("backup-$it").getOrThrow()
        }
        val rows = store.list().getOrThrow()
        assertEquals(10, rows.size)
        assertEquals(names.last(), rows.first().id)
        assertFalse(File(directory, names.first()).exists())
        assertFalse(File(directory, names[1]).exists())
    }

    @Test fun legacyNamesAreReadableWithoutInventingTheirMetadata() {
        val directory = temporary.newFolder()
        val legacy = File(directory, "20261013_091000.enc").apply { writeText("encrypted") }
        val store = LocalVaultBackupStore(directory)
        val row = store.list().getOrThrow().single()
        assertEquals(legacy.name, row.id)
        assertEquals(legacy.length(), row.sizeBytes)
        assertNull(row.itemCount)
        assertNull(row.vaultRevision)
        assertEquals("encrypted", store.read(row).getOrThrow().toString(Charsets.UTF_8))
    }

    @Test fun listIgnoresTemporaryAndUnknownFiles() {
        val directory = temporary.newFolder()
        File(directory, ".backup-incomplete.tmp").writeText("partial")
        File(directory, "random.enc").writeText("unknown")
        assertTrue(LocalVaultBackupStore(directory).list().getOrThrow().isEmpty())
    }

    @Test fun pathTraversalCannotReadOutsideTheBackupDirectory() {
        val directory = temporary.newFolder()
        val store = LocalVaultBackupStore(directory)
        val row = VaultBackupSummary(VaultBackupSource.LOCAL, "../outside.enc", 0, 1, etag = "0:1")
        assertTrue(store.read(row).isFailure)
        assertTrue(store.read(row.copy(id = "C:\\outside.enc")).isFailure)
    }

    @Test fun changedAndDeletedSelectionRequireARefresh() {
        val directory = temporary.newFolder()
        val store = LocalVaultBackupStore(directory)
        val name = store.save("old").getOrThrow()
        val row = store.list().getOrThrow().single()
        File(directory, name).writeText("changed-size")
        assertTrue(store.read(row).isFailure)
        assertTrue(File(directory, name).delete())
        assertTrue(store.read(row).isFailure)
    }

    @Test fun sourceCannotBeConfusedWithACloudKey() {
        val directory = temporary.newFolder()
        val store = LocalVaultBackupStore(directory)
        store.save("encrypted").getOrThrow()
        val row = store.list().getOrThrow().single()
        assertTrue(store.read(row.copy(source = VaultBackupSource.CLOUD)).isFailure)
    }

    @Test fun emptyBackupCannotReplaceOrPruneExistingCopies() {
        val directory = temporary.newFolder()
        val store = LocalVaultBackupStore(directory)
        val name = store.save("encrypted").getOrThrow()
        assertTrue(store.save("").isFailure)
        assertEquals(listOf(name), store.list().getOrThrow().map { it.id })
    }

    @Test fun oversizedFilesAreRejectedBeforeReadingThem() {
        val directory = temporary.newFolder()
        val file = File(directory, "20261013_091000.enc")
        java.io.RandomAccessFile(file, "rw").use { it.setLength(LocalVaultBackupStore.MAX_BACKUP_BYTES + 1) }
        val store = LocalVaultBackupStore(directory)
        assertTrue(store.read(store.list().getOrThrow().single()).isFailure)
    }

    @Test fun restoringHistoryAlwaysAdvancesBeyondAllKnownRevisions() {
        assertEquals(10L, nextHistoryRestoreRevision(9, 2, 5))
        assertEquals(13L, nextHistoryRestoreRevision(4, 2, 12))
        assertEquals(6L, nextHistoryRestoreRevision(4, 5))
        assertTrue(runCatching { nextHistoryRestoreRevision(Long.MAX_VALUE, 1) }.isFailure)
    }

}
