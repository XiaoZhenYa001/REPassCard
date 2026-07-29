package com.example.passcard.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class DropdownMenuPositionTest {
    @Test
    fun `menu top aligns with setting top instead of setting bottom`() {
        val position = calculateDropdownMenuPosition(
            anchorOffset = IntOffset(20, 320),
            anchorSize = IntSize(1040, 168),
            popupSize = IntSize(540, 504),
            windowSize = IntSize(1080, 2400),
            screenMarginPx = 24
        )

        assertEquals(IntOffset(516, 320), position)
    }

    @Test
    fun `menu stays inside right and bottom screen margins`() {
        val position = calculateDropdownMenuPosition(
            anchorOffset = IntOffset(900, 2200),
            anchorSize = IntSize(160, 168),
            popupSize = IntSize(540, 504),
            windowSize = IntSize(1080, 2400),
            screenMarginPx = 24
        )

        assertEquals(IntOffset(516, 1872), position)
    }

    @Test
    fun `oversized menu uses leading screen margin`() {
        val position = calculateDropdownMenuPosition(
            anchorOffset = IntOffset.Zero,
            anchorSize = IntSize(100, 100),
            popupSize = IntSize(1200, 2600),
            windowSize = IntSize(1080, 2400),
            screenMarginPx = 24
        )

        assertEquals(IntOffset(24, 24), position)
    }
}
