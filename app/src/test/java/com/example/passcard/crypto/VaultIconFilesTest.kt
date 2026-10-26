package com.example.passcard.crypto

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class VaultIconFilesTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun restoredFilesUseContentNamesAndRepeatedRestoresReuseTheFile() {
        val directory = temp.newFolder("icons")
        val store = VaultIconFiles(directory)
        val bytes = byteArrayOf(1, 2, 3)
        val first = store.store(bytes)
        val second = store.store(bytes)
        assertEquals(first, second)
        assertTrue(first.name.matches(Regex("icon_[0-9a-f]{64}\\.bin")))
        assertEquals(directory.canonicalFile, first.canonicalFile.parentFile)
        assertArrayEquals(bytes, first.readBytes())
        assertEquals(1, directory.listFiles()!!.size)
        assertArrayEquals(bytes, store.openOwnedReference(first.toURI().toString())!!.use { it.readBytes() })
    }

    @Test fun sensitiveFilesAndTraversalOutsideTheIconDirectoryAreNeverRead() {
        val directory = temp.newFolder("icons")
        val secret = temp.newFile("database-secret")
        secret.writeText("sensitive")
        val store = VaultIconFiles(directory)
        assertNull(store.openOwnedReference(secret.toURI().toString()))
        assertNull(store.openOwnedReference(File(directory, "../database-secret").toURI().toString()))
        assertNull(store.openOwnedReference("https://example.com/image"))
        assertNull(store.openOwnedReference("file://elsewhere/image"))
    }

    @Test fun corruptedExistingFileIsNotOverwrittenOrDeleted() {
        val directory = temp.newFolder("icons")
        val store = VaultIconFiles(directory)
        val bytes = byteArrayOf(1, 2, 3)
        val saved = store.store(bytes)
        saved.writeText("corrupt")
        assertThrows(IllegalArgumentException::class.java) { store.store(bytes) }
        assertEquals("corrupt", saved.readText())
    }

    @Test fun boundedReadRejectsEmptyAndOversizedStreams() {
        assertThrows(IllegalArgumentException::class.java) { readBoundedIcon(ByteArrayInputStream(byteArrayOf())) }
        assertThrows(IllegalArgumentException::class.java) { readBoundedIcon(ByteArrayInputStream(ByteArray(11)), 10) }
        assertEquals(10, readBoundedIcon(ByteArrayInputStream(ByteArray(10)), 10).size)
    }
}
