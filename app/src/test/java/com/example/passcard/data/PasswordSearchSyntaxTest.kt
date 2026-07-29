package com.example.passcard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordSearchSyntaxTest {
    @Test
    fun plainQuerySearchesEveryField() {
        val result = PasswordSearchSyntax.parse("  WeChat  ")

        assertEquals("WeChat", result.keyword)
        assertNull(result.field)
        assertEquals("WeChat%", result.prefix)
        assertEquals("%WeChat%", result.contains)
    }

    @Test
    fun fieldQueryKeepsRemainingTextAsKeyword() {
        val result = PasswordSearchSyntax.parse("/t username user name")

        assertEquals(PasswordSearchField.USERNAME, result.field)
        assertEquals("user name", result.keyword)
    }

    @Test
    fun incompleteFieldQueryProducesNoResults() {
        val result = PasswordSearchSyntax.parse("/t name")

        assertEquals("", result.keyword)
        assertNull(result.field)
    }

    @Test
    fun unknownFieldFallsBackToGlobalSearch() {
        val result = PasswordSearchSyntax.parse("/t custom value")

        assertEquals("custom value", result.keyword)
        assertNull(result.field)
    }

    @Test
    fun plainQueryStartingWithTCommandCharactersStaysGlobal() {
        val result = PasswordSearchSyntax.parse("/telegram")

        assertEquals("/telegram", result.keyword)
        assertNull(result.field)
    }

    @Test
    fun likeWildcardCharactersAreEscapedForLiteralSearch() {
        val result = PasswordSearchSyntax.parse("50%_off\\path")

        assertEquals("50%_off\\path", result.keyword)
        assertEquals("50\\%\\_off\\\\path%", result.prefix)
        assertEquals("%50\\%\\_off\\\\path%", result.contains)
    }
}
