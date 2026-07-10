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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
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
    private val onMove: (from: Int, to: Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)

    val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo
        get() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

    var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    val previousItemOffset = Animatable(0f)

    internal fun onDragStart(offset: Offset) {
        listState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                draggingItemIndex = it.index
                draggingItemInitialOffset = it.offset
            }
    }

    internal fun onDragInterrupted() {
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
        draggingItemDraggedDelta = 0f
        draggingItemIndex = null
        draggingItemInitialOffset = 0
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

        if (targetItem != null) {
            onMove(draggingItem.index, targetItem.index)
            draggingItemIndex = targetItem.index
        } else {
            val viewport = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            val overscroll = when {
                draggingItemDraggedDelta > 0 -> (endOffset - viewport).coerceAtLeast(0f)
                draggingItemDraggedDelta < 0 -> startOffset.coerceAtMost(0f)
                else -> 0f
            }
            if (overscroll != 0f) scrollChannel.trySend(overscroll)
        }
    }
}

@Composable
fun rememberDragDropListState(
    listState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit
): DragDropListState {
    val scope = rememberCoroutineScope()
    val state = remember(listState) { DragDropListState(listState, scope, onMove) }
    LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            listState.scrollBy(diff)
        }
    }
    return state
}

/** Long-press anywhere on the item to start dragging it — matches how home-screen icon
 * reordering works, so it reads as familiar rather than needing a dedicated drag handle. */
fun Modifier.dragToReorder(dragDropListState: DragDropListState): Modifier = this then Modifier.pointerInput(
    dragDropListState
) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset -> dragDropListState.onDragStart(offset) },
        onDrag = { change, offset ->
            change.consume()
            dragDropListState.onDrag(offset)
        },
        onDragEnd = { dragDropListState.onDragInterrupted() },
        onDragCancel = { dragDropListState.onDragInterrupted() }
    )
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
