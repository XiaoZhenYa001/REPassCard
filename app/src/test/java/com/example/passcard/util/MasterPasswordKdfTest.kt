package com.example.passcard.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MasterPasswordKdfTest {
    @Test
    fun matchingPasswordVerifies() {
        val record = recordFor("correct horse battery staple", saltByte = 1)

        assertTrue(MasterPasswordKdf.verify("correct horse battery staple", record))
    }

    @Test
    fun wrongPasswordDoesNotVerify() {
        val record = recordFor("correct password", saltByte = 2)

        assertFalse(MasterPasswordKdf.verify("wrong password", record))
    }

    @Test
    fun equalPasswordsUseDifferentVerifiersWithDifferentSalts() {
        val first = recordFor("same password", saltByte = 3)
        val second = recordFor("same password", saltByte = 4)

        assertNotEquals(first.saltBase64, second.saltBase64)
        assertNotEquals(first.verifierBase64, second.verifierBase64)
    }

    @Test
    fun unsupportedRecordVersionFailsClosed() {
        val record = recordFor("password", saltByte = 5).copy(version = 999)

        assertFalse(MasterPasswordKdf.verify("password", record))
    }

    @Test
    fun malformedRecordFailsClosed() {
        val record = recordFor("password", saltByte = 6).copy(saltBase64 = "not base64")

        assertFalse(MasterPasswordKdf.verify("password", record))
    }

    private fun recordFor(password: String, saltByte: Byte): MasterPasswordRecord {
        return MasterPasswordKdf.create(
            password = password,
            iterations = 10_000,
            salt = ByteArray(16) { saltByte }
        )
    }
}
