package com.example.passcard.ui.screens

import com.example.passcard.crypto.BackupPasswordError
import org.junit.Assert.*
import org.junit.Test

class BackupPasswordDialogStateTest {
    @Test fun cachedPasswordUsesBiometricsWithoutPuttingASecretInTheForm() {
        val state = BackupPasswordDialogState()
        state.open(upload = true, hasSavedPassword = true)
        assertTrue(state.visible)
        assertFalse(state.manualEntry)
        assertEquals("", state.password)
        assertEquals("", state.confirmation)
        assertNull(state.passwordError())
    }

    @Test fun firstUploadRequiresVerbatimMatchingPasswords() {
        val state = BackupPasswordDialogState()
        state.open(upload = true, hasSavedPassword = false)
        state.password = " A private backup Password  "
        state.confirmation = state.password.trim()
        assertEquals(BackupPasswordError.MISMATCH, state.passwordError())
        state.confirmation = state.password
        assertNull(state.passwordError())
        assertEquals(" A private backup Password  ", state.password)
    }

    @Test fun finishClearsCredentialsAndConfirmationFlags() {
        val state = BackupPasswordDialogState()
        state.open(upload = false, hasSavedPassword = false)
        state.password = " A private backup Password  "
        state.confirmation = state.password
        state.confirmRestore = true
        state.confirmEmptyOverwrite = true
        state.errorMessage = "failed"
        state.finish()
        assertFalse(state.visible)
        assertEquals("", state.password)
        assertEquals("", state.confirmation)
        assertFalse(state.confirmRestore)
        assertFalse(state.confirmEmptyOverwrite)
        assertNull(state.errorMessage)
    }

    @Test fun switchingToPasswordNeverReusesASecretFromAnotherOperation() {
        val state = BackupPasswordDialogState()
        state.open(upload = false, hasSavedPassword = true)
        state.password = "previous operation secret"
        state.confirmation = "previous confirmation"
        state.errorMessage = "authentication failed"
        state.usePassword()
        assertTrue(state.manualEntry)
        assertEquals("", state.password)
        assertEquals("", state.confirmation)
        assertNull(state.errorMessage)
    }

    @Test fun reopeningRestoreResetsAnEarlierUpload() {
        val state = BackupPasswordDialogState()
        state.open(upload = true, hasSavedPassword = false)
        state.password = "previous operation secret"
        state.confirmEmptyOverwrite = true
        state.open(upload = false, hasSavedPassword = true)
        assertFalse(state.isUpload)
        assertFalse(state.manualEntry)
        assertFalse(state.confirmEmptyOverwrite)
        assertFalse(state.confirmRestore)
        assertEquals("", state.password)
    }

    @Test fun backgroundClearingRemovesTextWithoutAuthorizingAnOperation() {
        val state = BackupPasswordDialogState()
        state.open(upload = true, hasSavedPassword = false)
        state.password = " A private backup Password  "
        state.confirmation = state.password
        state.clearCredentials()
        assertEquals("", state.password)
        assertEquals("", state.confirmation)
        assertNotNull(state.passwordError())
    }
}
