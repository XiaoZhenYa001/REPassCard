package com.example.passcard.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.passcard.data.AppDatabase
import com.example.passcard.util.SessionLockPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SessionStateViewModel : ViewModel() {
    var isInitialized: Boolean = false
        private set

    var isUnlocked by mutableStateOf(false)
        private set

    private var backgroundedAt: Long? = null
    private var autoLockJob: Job? = null
    private var vaultStoreOwner by mutableStateOf(VaultSessionStoreOwner())

    val vaultViewModelStoreOwner: ViewModelStoreOwner
        get() = vaultStoreOwner

    fun initialize(hasMasterPassword: Boolean) {
        if (isInitialized) return
        isUnlocked = !hasMasterPassword
        isInitialized = true
    }

    fun unlockVault() {
        cancelAutoLock()
        backgroundedAt = null
        isUnlocked = true
    }

    fun moveToBackground(
        hasMasterPassword: Boolean,
        elapsedRealtime: Long,
        timeoutSeconds: Int
    ) {
        cancelAutoLock()
        if (!hasMasterPassword) {
            backgroundedAt = null
            return
        }

        backgroundedAt = elapsedRealtime
        val normalizedTimeout = timeoutSeconds.coerceIn(0, MAX_AUTO_LOCK_SECONDS)
        if (normalizedTimeout == 0) {
            lockVault()
            return
        }

        autoLockJob = viewModelScope.launch {
            delay(normalizedTimeout * 1_000L)
            autoLockJob = null
            lockVault()
        }
    }

    fun moveToForeground(
        hasMasterPassword: Boolean,
        elapsedRealtime: Long,
        timeoutSeconds: Int
    ) {
        cancelAutoLock()
        val leftAt = backgroundedAt
        backgroundedAt = null
        if (
            SessionLockPolicy.shouldLock(
                hasMasterPassword = hasMasterPassword,
                backgroundedAtMillis = leftAt,
                resumedAtMillis = elapsedRealtime,
                timeoutSeconds = timeoutSeconds
            )
        ) {
            lockVault()
        }
    }

    fun lockVault() {
        cancelAutoLock()
        backgroundedAt = null
        isUnlocked = false

        val expiredStore = vaultStoreOwner
        vaultStoreOwner = VaultSessionStoreOwner()
        expiredStore.clear()
        AppDatabase.closeInstance()
    }

    override fun onCleared() {
        cancelAutoLock()
        vaultStoreOwner.clear()
        AppDatabase.closeInstance()
        super.onCleared()
    }

    private fun cancelAutoLock() {
        autoLockJob?.cancel()
        autoLockJob = null
    }

    private companion object {
        const val MAX_AUTO_LOCK_SECONDS = 300
    }
}

private class VaultSessionStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    fun clear() {
        viewModelStore.clear()
    }
}
