package com.example.passcard.util

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomPasswordGeneratorTest {
    @Test
    fun specClampsLengthAndAlwaysKeepsACharacterSet() {
        val normalized = RandomPasswordSpec(
            length = 100,
            includeUppercase = false,
            includeLowercase = false,
            includeNumbers = false,
            includeSymbols = false
        ).normalized()

        assertEquals(RandomPasswordSpec.MAX_LENGTH, normalized.length)
        assertTrue(normalized.includeLowercase)
    }

    @Test
    fun generatedPasswordContainsEverySelectedCharacterType() {
        val password = RandomPasswordGenerator.generate(
            spec = RandomPasswordSpec(length = 18),
            random = Random(42)
        )

        assertEquals(18, password.length)
        assertTrue(password.any(Char::isUpperCase))
        assertTrue(password.any(Char::isLowerCase))
        assertTrue(password.any(Char::isDigit))
        assertTrue(password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun generatorDoesNotAddDisabledCharacterTypes() {
        val password = RandomPasswordGenerator.generate(
            spec = RandomPasswordSpec(
                length = 12,
                includeUppercase = false,
                includeLowercase = true,
                includeNumbers = false,
                includeSymbols = false
            ),
            random = Random(7)
        )

        assertTrue(password.all(Char::isLowerCase))
        assertFalse(password.any(Char::isDigit))
    }
}
