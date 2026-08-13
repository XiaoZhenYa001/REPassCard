package com.example.passcard.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Before
    fun clearTargetState() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm clear $TARGET_PACKAGE")
            .close()
    }

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun mainNavigation() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = false
    ) {
        pressHome()
        startActivityAndWait()
        exerciseMainUi(device)
    }
}
