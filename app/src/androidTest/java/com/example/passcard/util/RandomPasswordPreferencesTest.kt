package com.example.passcard.util

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Each test redirects the manager to a private test file, away from the vault's preferences. */
@RunWith(AndroidJUnit4::class)
class RandomPasswordPreferencesTest {
    private lateinit var context: Context
    private lateinit var testContext: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var testFile: String

    @Before fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testFile = "random_password_test_${UUID.randomUUID()}"
        testContext = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
                context.getSharedPreferences(testFile, mode)
        }
        preferences = testContext.getSharedPreferences("isolated", Context.MODE_PRIVATE)
    }

    @After fun cleanUp() {
        context.deleteSharedPreferences(testFile)
    }

    @Test fun legacyTypeSettingsRetainEnabledGroupsAndLength() {
        preferences.edit()
            .putInt("random_password_length", 12)
            .putBoolean("random_password_uppercase", false)
            .putBoolean("random_password_lowercase", true)
            .putBoolean("random_password_numbers", false)
            .putBoolean("random_password_symbols", true)
            .commit()
        val loaded = PreferencesManager(testContext).randomPasswordSpec
        assertEquals(12, loaded.length)
        assertEquals("", loaded.selectedCharacters(PasswordCharacterGroup.Uppercase))
        assertEquals("", loaded.selectedCharacters(PasswordCharacterGroup.Numbers))
        assertEquals(PasswordCharacterGroup.Lowercase.availableCharacters,
            loaded.selectedCharacters(PasswordCharacterGroup.Lowercase))
        assertEquals(PasswordCharacterGroup.Symbols.availableCharacters,
            loaded.selectedCharacters(PasswordCharacterGroup.Symbols))
    }

    @Test fun customSubsetsAndEmptyGroupsRoundTripThroughANewManager() {
        val spec = RandomPasswordSpec(length = 18)
            .withSelectedCharacters(PasswordCharacterGroup.Uppercase, "AZ")
            .withSelectedCharacters(PasswordCharacterGroup.Lowercase, "")
            .withSelectedCharacters(PasswordCharacterGroup.Numbers, "01")
            .withSelectedCharacters(PasswordCharacterGroup.Symbols, "!?")
        PreferencesManager(testContext).randomPasswordSpec = spec
        val loaded = PreferencesManager(testContext).randomPasswordSpec
        assertEquals(spec, loaded)
        assertFalse(loaded.includeLowercase)
        assertTrue(RandomPasswordGenerator.generate(loaded).all { it in "AZ01!?" })
    }

    @Test fun savingSanitizesInvalidCharactersAndRepairsAnEmptyPool() {
        val manager = PreferencesManager(testContext)
        manager.randomPasswordSpec = RandomPasswordSpec(uppercaseCharacters = "中", lowercaseCharacters = "",
            numberCharacters = "", symbolCharacters = "")
        val loaded = PreferencesManager(testContext).randomPasswordSpec
        assertTrue(loaded.hasSelectedCharacters)
        assertFalse(loaded.includeUppercase)
        assertEquals(PasswordCharacterGroup.Lowercase.availableCharacters,
            loaded.selectedCharacters(PasswordCharacterGroup.Lowercase))
    }
}
