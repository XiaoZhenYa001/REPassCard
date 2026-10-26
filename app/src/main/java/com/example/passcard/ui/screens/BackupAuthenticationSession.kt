package com.example.passcard.ui.screens

import java.util.concurrent.atomic.AtomicLong

/** A biometric callback authorizes one current foreground operation, never a later session. */
internal class BackupAuthenticationSession {
    private val counter = AtomicLong(0L)
    val version: Long get() = counter.get()
    fun begin(): Long = counter.incrementAndGet()
    fun isCurrent(token: Long): Boolean = counter.get() == token
    fun claim(token: Long): Boolean = counter.compareAndSet(token, token + 1L)
    fun invalidate() { counter.incrementAndGet() }
}
