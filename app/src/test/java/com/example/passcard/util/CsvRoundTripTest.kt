package com.example.passcard.util

import java.io.StringWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvRoundTripTest {
    @Test fun exportedRecordsRoundTripAllUserContentIncludingEmptyPassword() {
        val originals = listOf(
            ExportPasswordEntry(service = "Mail", username = "", email = "me@example.com", password = ""),
            ExportPasswordEntry(service = " Example, Inc ", username = " user ", phone = " phone ",
                email = " mail ", password = "  pass\t\r\nword  ",
                note = "  line one\rline two\n\"quoted\"  ", category = " Work ",
                iconType = "emoji", iconValue = "🔑")
        )
        val writer = StringWriter()
        CsvExporter.writeCsv(writer, originals)
        val result = CsvImporter.parseCsvContent(writer.toString())
        assertTrue(result.issues.isEmpty())
        assertEquals(originals.map { it.toContent() }, result.entries.map { it.toContent() })
    }

    @Test fun carriageReturnInsideExportedFieldCannotSplitARecord() {
        val writer = StringWriter()
        CsvExporter.writeCsv(writer, listOf(ExportPasswordEntry("Example", "user", password = "a\rb")))
        val result = CsvImporter.parseCsvContent(writer.toString())
        assertEquals(1, result.entries.size)
        assertEquals("a\rb", result.entries.single().password)
    }

    @Test fun olderSevenColumnNativeExportPreservesNoteWhitespace() {
        val result = CsvImporter.parseCsvContent("服务,用户名,手机号,邮箱,密码,备注,分类\nExample,user,,,secret,  note  ,Work")
        assertEquals("  note  ", result.entries.single().note)
        assertEquals("generated", result.entries.single().iconType)
    }

    private fun ExportPasswordEntry.toContent() = ImportRecordContent(service, username, phone, email,
        password, note, category, iconType, iconValue)
    private fun PasswordEntryCsv.toContent() = ImportRecordContent(service, username, phone, email,
        password, note, category, iconType, iconValue)
}
