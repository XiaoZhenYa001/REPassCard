package com.example.passcard.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.os.Trace
import android.view.View
import androidx.annotation.MainThread
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.passcard.ui.theme.MotionDuration
import com.example.passcard.ui.theme.MotionEasing
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

val LocalAppearanceTransitionController = staticCompositionLocalOf<AppearanceTransitionController?> { null }

@Composable
fun rememberAppearanceTransitionController(): AppearanceTransitionController = remember {
    AppearanceTransitionController()
}

/** Owns one host and an ephemeral, app-drawn frame; no system capture or file is used. */
@Stable
class AppearanceTransitionController internal constructor() {
    private var binding: HostBinding? = null
    private var frame by mutableStateOf<AppearanceFrame?>(null)
    private var progress by mutableFloatStateOf(1f)
    private var navigationDirection by mutableIntStateOf(0)
    private var animator: ValueAnimator? = null
    private var generation = 0L
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val mask = AndroidPath()

    @MainThread
    fun requestTheme(originInWindow: Offset, action: () -> Unit) {
        request(AppearanceMotion.THEME, originInWindow, 0, action)
    }

    @MainThread
    fun requestLanguage(action: () -> Unit) {
        request(AppearanceMotion.LANGUAGE, Offset.Unspecified, 0, action)
    }

    @MainThread
    fun requestNavigation(direction: Int, action: () -> Unit) {
        if (direction == 0) {
            clear()
            action()
        } else {
            request(AppearanceMotion.NAVIGATION, Offset.Unspecified, direction.coerceIn(-1, 1), action)
        }
    }

    /** Also call before navigating, returning, or replacing the unlocked session. */
    @MainThread
    fun clear() {
        generation++
        stopAnimator()
        // RenderThread may still reference a queued frame. Release it, never recycle it here.
        frame = null
        navigationDirection = 0
        progress = 1f
    }

    internal fun attach(owner: Any, view: View, enabled: Boolean, resumed: Boolean) {
        clear()
        binding = HostBinding(owner, view, enabled = enabled, resumed = resumed)
    }

    internal fun detach(owner: Any) {
        if (binding?.owner !== owner) return
        clear()
        binding = null
    }

    internal fun setEnabled(owner: Any, enabled: Boolean) {
        val host = binding?.takeIf { it.owner === owner } ?: return
        host.enabled = enabled
        if (!enabled) clear()
    }

    internal fun setResumed(owner: Any, resumed: Boolean) {
        val host = binding?.takeIf { it.owner === owner } ?: return
        host.resumed = resumed
        if (!resumed) clear()
    }

    internal fun updateBounds(owner: Any, bounds: Rect) {
        val host = binding?.takeIf { it.owner === owner } ?: return
        if (host.bounds?.size != bounds.size) clear()
        host.bounds = bounds
    }

    internal fun incomingTranslationX(density: Float): Float {
        return navigationDirection * NAVIGATION_DISTANCE_DP * density * (1f - progress)
    }

    // Navigation shows only live controls, whose hit coordinates follow the layer.
    // Only an appearance snapshot can cover a different control beneath it.
    internal fun blocksContentInput(): Boolean = frame?.blocksInput == true

    internal fun drawOldFrame(scope: DrawScope) {
        val current = frame ?: return
        val fraction = progress.coerceIn(0f, 1f)
        if (fraction >= 1f) return
        val canvas = scope.drawContext.canvas.nativeCanvas
        val saved = canvas.save()
        try {
            canvas.clipRect(0f, 0f, scope.size.width, scope.size.height)
            if (current.motion == AppearanceMotion.THEME) {
                val origin = Offset(current.origin.x * scope.size.width, current.origin.y * scope.size.height)
                mask.reset()
                mask.addCircle(origin.x, origin.y, appearanceRevealRadius(origin, scope.size) * fraction, AndroidPath.Direction.CW)
                canvas.clipOutPath(mask)
                paint.alpha = 255
            } else {
                paint.alpha = ((1f - fraction) * 255).roundToInt().coerceIn(0, 255)
            }
            canvas.drawBitmap(current.bitmap, null, RectF(0f, 0f, scope.size.width, scope.size.height), paint)
        } finally {
            canvas.restoreToCount(saved)
        }
    }

    private fun request(motion: AppearanceMotion, origin: Offset, direction: Int, action: () -> Unit) {
        val host = binding
        if (host == null || !host.enabled || !host.resumed || !ValueAnimator.areAnimatorsEnabled()) {
            clear()
            action()
            return
        }
        // Frequent navigation moves the one live page, without rasterizing a snapshot
        // of the whole Activity on the click thread.
        val navigation = motion == AppearanceMotion.NAVIGATION
        // Appearance changes capture before cancelling to retain an interrupted frame.
        val wasBlocking = blocksContentInput()
        val initialProgress = if (navigation && navigationDirection != 0) progress else 0f
        val captured = if (navigation) null else capture(host, motion, origin)?.copy(
            blocksInput = motion != AppearanceMotion.THEME || wasBlocking
        )
        generation++
        val requestGeneration = generation
        stopAnimator()
        frame = captured
        navigationDirection = if (navigation) direction else 0
        progress = initialProgress
        try {
            action()
        } catch (failure: Throwable) {
            clear()
            throw failure
        }
        if ((!navigation && captured == null) || generation != requestGeneration) {
            if (generation == requestGeneration) clear()
            return
        }
        val next = ValueAnimator.ofFloat(initialProgress, 1f).apply {
            duration = when (motion) {
                AppearanceMotion.THEME -> MotionDuration.ThemeReveal.toLong()
                AppearanceMotion.LANGUAGE -> MotionDuration.Language.toLong()
                AppearanceMotion.NAVIGATION -> MotionDuration.Navigation.toLong()
            }
            interpolator = TimeInterpolator { MotionEasing.EnterExit.transform(it) }
            addUpdateListener {
                if (generation == requestGeneration) progress = it.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (generation == requestGeneration) {
                        animator = null
                        frame = null
                        navigationDirection = 0
                        progress = 1f
                    }
                }
            })
        }
        animator = next
        next.start()
    }

    private fun stopAnimator() {
        val previous = animator
        animator = null
        previous?.removeAllUpdateListeners()
        previous?.removeAllListeners()
        previous?.cancel()
    }

    private fun capture(host: HostBinding, motion: AppearanceMotion, origin: Offset): AppearanceFrame? {
        val bounds = host.bounds ?: return null
        if (!host.view.isAttachedToWindow || host.view.width <= 0 || host.view.height <= 0 ||
            !bounds.width.isFinite() || !bounds.height.isFinite() || bounds.width < 1f || bounds.height < 1f
        ) return null
        val captureSize = appearanceCaptureSize(IntSize(bounds.width.roundToInt(), bounds.height.roundToInt()))
        return try {
            Trace.beginSection("PassCard.Appearance.softwareCapture")
            val bitmap = try {
                Bitmap.createBitmap(captureSize.width, captureSize.height, Bitmap.Config.ARGB_8888).also {
                    val canvas = AndroidCanvas(it)
                    val viewLocation = IntArray(2)
                    host.view.getLocationInWindow(viewLocation)
                    canvas.scale(captureSize.width / bounds.width, captureSize.height / bounds.height)
                    canvas.translate(viewLocation[0] - bounds.left, viewLocation[1] - bounds.top)
                    host.view.draw(canvas)
                }
            } finally {
                Trace.endSection()
            }
            AppearanceFrame(bitmap, motion, appearanceOriginInBounds(origin, bounds, Size(1f, 1f)))
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: RuntimeException) {
            // Unavailable/software-incompatible drawing must never prevent the requested action.
            null
        }
    }

    private data class HostBinding(
        val owner: Any,
        val view: View,
        var bounds: Rect? = null,
        var enabled: Boolean,
        var resumed: Boolean
    )

    private data class AppearanceFrame(
        val bitmap: Bitmap,
        val motion: AppearanceMotion,
        val origin: Offset,
        val blocksInput: Boolean = false
    )
}

/** Keeps one live tree; pixels never add accessibility nodes or duplicate effects. */
@Composable
fun AppearanceTransitionHost(
    controller: AppearanceTransitionController,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    provideController: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val owner = remember { Any() }
    DisposableEffect(controller, view, lifecycle, owner) {
        controller.attach(owner, view, enabled, lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.setResumed(owner, true)
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> controller.setResumed(owner, false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            controller.detach(owner)
        }
    }
    SideEffect { controller.setEnabled(owner, enabled) }

    val hostedContent: @Composable () -> Unit = {
        Box(
            modifier = modifier
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInWindow()
                    controller.updateBounds(owner, Rect(position.x, position.y, position.x + coordinates.size.width, position.y + coordinates.size.height))
                }
                .clipToBounds()
                .pointerInput(controller, enabled) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        if (enabled && controller.blocksContentInput()) {
                            // A visible old control must not activate the new page beneath it.
                            down.consume()
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    // The enabled guard hides an old frame in the very first locked composition.
                    if (enabled) controller.drawOldFrame(this)
                }
        ) {
            Box(Modifier.graphicsLayer {
                // Keep the viewport opaque. Fading the entire page creates a large
                // offscreen surface; navigation only moves its cached display list.
                translationX = if (enabled) controller.incomingTranslationX(density) else 0f
            }) { content() }
        }
    }
    if (provideController) {
        CompositionLocalProvider(LocalAppearanceTransitionController provides controller) { hostedContent() }
    } else {
        hostedContent()
    }
}

internal fun appearanceCaptureSize(source: IntSize, maxPixels: Int = MAX_CAPTURE_PIXELS): IntSize {
    if (source.width <= 0 || source.height <= 0 || maxPixels <= 0) return IntSize.Zero
    val scale = minOf(1.0, sqrt(maxPixels / (source.width.toDouble() * source.height)))
    var width = floor(source.width * scale).toInt().coerceAtLeast(1)
    var height = floor(source.height * scale).toInt().coerceAtLeast(1)
    if (width.toLong() * height > maxPixels) {
        if (width >= height) width = (maxPixels / height).coerceAtLeast(1)
        else height = (maxPixels / width).coerceAtLeast(1)
    }
    return IntSize(width, height)
}

internal fun appearanceOriginInBounds(origin: Offset, bounds: Rect, renderSize: Size): Offset {
    if (bounds.width <= 0f || bounds.height <= 0f || !origin.isSpecified || !origin.x.isFinite() || !origin.y.isFinite()) {
        return Offset(renderSize.width / 2f, renderSize.height / 2f)
    }
    return Offset(
        ((origin.x - bounds.left) / bounds.width).coerceIn(0f, 1f) * renderSize.width,
        ((origin.y - bounds.top) / bounds.height).coerceIn(0f, 1f) * renderSize.height
    )
}

internal fun appearanceRevealRadius(origin: Offset, size: Size): Float =
    hypot(max(origin.x, size.width - origin.x), max(origin.y, size.height - origin.y)) + 1f

private enum class AppearanceMotion { THEME, LANGUAGE, NAVIGATION }
private const val MAX_CAPTURE_PIXELS = 1_000_000
private const val NAVIGATION_DISTANCE_DP = 16f
