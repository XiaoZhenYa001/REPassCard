package com.example.passcard.util

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomPasswordGeneratorTest {
    @Test
    fun productionRandomSourceHonorsTheSelectedLengthAndAlphabet() {
        val defaultPassword = RandomPasswordGenerator.generate(RandomPasswordSpec())
        assertEquals(RandomPasswordSpec.DEFAULT_LENGTH, defaultPassword.length)
        assertTrue(defaultPassword.any(Char::isUpperCase))
        assertTrue(defaultPassword.any(Char::isLowerCase))
        assertTrue(defaultPassword.any(Char::isDigit))
        assertTrue(defaultPassword.any { !it.isLetterOrDigit() })

        val digitsOnly = RandomPasswordGenerator.generate(RandomPasswordSpec(
            length = 18,
            includeUppercase = false,
            includeLowercase = false,
            includeNumbers = true,
            includeSymbols = false
        ))
        assertEquals(18, digitsOnly.length)
        assertTrue(digitsOnly.all { it in "0123456789" })
    }

    @Test
    fun newDefaultGenerationMeetsTheLocalLengthRecommendation() {
        repeat(25) { seed ->
            val password = RandomPasswordGenerator.generate(RandomPasswordSpec(), Random(seed))
            assertFalse(com.example.passcard.data.PasswordWeakReason.TOO_SHORT in
                com.example.passcard.data.PasswordHealthAnalyzer.assessPassword(password).reasons)
        }
    }

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

    @Test
    fun generatesOnlyCustomCharactersAndIncludesEveryNonemptyGroup() {
        val spec = RandomPasswordSpec(length = 18)
            .withSelectedCharacters(PasswordCharacterGroup.Uppercase, "AZ")
            .withSelectedCharacters(PasswordCharacterGroup.Lowercase, "mq")
            .withSelectedCharacters(PasswordCharacterGroup.Numbers, "01")
            .withSelectedCharacters(PasswordCharacterGroup.Symbols, "!?")
        repeat(100) { seed ->
            val generated = RandomPasswordGenerator.generate(spec, Random(seed))
            assertEquals(18, generated.length)
            assertTrue(generated.all { it in "AZmq01!?" })
            PasswordCharacterGroup.entries.forEach { group ->
                assertTrue(generated.any { it in spec.selectedCharacters(group) })
            }
        }
    }

    @Test
    fun shortestLengthSupportsOneCharacterFromEachGroup() {
        val spec = RandomPasswordSpec(length = 4, uppercaseCharacters = "O",
            lowercaseCharacters = "l", numberCharacters = "0", symbolCharacters = "!")
        repeat(30) { seed ->
            assertEquals(setOf('O', 'l', '0', '!'),
                RandomPasswordGenerator.generate(spec, Random(seed)).toSet())
        }
    }

    @Test
    fun emptyCategoriesStayEmptyWithoutIntroducingUnselectedCharacters() {
        val spec = RandomPasswordSpec(length = 12)
            .withSelectedCharacters(PasswordCharacterGroup.Uppercase, "")
            .withSelectedCharacters(PasswordCharacterGroup.Lowercase, "")
            .withSelectedCharacters(PasswordCharacterGroup.Numbers, "09")
            .withSelectedCharacters(PasswordCharacterGroup.Symbols, "")
            .normalized()
        assertFalse(spec.includeUppercase)
        assertFalse(spec.includeLowercase)
        assertFalse(spec.includeSymbols)
        repeat(30) { seed ->
            assertTrue(RandomPasswordGenerator.generate(spec, Random(seed)).all { it in "09" })
        }
    }

    @Test
    fun sanitizesDuplicatesWrongCaseAndUnknownCharactersInCanonicalOrder() {
        val spec = RandomPasswordSpec(uppercaseCharacters = "ZAAa0中!I",
            lowercaseCharacters = "zzzAa!", numberCharacters = "9910a", symbolCharacters = "??!日a")
            .normalized()
        assertEquals("AIZ", spec.uppercaseCharacters)
        assertEquals("az", spec.lowercaseCharacters)
        assertEquals("019", spec.numberCharacters)
        assertEquals("!?", spec.symbolCharacters)
        assertEquals(spec, spec.normalized())
    }

    @Test
    fun allEmptyCandidateIsDetectableBeforePersistenceRepair() {
        val empty = PasswordCharacterGroup.entries.fold(RandomPasswordSpec()) { spec, group ->
            spec.withSelectedCharacters(group, "")
        }
        assertFalse(empty.hasSelectedCharacters)
        val repaired = empty.normalized()
        assertEquals(PasswordCharacterGroup.Lowercase.availableCharacters,
            repaired.selectedCharacters(PasswordCharacterGroup.Lowercase))
        assertTrue(RandomPasswordGenerator.generate(empty, Random(1)).all(Char::isLowerCase))
    }

    @Test
    fun malformedEmptyEnabledGroupsAreDisabledWithoutReplacingOtherChoices() {
        val normalized = RandomPasswordSpec(uppercaseCharacters = "?中", lowercaseCharacters = "k",
            numberCharacters = "", symbolCharacters = "").normalized()
        assertFalse(normalized.includeUppercase)
        assertFalse(normalized.includeNumbers)
        assertFalse(normalized.includeSymbols)
        assertEquals("k".repeat(16), RandomPasswordGenerator.generate(normalized))
    }

    @Test
    fun disabledStoredSubsetIsPreservedButNeverGenerated() {
        val spec = RandomPasswordSpec(includeUppercase = false, uppercaseCharacters = "Q",
            lowercaseCharacters = "a", includeNumbers = false, includeSymbols = false).normalized()
        assertEquals("Q", spec.uppercaseCharacters)
        assertEquals("", spec.selectedCharacters(PasswordCharacterGroup.Uppercase))
        assertEquals("a".repeat(16), RandomPasswordGenerator.generate(spec))
        assertEquals("Q", spec.withSelectedCharacters(PasswordCharacterGroup.Uppercase, "Q")
            .selectedCharacters(PasswordCharacterGroup.Uppercase))
    }

    @Test
    fun fullAlphabetsIncludePreviouslyExcludedAmbiguousCharacters() {
        assertEquals(('A'..'Z').joinToString(""), PasswordCharacterGroup.Uppercase.availableCharacters)
        assertEquals(('a'..'z').joinToString(""), PasswordCharacterGroup.Lowercase.availableCharacters)
        assertEquals(('0'..'9').joinToString(""), PasswordCharacterGroup.Numbers.availableCharacters)
        assertEquals(14, PasswordCharacterGroup.Symbols.availableCharacters.length)
        assertEquals(14, PasswordCharacterGroup.Symbols.availableCharacters.toSet().size)
    }

    @Test
    fun singleAllowedCharacterHasZeroConfigurationReferenceRegardlessOfLength() {
        val spec = RandomPasswordSpec(length = 18, includeUppercase = false,
            lowercaseCharacters = "a", includeNumbers = false, includeSymbols = false)
        assertEquals(0, RandomPasswordGenerator.strength(spec).score)
        assertEquals("a".repeat(18), RandomPasswordGenerator.generate(spec))
    }

    @Test
    fun configurationReferenceRespondsToPoolSizeAndLength() {
        val small = RandomPasswordSpec(length = 12, includeUppercase = false,
            lowercaseCharacters = "ab", includeNumbers = false, includeSymbols = false)
        val moreCharacters = small.copy(lowercaseCharacters = "abcdefghijklmnopqrstuvwxyz")
        assertTrue(RandomPasswordGenerator.strength(moreCharacters).score >
            RandomPasswordGenerator.strength(small).score)
        assertTrue(RandomPasswordGenerator.strength(moreCharacters.copy(length = 18)).score >
            RandomPasswordGenerator.strength(moreCharacters).score)
    }
}
