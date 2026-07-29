package com.example.passcard.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class MasterPasswordRecord(
    val version: Int,
    val iterations: Int,
    val saltBase64: String,
    val verifierBase64: String
)

object MasterPasswordKdf {
    const val CURRENT_VERSION = 1
    const val DEFAULT_ITERATIONS = 600_000

    private const val MIN_ITERATIONS = 10_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun create(
        password: String,
        iterations: Int = DEFAULT_ITERATIONS,
        salt: ByteArray = randomSalt()
    ): MasterPasswordRecord {
        require(password.isNotEmpty()) { "Master password cannot be empty" }
        require(iterations >= MIN_ITERATIONS) { "PBKDF2 iteration count is too low" }
        require(salt.size >= SALT_BYTES) { "Master password salt is too short" }

        val verifier = derive(password, salt, iterations)
        return try {
            MasterPasswordRecord(
                version = CURRENT_VERSION,
                iterations = iterations,
                saltBase64 = Base64.getEncoder().encodeToString(salt),
                verifierBase64 = Base64.getEncoder().encodeToString(verifier)
            )
        } finally {
            Arrays.fill(verifier, 0)
        }
    }

    fun verify(password: String, record: MasterPasswordRecord): Boolean {
        if (record.version != CURRENT_VERSION || record.iterations < MIN_ITERATIONS) return false

        var salt: ByteArray? = null
        var expected: ByteArray? = null
        var actual: ByteArray? = null
        return try {
            salt = Base64.getDecoder().decode(record.saltBase64)
            expected = Base64.getDecoder().decode(record.verifierBase64)
            if (salt.size < SALT_BYTES || expected.size != KEY_BITS / 8) return false

            actual = derive(password, salt, record.iterations)
            MessageDigest.isEqual(expected, actual)
        } catch (_: Exception) {
            false
        } finally {
            salt?.let { Arrays.fill(it, 0) }
            expected?.let { Arrays.fill(it, 0) }
            actual?.let { Arrays.fill(it, 0) }
        }
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            Arrays.fill(chars, '\u0000')
        }
    }

    private fun randomSalt(): ByteArray {
        return ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
    }
}
