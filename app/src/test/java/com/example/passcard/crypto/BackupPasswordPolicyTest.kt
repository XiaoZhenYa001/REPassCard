package com.example.passcard.crypto

import org.junit.Assert.*
import org.junit.Test

class BackupPasswordPolicyTest {
    @Test fun requiresFifteenCharactersWhenChoosingANewPassword() {
        assertEquals(BackupPasswordError.TOO_SHORT, BackupPasswordPolicy.validationError("short password", "short password"))
        assertNull(BackupPasswordPolicy.validationError("fifteen letters", "fifteen letters"))
    }

    @Test fun refusesBlankNewPasswordsWithoutTrimmingValidPasswords() {
        listOf("", " ".repeat(15), "\t\n".repeat(15)).forEach { blank ->
            assertEquals(BackupPasswordError.BLANK, BackupPasswordPolicy.validationError(blank, blank))
        }
        val padded = "  Violet lanterns  "
        assertNull(BackupPasswordPolicy.validationError(padded, padded))
    }

    @Test fun countsUnicodeCodePointsRatherThanUtf16Units() {
        val fourteen = "🔐".repeat(14)
        assertEquals(BackupPasswordError.TOO_SHORT, BackupPasswordPolicy.validationError(fourteen, fourteen))
        val fifteen = "🔐".repeat(15)
        assertNull(BackupPasswordPolicy.validationError(fifteen, fifteen))
    }

    @Test fun doesNotTrimOrFoldCaseWhenConfirming() {
        val password = " My backup password "
        assertNull(BackupPasswordPolicy.validationError(password, password))
        assertEquals(BackupPasswordError.MISMATCH, BackupPasswordPolicy.validationError(password, password.trim()))
        assertEquals(BackupPasswordError.MISMATCH, BackupPasswordPolicy.validationError(password, password.lowercase()))
    }

    @Test fun doesNotNormalizeUnicode() {
        val composed = "é".repeat(15)
        val decomposed = "e\u0301".repeat(15)
        assertEquals(BackupPasswordError.MISMATCH, BackupPasswordPolicy.validationError(composed, decomposed))
    }

    @Test fun acceptsUpperBoundaryAndRejectsOverlongInput() {
        val maximum = "🔐".repeat(BackupPasswordPolicy.MAX_CODE_POINTS)
        assertNull(BackupPasswordPolicy.validationError(maximum, maximum))
        assertEquals(BackupPasswordError.TOO_LONG, BackupPasswordPolicy.validationError(maximum + "x", maximum + "x"))
        assertThrows(IllegalArgumentException::class.java) { BackupPasswordPolicy.requireUsablePassword(maximum + "x") }
    }

    @Test fun decryptionDoesNotApplyTheNewPasswordMinimum() {
        BackupPasswordPolicy.requireUsablePassword("a")
        BackupPasswordPolicy.requireUsablePassword("  ")
        assertThrows(IllegalArgumentException::class.java) { BackupPasswordPolicy.requireUsablePassword("") }
    }
}
