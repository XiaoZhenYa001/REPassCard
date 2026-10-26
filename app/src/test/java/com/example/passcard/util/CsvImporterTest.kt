package com.example.passcard.util

import com.example.passcard.ui.screens.PasswordItem
import com.example.passcard.ui.screens.accountLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImporterTest {
    @Test
    fun missingPhoneColumnDoesNotCopyThePasswordIntoTheAccountLabel() {
        val result = CsvImporter.parseCsvContent("name,username,password\nExample,,secret")
        val entry = result.entries.single()

        assertTrue(result.headerDetected)
        assertEquals("secret", entry.password)
        assertEquals("", entry.phone)
        assertEquals("", entry.email)
        val item = PasswordItem(id = "imported", name = entry.service, username = entry.username,
            phone = entry.phone, email = entry.email, password = entry.password)
        assertEquals("", item.accountLabel())
    }

    @Test
    fun reorderedHeadersOnlyPopulateColumnsThatActuallyExist() {
        val entry = CsvImporter.parseCsvContent("password,name,email,username\nsecret,Example,me@example.com,user").entries.single()

        assertEquals("Example", entry.service)
        assertEquals("user", entry.username)
        assertEquals("me@example.com", entry.email)
        assertEquals("secret", entry.password)
        assertEquals("", entry.phone)
        assertEquals("", entry.note)
        assertEquals("", entry.category)
    }

    @Test
    fun unknownColumnsCannotBecomeMissingAccountFields() {
        val entry = CsvImporter.parseCsvContent("name,password,custom\nExample,secret,extra").entries.single()

        assertEquals("secret", entry.password)
        assertEquals("", entry.username)
        assertEquals("", entry.phone)
        assertEquals("", entry.email)
    }

    @Test
    fun quotedPasswordKeepsLeadingAndTrailingWhitespace() {
        val entry = CsvImporter.parseCsvContent("name,password\nExample,\"  secret\t \"").entries.single()
        assertEquals("  secret\t ", entry.password)
    }

    @Test
    fun unquotedPasswordKeepsLeadingAndTrailingWhitespace() {
        val entry = CsvImporter.parseCsvContent("name,password\nExample,  secret  ").entries.single()
        assertEquals("  secret  ", entry.password)
    }

    @Test
    fun nonEmptyWhitespacePasswordIsPreserved() {
        val result = CsvImporter.parseCsvContent("name,password\nExample,\"  \t \"")
        assertTrue(result.issues.isEmpty())
        assertEquals("  \t ", result.entries.single().password)
    }

    @Test
    fun legacyHeaderlessRowsKeepTheirColumnOrderAndPasswordCharacters() {
        val result = CsvImporter.parseCsvContent("Example, user , phone , email ,\"  secret  \", note , Work ")
        val entry = result.entries.single()

        assertFalse(result.headerDetected)
        assertEquals("user", entry.username)
        assertEquals("phone", entry.phone)
        assertEquals("email", entry.email)
        assertEquals("  secret  ", entry.password)
        assertEquals("note", entry.note)
        assertEquals("Work", entry.category)
    }

    @Test
    fun bomAndSemicolonRowsKeepQuotedSeparatorsAndEscapedQuotes() {
        val result = CsvImporter.parseCsvContent("\uFEFFname;username;password\r\nExample;user;\" a;b \"\"c\"\" \"\r\n")
        assertEquals(';', result.detectedDelimiter)
        assertEquals(" a;b \"c\" ", result.entries.single().password)
    }

    @Test
    fun quotedPasswordKeepsEmbeddedLineEndings() {
        val result = CsvImporter.parseCsvContent("name,password\r\nExample,\"first\r\nsecond\"\r\n")
        assertEquals("first\r\nsecond", result.entries.single().password)
    }

    @Test
    fun quotedCommasRemainInsideTheirOwnFields() {
        val entry = CsvImporter.parseCsvContent("name,username,password\n\"Example, Inc\",user,\"se,c\"\"ret\"").entries.single()
        assertEquals("Example, Inc", entry.service)
        assertEquals("se,c\"ret", entry.password)
        assertEquals("", entry.phone)
    }

    @Test
    fun passwordlessRecordsRemainImportable() {
        val result = CsvImporter.parseCsvContent("name,password\nExample,\"\"")
        assertTrue(result.issues.isEmpty())
        assertEquals("", result.entries.single().password)
    }

    @Test
    fun emptyServiceNamesStillProduceAnImportIssue() {
        val result = CsvImporter.parseCsvContent("name,password\n,secret")
        assertTrue(result.entries.isEmpty())
        assertEquals(1, result.issues.size)
    }
}
