package com.example.passcard.ui.screens

import org.junit.Assert.*
import org.junit.Test

class BackupAuthenticationSessionTest {
    @Test fun backgroundingInvalidatesPendingAuthenticationAndLateCacheSetup() {
        val session = BackupAuthenticationSession()
        val operationToken = session.version
        val promptToken = session.begin()
        session.invalidate()
        assertFalse(session.isCurrent(operationToken))
        assertFalse(session.claim(promptToken))
    }

    @Test fun terminalCallbacksCanAuthorizeOnlyOnce() {
        val session = BackupAuthenticationSession()
        val token = session.begin()
        assertTrue(session.claim(token))
        assertFalse(session.claim(token))
    }

    @Test fun aNewPromptCannotReuseAnOlderPromptCallback() {
        val session = BackupAuthenticationSession()
        val old = session.begin()
        val current = session.begin()
        assertFalse(session.claim(old))
        assertTrue(session.claim(current))
    }
}
