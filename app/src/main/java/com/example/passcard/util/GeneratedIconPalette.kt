package com.example.passcard.util

import kotlin.math.absoluteValue

/** Keep existing generated colors while safely handling the smallest Int hash. */
internal fun passwordIconPaletteIndex(seed: String, paletteCount: Int): Int {
    require(paletteCount > 0)
    return (seed.hashCode().toLong().absoluteValue % paletteCount).toInt()
}
