package com.example.passcard.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses Android's real JSON implementation through the production codec entry points. */
@RunWith(AndroidJUnit4::class)
class JsonRoundTripTest {
    @Test fun nativeExportRoundTripsAllFieldsAndPasswordlessRecords() {
        val originals = listOf(
            ExportPasswordEntry("Mail", "", email = "me@example.com", password = ""),
            ExportPasswordEntry(" Example ", " user ", phone = " 123 ", email = " mail ",
                password = "  a\t\r\nb  ", note = " note\n\"quoted\" ", category = " Work ",
                iconType = "emoji", iconValue = "🔑")
        )
        val result = JsonImporter.parseJsonContent(JsonExporter.createJsonContent(originals))
        assertTrue(result.issues.isEmpty())
        assertEquals(originals.map { it.toContent() }, result.entries.map { it.toContent() })
    }

    @Test fun olderNativeExportDefaultsMissingIconFields() {
        val entry = JsonImporter.parseJsonContent("""{"app":"REPassCard","version":1,"passwords":[{"service":"Example","username":"user","password":""}]}""").entries.single()
        assertEquals("", entry.password)
        assertEquals("", entry.phone)
        assertEquals("", entry.email)
        assertEquals("generated", entry.iconType)
        assertEquals("", entry.iconValue)
    }

    @Test fun nativeMissingFieldsCannotBeFilledFromUnrelatedAliases() {
        val entry = JsonImporter.parseJsonContent("""{"app":"REPassCard","passwords":[{"service":"Example","username":"user","password":"secret","telephone_backup":"unexpected"}]}""").entries.single()
        assertEquals("", entry.phone)
        assertEquals("", entry.email)
    }

    @Test fun thirdPartyNestedLoginKeepsPasswordCharacters() {
        val entry = JsonImporter.parseJsonContent("""{"items":[{"name":"Example","login":{"username":"user","password":"  secret  "},"notes":"note"}]}""").entries.single()
        assertEquals("Example", entry.service)
        assertEquals("user", entry.username)
        assertEquals("  secret  ", entry.password)
        assertEquals("note", entry.note)
        assertEquals("", entry.phone)
    }

    @Test fun similarlyNamedUnknownFieldsCannotBecomeSecretsOrServiceNames() {
        val result = JsonImporter.parseJsonContent("""[{"name":"Example","password_hint":"hint"},{"username":"user","password":"secret"}]""")
        assertEquals("", result.entries.single().password)
        assertEquals(1, result.issues.size)
    }

    @Test fun literalNullStringIsNotDiscardedAsJsonNull() {
        val entry = JsonImporter.parseJsonContent("""[{"name":"Example","password":"null","phone":null}]""").entries.single()
        assertEquals("null", entry.password)
        assertEquals("", entry.phone)
    }

    private fun ExportPasswordEntry.toContent() = ImportRecordContent(service, username, phone, email,
        password, note, category, iconType, iconValue)
    private fun PasswordEntryCsv.toContent() = ImportRecordContent(service, username, phone, email,
        password, note, category, iconType, iconValue)
}
