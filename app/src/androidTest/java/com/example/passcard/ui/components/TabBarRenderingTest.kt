package com.example.passcard.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.passcard.ui.theme.PassCardTheme
import com.example.passcard.ui.theme.buildThemeColors
import com.example.passcard.ui.theme.rememberThemeColors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the same software View.draw path used for an appearance transition's old frame. */
@RunWith(AndroidJUnit4::class)
class TabBarRenderingTest {
    @Test
    fun themeAndLanguageControllerCapturesStayWithinOneMillionPixels() {
        assumeTrue("Appearance animation must be enabled to exercise actual captures",
            ValueAnimator.areAnimatorsEnabled())
        val viewReference = AtomicReference<SoftwareDrawCountingView>()
        val sourcePixels = AtomicReference<Long>()
        val controller = AppearanceTransitionController()
        val owner = Any()

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val view = SoftwareDrawCountingView(activity)
                viewReference.set(view)
                activity.setContentView(view)
            }
            awaitFrames(scenario)
            scenario.onActivity {
                val view = requireNotNull(viewReference.get())
                sourcePixels.set(view.width.toLong() * view.height)
            }
            assumeTrue("A larger source is required to verify downsampling, not just an already-small image",
                requireNotNull(sourcePixels.get()) > 1_000_000L)
            scenario.onActivity {
                val view = requireNotNull(viewReference.get())
                assertTrue("The capture source must be attached and measured",
                    view.isAttachedToWindow && view.width > 0 && view.height > 0)
                val location = IntArray(2)
                view.getLocationInWindow(location)
                val bounds = Rect(location[0].toFloat(), location[1].toFloat(),
                    (location[0] + view.width).toFloat(), (location[1] + view.height).toFloat())
                controller.attach(owner, view, enabled = true, resumed = true)
                controller.updateBounds(owner, bounds)
                view.softwareDrawCount = 0
                view.softwareCanvasPixels.clear()
                try {
                    var themeActions = 0
                    controller.requestTheme(Offset(bounds.center.x, bounds.center.y)) { themeActions++ }
                    assertEquals("Theme change executes once after capture", 1, themeActions)
                    assertEquals("Theme uses one real software capture", 1, view.softwareDrawCount)
                    controller.clear()

                    var languageActions = 0
                    controller.requestLanguage { languageActions++ }
                    assertEquals("Language change executes once after capture", 1, languageActions)
                    assertEquals("Language adds exactly one real software capture", 2, view.softwareDrawCount)
                    assertEquals(2, view.softwareCanvasPixels.size)
                    view.softwareCanvasPixels.forEachIndexed { index, pixels ->
                        assertTrue("Actual capture $index exceeded its 1 MP budget: $pixels pixels",
                            pixels in 1L..1_000_000L)
                    }
                } finally {
                    controller.detach(owner)
                }
            }
        }
    }

    @Test
    fun rapidNavigationDoesNotSoftwareCaptureAndClearRestoresTheIncomingLayer() {
        assumeTrue("Navigation animation must be enabled to exercise its capture path",
            ValueAnimator.areAnimatorsEnabled())
        val viewReference = AtomicReference<SoftwareDrawCountingView>()
        val controller = AppearanceTransitionController()
        val owner = Any()

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val view = SoftwareDrawCountingView(activity)
                viewReference.set(view)
                activity.setContentView(view)
            }
            awaitFrames(scenario)
            scenario.onActivity {
                val view = requireNotNull(viewReference.get())
                assertTrue("The counting view must be attached and measured",
                    view.isAttachedToWindow && view.width > 0 && view.height > 0)
                val location = IntArray(2)
                view.getLocationInWindow(location)
                controller.attach(owner, view, enabled = true, resumed = true)
                controller.updateBounds(owner, Rect(location[0].toFloat(), location[1].toFloat(),
                    (location[0] + view.width).toFloat(), (location[1] + view.height).toFloat()))
                view.softwareDrawCount = 0
                try {
                    var selected = TabItem.HOME
                    var actions = 0
                    val requests = listOf(TabItem.SECURITY, TabItem.CLOUD, TabItem.HOME, TabItem.SETTINGS)
                    requests.forEach { target ->
                        val direction = if (target.ordinal > selected.ordinal) 1 else -1
                        controller.requestNavigation(direction) {
                            selected = target
                            actions++
                        }
                    }
                    assertEquals("Every requested tab action runs exactly once", requests.size, actions)
                    assertEquals("An interrupted animation must retain the final tab", TabItem.SETTINGS, selected)
                    assertEquals("A tab click must not rasterize the Activity on its calling thread",
                        0, view.softwareDrawCount)
                    assertTrue("The final transition should enter from the final direction",
                        controller.incomingTranslationX(1f) > 0f)
                    assertFalse("The live incoming page must accept input before its movement ends",
                        controller.blocksContentInput())

                    controller.clear()
                    assertEquals(0f, controller.incomingTranslationX(1f), 0f)
                    assertFalse(controller.blocksContentInput())
                } finally {
                    controller.detach(owner)
                }
            }
            // Pending callbacks from cancelled animators must not revive the old transition.
            awaitFrames(scenario)
            scenario.onActivity {
                assertEquals(0f, controller.incomingTranslationX(1f), 0f)
                assertFalse(controller.blocksContentInput())
            }
        }
    }

    @Test
    fun softwareSnapshotsPreserveTheBarBackdropAcrossThemeAndLanguageChanges() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val state = mutableStateOf(RenderState(dark = false, chinese = true))
        val rendered = AtomicReference<RenderState>()
        val barBounds = AtomicReference<Rect>()
        val pixelDensity = AtomicReference<Float>()

        // ui-test-manifest already declares ComponentActivity in the debug app. This avoids
        // launching the vault, changing production preferences, or needing Compose test APIs.
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    val current = state.value
                    val density = LocalDensity.current.density
                    CompositionLocalProvider(
                        LocalDensity provides Density(density, fontScale = 1f),
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        PassCardTheme(darkTheme = current.dark) {
                            val colors = rememberThemeColors()
                            SideEffect {
                                pixelDensity.set(density)
                                rendered.set(current)
                            }
                            Box(
                                Modifier.fillMaxSize().background(colors.background),
                                contentAlignment = Alignment.Center
                            ) {
                                TabBar(
                                    selectedTab = TabItem.HOME,
                                    isChinese = current.chinese,
                                    onTabSelected = {},
                                    onAddClick = {},
                                    modifier = Modifier.onGloballyPositioned { coordinates ->
                                        val position = coordinates.positionInWindow()
                                        barBounds.set(Rect(position.x, position.y,
                                            position.x + coordinates.size.width,
                                            position.y + coordinates.size.height))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            val cases = listOf(
                RenderState(dark = false, chinese = true),
                RenderState(dark = false, chinese = false),
                RenderState(dark = true, chinese = false),
                RenderState(dark = true, chinese = true),
                RenderState(dark = false, chinese = true)
            )
            cases.forEach { requested ->
                scenario.onActivity { state.value = requested }
                awaitFrames(scenario)
                instrumentation.waitForIdleSync()
                assertTrue("Requested appearance was not composed: $requested", rendered.get() == requested)
                assertNotNull("TabBar was not laid out", barBounds.get())

                var snapshot: Bitmap? = null
                var boundsInBitmap: Rect? = null
                scenario.onActivity { activity ->
                    val view = activity.window.decorView
                    val location = IntArray(2)
                    view.getLocationInWindow(location)
                    val bar = requireNotNull(barBounds.get())
                    boundsInBitmap = Rect(bar.left - location[0], bar.top - location[1],
                        bar.right - location[0], bar.bottom - location[1])
                    snapshot = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
                        view.draw(Canvas(it))
                    }
                }
                val bitmap = requireNotNull(snapshot)
                try {
                    val bounds = requireNotNull(boundsInBitmap)
                    val density = requireNotNull(pixelDensity.get())
                    val colors = buildThemeColors(requested.dark)
                    val expected = colors.tabBarBackground.compositeOver(colors.background).toArgb()
                    val rowInset = 6f * density
                    val addWidth = 54f * density
                    val tabWidth = (bounds.width - 2f * rowInset - addWidth) / 4f

                    // HOME owns the selection capsule. Sample the other three tab backdrops,
                    // below the glyphs and inside the former offscreen tint layer's rectangle.
                    for (index in 1..3) {
                        val slotLeft = rowInset + index * tabWidth + if (index >= 2) addWidth else 0f
                        val x = (bounds.left + slotLeft + 0.14f * tabWidth).roundToInt()
                        val y = (bounds.bottom - 15f * density).roundToInt()
                        assertBackdrop(expected, bitmap.getPixel(x, y), "$requested, tab=$index, pixel=($x,$y)")
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    private fun awaitFrames(scenario: ActivityScenario<ComponentActivity>) {
        val frames = CountDownLatch(1)
        scenario.onActivity { activity ->
            activity.window.decorView.postOnAnimation {
                activity.window.decorView.postOnAnimation { frames.countDown() }
            }
        }
        assertTrue("Timed out waiting for Compose layout/draw", frames.await(5, TimeUnit.SECONDS))
    }

    private fun assertBackdrop(expected: Int, actual: Int, description: String) {
        val largestDifference = maxOf(
            abs(AndroidColor.red(expected) - AndroidColor.red(actual)),
            abs(AndroidColor.green(expected) - AndroidColor.green(actual)),
            abs(AndroidColor.blue(expected) - AndroidColor.blue(actual))
        )
        // Small shadow/alpha rounding differences are harmless; a tint-colored rectangle is not.
        assertTrue("Backdrop changed during software capture ($description): " +
            "expected=${Integer.toHexString(expected)}, actual=${Integer.toHexString(actual)}",
            largestDifference <= 8)
    }

    private data class RenderState(val dark: Boolean, val chinese: Boolean)

    private class SoftwareDrawCountingView(context: Context) : View(context) {
        var softwareDrawCount = 0
        val softwareCanvasPixels = mutableListOf<Long>()

        init {
            setBackgroundColor(AndroidColor.WHITE)
        }

        override fun draw(canvas: Canvas) {
            if (!canvas.isHardwareAccelerated) {
                softwareDrawCount++
                softwareCanvasPixels += canvas.width.toLong() * canvas.height
            }
            super.draw(canvas)
        }
    }
}
