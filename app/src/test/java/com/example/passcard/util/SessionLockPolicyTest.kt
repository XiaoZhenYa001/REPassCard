package com.example.passcard.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLockPolicyTest {
    @Test
    fun immediateTimeoutLocksWhenAppResumes() {
        assertTrue(
            SessionLockPolicy.shouldLock(
                hasMasterPassword = true,
                backgroundedAtMillis = 1_000L,
                resumedAtMillis = 1_000L,
                timeoutSeconds = 0
            )
        )
    }

    @Test
    fun sessionStaysUnlockedBeforeTimeout() {
        assertFalse(
            SessionLockPolicy.shouldLock(
                hasMasterPassword = true,
                backgroundedAtMillis = 1_000L,
                resumedAtMillis = 30_999L,
                timeoutSeconds = 30
            )
        )
    }

    @Test
    fun sessionLocksAtTimeoutBoundary() {
        assertTrue(
            SessionLockPolicy.shouldLock(
                hasMasterPassword = true,
                backgroundedAtMillis = 1_000L,
                resumedAtMillis = 31_000L,
                timeoutSeconds = 30
            )
        )
    }

    @Test
    fun vaultWithoutMasterPasswordNeverLocks() {
        assertFalse(
            SessionLockPolicy.shouldLock(
                hasMasterPassword = false,
                backgroundedAtMillis = 1_000L,
                resumedAtMillis = 999_999L,
                timeoutSeconds = 0
            )
        )
    }
}
