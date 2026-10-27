package com.example.passcard.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

/** Shared timings keep interaction feedback predictable across the app. */
object MotionDuration {
    const val Press = 100
    const val Release = 160
    const val Fast = 120
    const val Exit = 140
    const val Standard = 160
    const val Emphasized = 240
    const val Sheet = 240
    const val ThemeReveal = 540
    const val Language = 320
    const val Navigation = 240
}

object MotionEasing {
    val EnterExit = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val Drawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
}

object MotionScale {
    const val Pressed = 0.97f
    const val StrongPressed = 0.94f
    const val Enter = 0.96f
}
