package com.gadfaria.nim.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * The home's two gestures — swipe up opens the drawer, hold opens settings — in
 * a single detector.
 *
 * Stacking `detectVerticalDragGestures` and `detectTapGestures` on the same Box
 * doesn't work: both receive the same touch, fight over who consumes it, and the
 * drag starts failing intermittently. Here the gesture itself decides what it
 * is: if it leaves the slop before the timeout, it's a drag; if it stays still
 * until the timeout, it's a long press.
 */
fun Modifier.homeGestures(
    swipeUpThresholdPx: Float,
    onSwipeUp: () -> Unit,
    onLongPress: () -> Unit,
): Modifier = pointerInput(swipeUpThresholdPx) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var totalY = 0f

        // null = timed out with the finger still (long press)
        // true = became a drag
        // false = released before either (plain tap)
        val becameDrag = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var drag = false
            while (true) {
                val change = awaitPointerEvent().changes
                    .firstOrNull { it.id == down.id } ?: break
                totalY += change.positionChange().y
                if (!change.pressed) break
                if (abs(totalY) > viewConfiguration.touchSlop) {
                    drag = true
                    break
                }
            }
            drag
        }

        when (becameDrag) {
            null -> {
                // Down already consumed = the touch started on an app row, which
                // has its own long press (the menu). This timeout runs in
                // parallel with the row's, so without this check both fired: the
                // menu opened on top of settings, and back landed on them.
                if (!down.isConsumed) onLongPress()
                consumeUntilUp(down.id)
            }

            true -> {
                while (true) {
                    val change = awaitPointerEvent().changes
                        .firstOrNull { it.id == down.id } ?: break
                    totalY += change.positionChange().y
                    change.consume()
                    if (!change.pressed) break
                }
                if (totalY < -swipeUpThresholdPx) onSwipeUp()
            }

            false -> Unit
        }
    }
}

private suspend fun AwaitPointerEventScope.consumeUntilUp(pointer: PointerId) {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer } ?: break
        change.consume()
        if (!change.pressed) break
    }
}
