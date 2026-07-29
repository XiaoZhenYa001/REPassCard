package com.example.passcard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStateViewModelTest {
    @Test
    fun lockingVaultClearsSessionViewModelsAndReplacesTheirStore() {
        val session = SessionStateViewModel().apply { initialize(hasMasterPassword = false) }
        val initialOwner = session.vaultViewModelStoreOwner
        val trackedViewModel = ViewModelProvider(initialOwner)[TrackingViewModel::class.java]

        session.lockVault()

        assertFalse(session.isUnlocked)
        assertTrue(trackedViewModel.wasCleared)
        assertNotSame(initialOwner, session.vaultViewModelStoreOwner)
    }

    @Test
    fun unlockingVaultKeepsTheFreshSessionStore() {
        val session = SessionStateViewModel().apply { initialize(hasMasterPassword = true) }
        val initialOwner = session.vaultViewModelStoreOwner

        session.lockVault()
        val freshOwner = session.vaultViewModelStoreOwner
        session.unlockVault()

        assertTrue(session.isUnlocked)
        assertNotSame(initialOwner, freshOwner)
        assertTrue(freshOwner === session.vaultViewModelStoreOwner)
    }

    class TrackingViewModel : ViewModel() {
        var wasCleared = false
            private set

        override fun onCleared() {
            wasCleared = true
        }
    }
}
