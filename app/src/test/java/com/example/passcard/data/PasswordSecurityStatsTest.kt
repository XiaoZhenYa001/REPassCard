package com.example.passcard.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordSecurityStatsTest {
    @Test
    fun emptyVaultHasNoScore() {
        assertEquals(0, PasswordSecurityStats().score)
    }

    @Test
    fun healthyVaultHasFullScore() {
        val stats = PasswordSecurityStats(totalCount = 20)

        assertEquals(100, stats.score)
    }

    @Test
    fun penaltiesAreCalculatedFromRealRatios() {
        val stats = PasswordSecurityStats(
            totalCount = 10,
            weakCount = 2,
            reusedCount = 3,
            compromisedCount = 1
        )

        assertEquals(76, stats.score)
    }

    @Test
    fun scoreNeverDropsBelowZero() {
        val stats = PasswordSecurityStats(
            totalCount = 1,
            weakCount = 1,
            reusedCount = 1,
            compromisedCount = 1
        )

        assertEquals(0, stats.score)
    }
}
