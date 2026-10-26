package com.example.passcard.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportDeduplicationTest {
    private val original = ImportRecordContent(
        service = "Example", username = "user", email = "one@example.com",
        phone = "123", password = "secret", note = "note", category = "Work",
        iconType = "emoji", iconValue = "🔑"
    )

    @Test fun identicalRecordsAreSkippedInExistingVaultAndSameBatch() {
        val newRecord = original.copy(service = "Other")
        assertEquals(listOf(true, false, true), ImportDeduplication.duplicateFlags(
            listOf(original), listOf(original, newRecord, newRecord)
        ))
    }

    @Test fun differentEmailOrPhoneRemainsImportableWithSameServiceAndUsername() {
        assertEquals(listOf(false, false, false, false), ImportDeduplication.duplicateFlags(
            listOf(original), listOf(original.copy(email = "two@example.com"),
                original.copy(phone = "456"), original.copy(email = ""), original.copy(phone = ""))
        ))
    }

    @Test fun differentPasswordNoteCategoryOrIconIsNotDiscarded() {
        assertEquals(List(5) { false }, ImportDeduplication.duplicateFlags(
            listOf(original), listOf(original.copy(password = "new secret"),
                original.copy(note = "new note"), original.copy(category = "Home"),
                original.copy(iconValue = "🔒"), original.copy(iconType = "generated"))
        ))
    }

    @Test fun caseWhitespaceAndSeparatorCharactersDoNotMergeRecords() {
        val values = listOf(original.copy(username = "USER"), original.copy(username = " user "),
            original.copy(service = "Example|user", username = ""),
            original.copy(service = "Example", username = "user|"))
        assertEquals(List(4) { false }, ImportDeduplication.duplicateFlags(listOf(original), values))
    }
}
