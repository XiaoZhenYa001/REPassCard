package com.example.passcard.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordAccountLabelTest {
    private val item = PasswordItem(id = "phone-only", name = "Account", username = "", password = "", phone = "13800138000")

    @Test
    fun phoneOnlyImportedRecordsRemainIdentifiable() {
        assertEquals("13800138000", item.accountLabel())
    }

    @Test
    fun blankAccountFieldsDoNotHideAvailableIdentifiers() {
        assertEquals("13800138000", item.copy(email = " ", username = " ").accountLabel())
        assertEquals("username", item.copy(username = "username").accountLabel())
        assertEquals("a@example.com", item.copy(email = "a@example.com", username = "username").accountLabel())
    }
}
