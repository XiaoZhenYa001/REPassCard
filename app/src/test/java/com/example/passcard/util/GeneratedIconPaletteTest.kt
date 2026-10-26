package com.example.passcard.util

import org.junit.Assert.assertEquals
import org.junit.Test

class GeneratedIconPaletteTest {
    @Test fun smallestIntHashSelectsAValidPalette() {
        val seed = "polygenelubricants"
        assertEquals(Int.MIN_VALUE, seed.hashCode())
        assertEquals(2, passwordIconPaletteIndex(seed, 6))
    }

    @Test fun existingNegativeHashesKeepTheirPalette() {
        val seed = "zzzzzz"
        assertEquals(-685785664, seed.hashCode())
        assertEquals(4, passwordIconPaletteIndex(seed, 6))
    }
}
