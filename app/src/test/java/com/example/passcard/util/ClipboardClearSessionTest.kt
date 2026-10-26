package com.example.passcard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardClearSessionTest {
    @Test
    fun foregroundCopyIsClearedAtItsDeadline() {
        val session = scheduled()
        var clears = 0
        assertFalse(session.clearIfDue(30_999, true, { "ours" }, { clears++ }))
        assertTrue(session.clearIfDue(31_000, true, { "ours" }, { clears++ }))
        assertEquals(1, clears)
        assertNull(session.remainingDelayMillis(31_000))
    }

    @Test
    fun backgroundDeadlineNeverReadsOrClearsClipboard() {
        val session = scheduled()
        var reads = 0
        var clears = 0
        assertFalse(session.clearIfDue(60_000, false, { reads++; "ours" }, { clears++ }))
        assertEquals(0, reads)
        assertEquals(0, clears)
        assertEquals(0L, session.remainingDelayMillis(60_000))
        assertTrue(session.clearIfDue(60_001, true, { "ours" }, { clears++ }))
        assertEquals(1, clears)
    }

    @Test
    fun laterCopyFromAnotherApplicationIsPreserved() {
        val session = scheduled()
        var clears = 0
        assertFalse(session.clearIfDue(60_000, true, { "another-app" }, { clears++ }))
        assertEquals(0, clears)
        assertNull(session.remainingDelayMillis(60_000))
    }

    @Test
    fun replacementWithoutAnOwnershipMarkerIsPreserved() {
        val session = scheduled()
        var clears = 0
        assertFalse(session.clearIfDue(60_000, true, { "" }, { clears++ }))
        assertEquals(0, clears)
        assertNull(session.remainingDelayMillis(60_000))
    }

    @Test
    fun restrictedClipboardReadCanBeRetriedWhenFocusReturns() {
        val session = scheduled()
        assertFalse(session.clearIfDue(60_000, true, { null }, { error("cannot clear") }))
        assertEquals(0L, session.remainingDelayMillis(60_000))
        assertTrue(session.clearIfDue(60_001, true, { "ours" }, {}))
    }

    @Test
    fun failedClearRetainsOwnershipForAForegroundRetry() {
        val session = scheduled()
        try {
            session.clearIfDue(60_000, true, { "ours" }, { throw SecurityException() })
        } catch (_: SecurityException) {
        }
        assertEquals(0L, session.remainingDelayMillis(60_000))
        assertTrue(session.clearIfDue(60_001, true, { "ours" }, {}))
    }

    @Test
    fun copyingAgainReplacesTheOldDeadlineAndOwnership() {
        val session = scheduled()
        session.schedule("new-copy", 20_000, 30_000)
        assertFalse(session.clearIfDue(31_000, true, { "new-copy" }, { error("too early") }))
        assertEquals(19_000L, session.remainingDelayMillis(31_000))
        assertTrue(session.clearIfDue(50_000, true, { "new-copy" }, {}))
    }

    @Test
    fun disablingCleanupCancelsPendingOwnership() {
        val session = scheduled()
        session.cancel()
        assertFalse(session.clearIfDue(60_000, true, { "ours" }, { error("cancelled") }))
        assertNull(session.remainingDelayMillis(60_000))
    }

    @Test
    fun aNewCopyDuringCleanupIsNotDiscardedByTheOlderCompletion() {
        val session = scheduled()
        assertTrue(session.clearIfDue(31_000, true, { "ours" }, {
            session.schedule("next-copy", 31_000, 30_000)
        }))
        assertEquals(30_000L, session.remainingDelayMillis(31_000))
    }

    @Test
    fun aNewCopyWhileReadingOwnershipPreventsTheOlderClear() {
        val session = scheduled()
        var clears = 0
        assertFalse(session.clearIfDue(31_000, true, {
            session.schedule("next-copy", 31_000, 30_000)
            "ours"
        }, { clears++ }))
        assertEquals(0, clears)
        assertEquals(30_000L, session.remainingDelayMillis(31_000))
    }

    private fun scheduled() = ClipboardClearSession().apply { schedule("ours", 1_000, 30_000) }
}
