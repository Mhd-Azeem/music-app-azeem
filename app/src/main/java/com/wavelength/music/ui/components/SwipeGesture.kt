package com.wavelength.music.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Tracks a single continuous vertical drag and fires at most once per gesture, once the
 * cumulative movement passes [thresholdPx] in either direction — used for swipe-down-to-collapse
 * on Now Playing and swipe-up-to-expand on the mini player. */
fun Modifier.swipeVertical(
    thresholdPx: Float,
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null
): Modifier = this.pointerInput(onSwipeUp, onSwipeDown, thresholdPx) {
    var totalDrag = 0f
    var fired = false
    detectVerticalDragGestures(
        onDragStart = {
            totalDrag = 0f
            fired = false
        },
        onDragEnd = {
            totalDrag = 0f
            fired = false
        },
        onDragCancel = {
            totalDrag = 0f
            fired = false
        }
    ) { change, dragAmount ->
        change.consume()
        totalDrag += dragAmount
        if (!fired) {
            if (onSwipeDown != null && totalDrag > thresholdPx) {
                fired = true
                onSwipeDown()
            } else if (onSwipeUp != null && totalDrag < -thresholdPx) {
                fired = true
                onSwipeUp()
            }
        }
    }
}

/** Tracks a single continuous horizontal drag and fires at most once per gesture, once the
 * cumulative movement passes [thresholdPx] in either direction — used for swipe-to-skip on the
 * mini player. Compose's vertical/horizontal drag detectors each do their own axis-aware touch
 * slop, so this coexists with [swipeVertical] on the same element without one stealing the
 * other's gestures. */
fun Modifier.swipeHorizontal(
    thresholdPx: Float,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
): Modifier = this.pointerInput(onSwipeLeft, onSwipeRight, thresholdPx) {
    var totalDrag = 0f
    var fired = false
    detectHorizontalDragGestures(
        onDragStart = {
            totalDrag = 0f
            fired = false
        },
        onDragEnd = {
            totalDrag = 0f
            fired = false
        },
        onDragCancel = {
            totalDrag = 0f
            fired = false
        }
    ) { change, dragAmount ->
        change.consume()
        totalDrag += dragAmount
        if (!fired) {
            if (onSwipeLeft != null && totalDrag < -thresholdPx) {
                fired = true
                onSwipeLeft()
            } else if (onSwipeRight != null && totalDrag > thresholdPx) {
                fired = true
                onSwipeRight()
            }
        }
    }
}
