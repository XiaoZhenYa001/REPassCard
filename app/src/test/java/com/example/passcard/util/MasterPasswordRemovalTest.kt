package com.example.passcard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MasterPasswordRemovalTest {
    @Test
    fun incorrectPasswordCannotRemoveProtection() {
        var cleared = false
        assertFalse(MasterPasswordRemoval.remove("wrong", { false }, { cleared = true }))
        assertFalse(cleared)
    }

    @Test
    fun emptyPasswordCannotRemoveProtectionEvenWhenVerifierAcceptsIt() {
        var verified = false
        var cleared = false
        assertFalse(MasterPasswordRemoval.remove("", { verified = true; true }, { cleared = true }))
        assertFalse(verified)
        assertFalse(cleared)
    }

    @Test
    fun validCurrentPasswordIsVerifiedBeforeProtectionChanges() {
        val calls = mutableListOf<String>()
        assertTrue(MasterPasswordRemoval.remove("  current  ", {
            assertEquals("  current  ", it)
            calls += "verify"
            true
        }, { calls += "clear" }))
        assertEquals(listOf("verify", "clear"), calls)
    }

    @Test
    fun realKdfRejectsAnIncorrectPasswordBeforeRemoval() {
        val record = MasterPasswordKdf.create("correct", iterations = 10_000)
        var cleared = false
        assertFalse(MasterPasswordRemoval.remove("incorrect", {
            MasterPasswordKdf.verify(it, record)
        }, { cleared = true }))
        assertFalse(cleared)
        assertTrue(MasterPasswordRemoval.remove("correct", {
            MasterPasswordKdf.verify(it, record)
        }, { cleared = true }))
        assertTrue(cleared)
    }

    @Test
    fun verifierFailureCannotReachTheMutation() {
        var cleared = false
        try {
            MasterPasswordRemoval.remove("current", { error("unavailable") }, { cleared = true })
        } catch (_: IllegalStateException) {
            // The screen reports this error without treating the operation as completed.
        }
        assertFalse(cleared)
    }
}
