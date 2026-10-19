package com.wavelength.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Press-and-hold drag-to-reorder state for a [LazyListState], hand-rolled from
 * [androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress] and the list's own
 * [LazyListState.layoutInfo] rather than a third-party library, so there's no external dependency
 * version to keep compatible with this project's Compose version.
 *
 * [onMove] is called with the *visible list's own* 0-based indices every time the dragged item
 * crosses another item's midpoint — the caller is responsible for translating those into whatever
 * absolute index space the underlying data actually lives in (e.g. offsetting by how many items
 * precede this list, if it's a sublist).
 */
class DragDropListState internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val onPreviewMove: (from: Int, to: Int) -> Unit,
    private val onDrop: (from: Int, to: Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    private var edgeScrollJob: Job? = null
    private var edgeScrollSpeed = 0f

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)
    private var dragStartIndex: Int? = null
    private var dragTargetIndex: Int? = null

    val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            val rawOffset = draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
            val layout = listState.layoutInfo

            // Keep the dragged row physically inside the visible Up Next viewport. Without this,
            // the first queue item can be pulled above the top while the list itself cannot scroll
            // any farther, making the handle feel loose/uncontrolled and allowing the row to escape
            // the user's finger. The same protection applies at the bottom edge.
            val minOffset = (layout.viewportStartOffset - item.offset).toFloat()
            val maxOffset = (layout.viewportEndOffset - item.size - item.offset).toFloat()
            rawOffset.coerceIn(minOffset, maxOffset.coerceAtLeast(minOffset))
        } ?: 0f

    private val draggingItemLayoutInfo
        get() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

    var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    val previousItemOffset = Animatable(0f)

    internal fun onDragStart(index: Int) {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
        draggingItemIndex = index
        dragStartIndex = index
        dragTargetIndex = index
        draggingItemInitialOffset = item.offset
        draggingItemDraggedDelta = 0f
    }

    internal fun onDragInterrupted() {
        val startIndex = dragStartIndex
        val targetIndex = dragTargetIndex
        if (draggingItemIndex != null) {
            previousIndexOfDraggedItem = draggingItemIndex
            val startOffset = draggingItemOffset
            scope.launch {
                previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(
                    0f,
                    spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 1f)
                )
                previousIndexOfDraggedItem = null
            }
        }

        // The visible list is reordered locally while dragging (YouTube-style), but the real
        // Media3 queue is committed exactly once on release.
        if (startIndex != null && targetIndex != null && startIndex != targetIndex) {
            onDrop(startIndex, targetIndex)
        }

        stopEdgeAutoScroll()
        draggingItemDraggedDelta = 0f
        draggingItemIndex = null
        draggingItemInitialOffset = 0
        dragStartIndex = null
        dragTargetIndex = null
    }

    internal fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset.y

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        val targetItem = listState.layoutInfo.visibleItemsInfo.find { item ->
            middleOffset.toInt() in item.offset..(item.offset + item.size) && draggingItem.index != item.index
        }

        if (targetItem != null && targetItem.index != draggingItem.index) {
            val from = draggingItem.index
            val to = targetItem.index
            onPreviewMove(from, to)
            draggingItemIndex = to
            dragTargetIndex = to
            draggingItemInitialOffset = targetItem.offset
            draggingItemDraggedDelta = startOffset - targetItem.offset
        }

        // Edge scrolling must be evaluated on EVERY drag event, even while the dragged row is
        // still overlapping another visible item. Previously this lived in the targetItem == null
        // branch, so reaching the top/bottom almost always found a row underneath the finger and
        // therefore never started auto-scroll.
        val layout = listState.layoutInfo
        val topEdge = layout.viewportStartOffset.toFloat()
        val bottomEdge = layout.viewportEndOffset.toFloat()
        val viewportHeight = (bottomEdge - topEdge).coerceAtLeast(1f)
        val edgeZone = (viewportHeight * 0.22f).coerceIn(72f, 180f)

        val overscroll = when {
            endOffset >= bottomEdge - edgeZone && listState.canScrollForward -> {
                val penetration = (endOffset - (bottomEdge - edgeZone)) / edgeZone
                penetration.coerceIn(0f, 1f) * 38f
            }
            startOffset <= topEdge + edgeZone && listState.canScrollBackward -> {
                val penetration = ((topEdge + edgeZone) - startOffset) / edgeZone
                -penetration.coerceIn(0f, 1f) * 38f
            }
            else -> 0f
        }
        updateEdgeAutoScroll(overscroll)
    }

    private fun updateEdgeAutoScroll(speed: Float) {
        edgeScrollSpeed = speed
        if (speed == 0f) {
            stopEdgeAutoScroll()
            return
        }
        if (edgeScrollJob?.isActive == true) return

        edgeScrollJob = scope.launch {
            while (true) {
                val currentSpeed = edgeScrollSpeed
                if (currentSpeed == 0f || draggingItemIndex == null) break

                val canScroll = if (currentSpeed > 0f) {
                    listState.canScrollForward
                } else {
                    listState.canScrollBackward
                }
                if (!canScroll) break

                val consumed = listState.scrollBy(currentSpeed)

                // As the list moves underneath a stationary finger, keep moving the local
                // placeholder through newly revealed rows. This matches YouTube's queue: the
                // dragged card stays under the finger while neighbouring rows slide out of the way.
                val draggingIndex = draggingItemIndex
                val visible = listState.layoutInfo.visibleItemsInfo
                if (draggingIndex != null && visible.isNotEmpty()) {
                    val nextIndex = if (currentSpeed > 0f) {
                        visible.last().index.coerceAtMost(listState.layoutInfo.totalItemsCount - 1)
                    } else {
                        visible.first().index.coerceAtLeast(0)
                    }
                    if (nextIndex != draggingIndex) {
                        onPreviewMove(draggingIndex, nextIndex)
                        draggingItemIndex = nextIndex
                        dragTargetIndex = nextIndex
                    }
                }

                if (consumed == 0f) break
                delay(16L)
            }
            edgeScrollJob = null
        }
    }

    private fun stopEdgeAutoScroll() {
        edgeScrollSpeed = 0f
        edgeScrollJob?.cancel()
        edgeScrollJob = null
    }
}

@Composable
fun rememberDragDropListState(
    listState: LazyListState,
    onPreviewMove: (from: Int, to: Int) -> Unit,
    onDrop: (from: Int, to: Int) -> Unit
): DragDropListState {
    val scope = rememberCoroutineScope()
    val latestPreviewMove = rememberUpdatedState(onPreviewMove)
    val latestDrop = rememberUpdatedState(onDrop)
    return remember(listState) {
        DragDropListState(
            listState = listState,
            scope = scope,
            onPreviewMove = { from, to -> latestPreviewMove.value(from, to) },
            onDrop = { from, to -> latestDrop.value(from, to) }
        )
    }
}

/** Apply to a dedicated drag-handle icon inside each reorderable row, keyed by that row's index in
 * the *visible list*. A handle (rather than long-press-anywhere-on-the-row) is used because the
 * row itself already has its own tap/click handling (play, add to playlist, more options) — two
 * long-press-sensitive gesture detectors racing on the same touch would make the drag unreliable.
 *
 * The gesture detector is keyed only on [dragDropListState], NOT [index] — reordering swaps change
 * an in-progress drag's own [index] every time it crosses a neighbor, and keying `pointerInput` on
 * a value that changes mid-gesture would tear down and restart the detector on every swap (losing
 * the current touch). [rememberUpdatedState] lets `onDragStart` always read the latest [index]
 * without needing the detector itself to restart. */
@Composable
fun Modifier.dragHandle(dragDropListState: DragDropListState, index: Int): Modifier {
    val latestIndex = rememberUpdatedState(index)
    return this.pointerInput(dragDropListState) {
        detectDragGesturesAfterLongPress(
            onDragStart = { dragDropListState.onDragStart(latestIndex.value) },
            onDrag = { change, offset ->
                change.consume()
                dragDropListState.onDrag(offset)
            },
            onDragEnd = { dragDropListState.onDragInterrupted() },
            onDragCancel = { dragDropListState.onDragInterrupted() }
        )
    }
}

/** Apply to each reorderable item's modifier, keyed by its position in the *visible list*
 * (matching the indices [DragDropListState.onMove] operates on). Lifts the actively-dragged item
 * above its siblings and follows the finger; the item it displaced last animates back into its
 * resting position instead of snapping. */
fun Modifier.dragDropItemOffset(dragDropListState: DragDropListState, index: Int): Modifier = graphicsLayer {
    translationY = when (index) {
        dragDropListState.draggingItemIndex -> dragDropListState.draggingItemOffset
        dragDropListState.previousIndexOfDraggedItem -> dragDropListState.previousItemOffset.value
        else -> 0f
    }
}
