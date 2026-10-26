package com.example.passcard.crypto

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Base64

class VaultIconArchiveTest {
    private val firstImage = byteArrayOf(82, 73, 70, 70, 1, 2, 3, 4)

    @Test fun imageContentsAreEmbeddedAndIdenticalImagesShareOneAsset() {
        val storage = MemoryIcons(mapOf("first" to firstImage, "second" to firstImage))
        val payload = VaultIconArchive(storage).prepareExport(listOf(record("1", "image", "first"), record("2", "image", "second")))
        assertEquals(1, payload.icons.size)
        assertArrayEquals(firstImage, Base64.getDecoder().decode(payload.icons.single().contentBase64))
        assertEquals(payload.items[0].iconAssetId, payload.items[1].iconAssetId)
        assertEquals("", payload.items[0].iconValue)
    }

    @Test fun emojiAndGeneratedIconsKeepTheirValues() {
        val records = listOf(record("1", "emoji", "🔑"), record("2", "generated", "letter"))
        val payload = VaultIconArchive(null).prepareExport(records)
        assertEquals(records, payload.items)
        assertTrue(payload.icons.isEmpty())
        assertEquals(records, VaultIconArchive(null).prepareRestore(payload).records)
    }

    @Test fun unavailableImageFailsExportRatherThanSilentlyDroppingIt() {
        assertThrows(IllegalArgumentException::class.java) {
            VaultIconArchive(MemoryIcons()).prepareExport(listOf(record("1", "image", "missing")))
        }
    }

    @Test fun oversizedImageFailsBeforeExporting() {
        val storage = MemoryIcons(mapOf("large" to ByteArray(VaultIconArchive.MAX_ICON_BYTES + 1)))
        assertThrows(IllegalArgumentException::class.java) {
            VaultIconArchive(storage).prepareExport(listOf(record("1", "image", "large")))
        }
    }

    @Test fun totalImageLimitCountsUniqueContentsAndStopsExport() {
        val images = (1..9).associate { "image$it" to ByteArray(VaultIconArchive.MAX_ICON_BYTES).also { bytes -> bytes[0] = it.toByte() } }
        assertThrows(IllegalArgumentException::class.java) {
            VaultIconArchive(MemoryIcons(images)).prepareExport(images.keys.map { record(it, "image", it) })
        }
    }

    @Test fun crossDeviceRestoreWritesImageContentsToNewLocalReferences() {
        val payload = VaultIconArchive(MemoryIcons(mapOf("old-device" to firstImage)))
            .prepareExport(listOf(record("1", "image", "old-device")))
        val newDevice = MemoryIcons()
        val restored = VaultIconArchive(newDevice).prepareRestore(payload)
        val record = restored.records.single()
        assertEquals("image", record.iconType)
        assertEquals("restored:1", record.iconValue)
        assertNull(record.iconAssetId)
        assertArrayEquals(firstImage, newDevice.saved.single())
        assertTrue(restored.warnings.isEmpty())
    }

    @Test fun modifiedAssetHashFailsBeforeAnyFilesAreWritten() {
        val payload = VaultIconArchive(MemoryIcons(mapOf("original" to firstImage)))
            .prepareExport(listOf(record("1", "image", "original")))
        val altered = payload.copy(icons = payload.icons.map { it.copy(contentBase64 = Base64.getEncoder().encodeToString(byteArrayOf(0))) })
        val storage = MemoryIcons()
        assertThrows(IllegalArgumentException::class.java) { VaultIconArchive(storage).prepareRestore(altered) }
        assertTrue(storage.saved.isEmpty())
    }

    @Test fun missingDeclaredAssetFailsEvenWhenAnOldReferenceCouldBeRead() {
        val storage = MemoryIcons(mapOf("old" to firstImage))
        val payload = VaultPayload(listOf(record("1", "image", "old").copy(iconAssetId = "0".repeat(64))))
        assertThrows(IllegalArgumentException::class.java) { VaultIconArchive(storage).prepareRestore(payload) }
        assertTrue(storage.saved.isEmpty())
    }

    @Test fun assetIdentifiersCannotBePaths() {
        val storage = MemoryIcons()
        val asset = VaultIconAsset("../../outside", Base64.getEncoder().encodeToString(firstImage))
        val payload = VaultPayload(listOf(record("1", "image", "").copy(iconAssetId = asset.id)), listOf(asset))
        assertThrows(IllegalArgumentException::class.java) { VaultIconArchive(storage).prepareRestore(payload) }
        assertTrue(storage.saved.isEmpty())
    }

    @Test fun duplicateAssetIdentifiersAreRejected() {
        val payload = VaultIconArchive(MemoryIcons(mapOf("image" to firstImage)))
            .prepareExport(listOf(record("1", "image", "image")))
        val storage = MemoryIcons()
        assertThrows(IllegalArgumentException::class.java) {
            VaultIconArchive(storage).prepareRestore(payload.copy(icons = payload.icons + payload.icons))
        }
        assertTrue(storage.saved.isEmpty())
    }

    @Test fun oversizedEncodedAssetIsRejectedBeforeFilesAreWritten() {
        val storage = MemoryIcons()
        val asset = VaultIconAsset("0".repeat(64), "A".repeat(VaultIconArchive.MAX_ENCODED_ICON_LENGTH + 1))
        assertThrows(IllegalArgumentException::class.java) {
            VaultIconArchive(storage).prepareRestore(VaultPayload(emptyList(), listOf(asset)))
        }
        assertTrue(storage.saved.isEmpty())
    }

    @Test fun decodedImageLimitIsCheckedEvenWhenBase64LengthFits() {
        val storage = MemoryIcons()
        val bytes = ByteArray(VaultIconArchive.MAX_ICON_BYTES + 1)
        val asset = VaultIconAsset(iconContentHash(bytes), Base64.getEncoder().encodeToString(bytes))
        assertThrows(IllegalArgumentException::class.java) {
            VaultIconArchive(storage).prepareRestore(VaultPayload(emptyList(), listOf(asset)))
        }
        assertTrue(storage.saved.isEmpty())
    }

    @Test fun invalidBase64CannotProduceAFile() {
        val storage = MemoryIcons()
        val asset = VaultIconAsset("0".repeat(64), "invalid!")
        assertThrows(IllegalArgumentException::class.java) {
            VaultIconArchive(storage).prepareRestore(VaultPayload(emptyList(), listOf(asset)))
        }
        assertTrue(storage.saved.isEmpty())
    }

    @Test fun oldRecordWithoutIconMetadataStillUsesGeneratedIcon() {
        val restored = VaultIconArchive(null).prepareRestore(VaultPayload(listOf(record("1"))))
        assertEquals("generated", restored.records.single().iconType)
        assertEquals("", restored.records.single().iconValue)
        assertTrue(restored.warnings.isEmpty())
    }

    @Test fun oldMissingImageReferenceFallsBackWithAnExplanation() {
        val restored = VaultIconArchive(MemoryIcons()).prepareRestore(VaultPayload(listOf(record("1", "image", "old-device"))))
        assertEquals("generated", restored.records.single().iconType)
        assertEquals("", restored.records.single().iconValue)
        assertEquals(1, restored.warnings.size)
    }

    @Test fun oldAccessibleImageKeepsItsMetadata() {
        val original = record("1", "image", "existing")
        val restored = VaultIconArchive(MemoryIcons(mapOf("existing" to firstImage))).prepareRestore(VaultPayload(listOf(original)))
        assertEquals(original, restored.records.single())
        assertTrue(restored.warnings.isEmpty())
    }

    private fun record(id: String, type: String = "generated", value: String = "") = VaultPasswordRecord(
        id, "Example", "user", "", "", "secret", "Work", "note", 1, 2,
        iconType = type, iconValue = value)

    private class MemoryIcons(private val readable: Map<String, ByteArray> = emptyMap()) : VaultIconStorage {
        val saved = mutableListOf<ByteArray>()
        override fun openLocalIcon(reference: String): InputStream? = readable[reference]?.let { ByteArrayInputStream(it) }
        override fun storeRestoredIcon(bytes: ByteArray): String {
            saved += bytes.copyOf()
            return "restored:${saved.size}"
        }
    }
}
