package com.wavelength.music.ui.components

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
