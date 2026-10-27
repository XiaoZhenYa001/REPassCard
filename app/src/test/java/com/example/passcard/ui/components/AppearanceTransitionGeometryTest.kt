package com.example.passcard.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTransitionGeometryTest {
    @Test
    fun `activity coordinates map into a scaled host instead of the full window`() {
        val origin = appearanceOriginInBounds(
            origin = Offset(170f, 340f),
            bounds = Rect(20f, 40f, 220f, 440f),
            renderSize = Size(100f, 200f)
        )

        assertEquals(75f, origin.x, 0.001f)
        assertEquals(150f, origin.y, 0.001f)
    }

    @Test
    fun `off-host origin clamps to the nearest edge`() {
        val origin = appearanceOriginInBounds(Offset(-20f, 900f), Rect(20f, 40f, 220f, 440f), Size(100f, 200f))

        assertEquals(Offset(0f, 200f), origin)
    }

    @Test
    fun `unspecified origin uses the center`() {
        assertEquals(
            Offset(50f, 100f),
            appearanceOriginInBounds(Offset.Unspecified, Rect(20f, 40f, 220f, 440f), Size(100f, 200f))
        )
    }

    @Test
    fun `corner-origin reveal covers the opposite corner plus rounding margin`() {
        val radius = appearanceRevealRadius(Offset.Zero, Size(100f, 200f))

        assertEquals(224.6068f, radius, 0.001f)
        assertTrue(radius * radius > 100f * 100f + 200f * 200f)
    }

    @Test
    fun `center-origin reveal covers all four corners`() {
        val radius = appearanceRevealRadius(Offset(50f, 100f), Size(100f, 200f))

        assertEquals(112.8034f, radius, 0.001f)
        assertTrue(radius > 111.8034f)
    }

    @Test
    fun `small frames retain their original resolution`() {
        assertEquals(IntSize(400, 800), appearanceCaptureSize(IntSize(400, 800)))
    }

    @Test
    fun `4k capture remains below the pixel budget without changing aspect ratio`() {
        val captured = appearanceCaptureSize(IntSize(3840, 2160))

        assertTrue(captured.width.toLong() * captured.height <= 1_000_000)
        assertTrue(captured.width.toLong() * captured.height > 990_000)
        assertTrue(captured.width < 3840 && captured.height < 2160)
        assertEquals(16f / 9f, captured.width.toFloat() / captured.height, 0.005f)
    }

    @Test
    fun `extreme aspect ratios still obey the allocation budget`() {
        val captured = appearanceCaptureSize(IntSize(Int.MAX_VALUE, 1), maxPixels = 100)

        assertEquals(IntSize(100, 1), captured)
    }

    @Test
    fun `unmeasured hosts do not allocate a frame`() {
        assertEquals(IntSize.Zero, appearanceCaptureSize(IntSize.Zero))
    }
}
