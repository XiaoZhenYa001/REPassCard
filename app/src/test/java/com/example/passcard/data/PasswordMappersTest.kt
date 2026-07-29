package com.example.passcard.data

import com.example.passcard.ui.screens.PasswordItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordMappersTest {
    @Test
    fun entityRoundTripPreservesEveryPersistedField() {
        val source = PasswordEntity(
            id = "record-id",
            name = "Example",
            username = "user",
            phone = "13800000000",
            email = "user@example.com",
            password = "secret",
            category = "Work",
            note = "note",
            iconType = "emoji",
            iconValue = "key",
            createdAt = 100L,
            updatedAt = 200L,
            revision = 3L,
            deviceId = "device-id",
            deletedAt = 300L
        )

        assertEquals(source, source.toPasswordItem().toPasswordEntity())
    }

    @Test
    fun uiRoundTripPreservesSyncMetadata() {
        val source = PasswordItem(
            id = "record-id",
            name = "Example",
            username = "user",
            password = "secret",
            createdAt = 10L,
            updatedAt = 20L,
            revision = 4L,
            deviceId = "device-id",
            deletedAt = null
        )

        assertEquals(source, source.toPasswordEntity().toPasswordItem())
    }
}
