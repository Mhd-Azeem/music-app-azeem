package com.wavelength.music.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DragDropListState internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val firstReorderableIndex: Int,
    private val itemCount: Int,
    private val onPreviewMove: (from: Int, to: Int) -> Unit,
    private val onDrop: (from: Int, to: Int) -> Unit,
    private val onCancel: () -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set
    private var draggingKey: Any? = null
    private var startIndex: Int? = null
    private var itemSize = 0
    private var draggedTop by mutableFloatStateOf(0f)
    private var scrollJob: Job? = null

    private fun clampedTop(): Float {
        val layout = listState.layoutInfo
        val top = layout.viewportStartOffset.toFloat()
        return draggedTop.coerceIn(top, (layout.viewportEndOffset - itemSize).toFloat().coerceAtLeast(top))
    }

    fun offsetFor(key: Any): Float {
        if (draggingItemIndex == null || key != draggingKey) return 0f
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return 0f
        return clampedTop() - item.offset
    }

    internal fun start(index: Int): Boolean {
        if (draggingItemIndex != null) return false
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return false
        draggingKey = item.key
        itemSize = item.size
        draggedTop = item.offset.toFloat()
        startIndex = index
        draggingItemIndex = index
        scrollJob = scope.launch {
            var previousFrame = withFrameNanos { it }
            while (draggingItemIndex != null) {
                val frame = withFrameNanos { it }
                val elapsedSeconds = ((frame - previousFrame) / 1_000_000_000f).coerceAtMost(0.032f)
                previousFrame = frame
                val speed = edgeScrollSpeed()
                if (speed != 0f) listState.scrollBy(speed * elapsedSeconds)
                movePreview()
            }
        }
        return true
    }

    internal fun drag(deltaY: Float) {
        if (draggingItemIndex == null) return
        draggedTop += deltaY
        // Discard overshoot beyond the viewport so reversing direction moves immediately.
        draggedTop = clampedTop()
        movePreview()
    }

    private fun movePreview() {
        val index = draggingItemIndex ?: return
        val visible = listState.layoutInfo.visibleItemsInfo
        // Wait for the preceding placement to be applied before computing another move.
        val item = visible.firstOrNull { it.key == draggingKey && it.index == index } ?: return
        val center = clampedTop() + itemSize / 2f
        val itemCenter = item.offset + item.size / 2f
        val candidates = visible.asSequence().filter {
            it.index in firstReorderableIndex until firstReorderableIndex + itemCount
        }
        val target = if (center > itemCenter) {
            candidates.lastOrNull { it.index > index && center >= it.offset + it.size / 2f }
        } else {
            candidates.firstOrNull { it.index < index && center <= it.offset + it.size / 2f }
        } ?: return
        // The lazy list normally follows the old first visible key when rows move. Freeze
        // its numeric scroll anchor for this layout so the viewport stays under the pointer.
        listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        draggingItemIndex = target.index
        onPreviewMove(index - firstReorderableIndex, target.index - firstReorderableIndex)
    }

    private fun edgeScrollSpeed(): Float {
        val layout = listState.layoutInfo
        val top = layout.viewportStartOffset
        val bottom = layout.viewportEndOffset
        val edge = ((bottom - top) * 0.20f).coerceAtLeast(1f)
        val draggedStart = clampedTop()
        val draggedEnd = draggedStart + itemSize
        val downward = ((draggedEnd - bottom + edge) / edge).coerceAtLeast(0f)
        val upward = ((top + edge - draggedStart) / edge).coerceAtLeast(0f)
        val penetration = when {
            downward > upward && listState.canScrollForward -> downward
            upward > downward && listState.canScrollBackward -> -upward
            else -> 0f
        }
        return penetration.coerceIn(-1f, 1f) * edge * 12f
    }

    internal fun finish(commit: Boolean) {
        val from = startIndex ?: return
        val to = draggingItemIndex ?: from
        scrollJob?.cancel()
        scrollJob = null
        draggingItemIndex = null
        draggingKey = null
        startIndex = null
        if (commit && from != to) {
            onDrop(from - firstReorderableIndex, to - firstReorderableIndex)
        }
        onCancel()
    }
}

@Composable
fun rememberDragDropListState(
    listState: LazyListState,
    itemKeys: List<Any>,
    firstReorderableIndex: Int = 0,
    onPreviewMove: (from: Int, to: Int) -> Unit,
    onDrop: (from: Int, to: Int) -> Unit,
    onCancel: () -> Unit
): DragDropListState {
    val scope = rememberCoroutineScope()
    val latestPreviewMove = rememberUpdatedState(onPreviewMove)
    val latestDrop = rememberUpdatedState(onDrop)
    val latestCancel = rememberUpdatedState(onCancel)
    val state = remember(listState, itemKeys, firstReorderableIndex) {
        DragDropListState(
            listState = listState,
            scope = scope,
            firstReorderableIndex = firstReorderableIndex,
            itemCount = itemKeys.size,
            onPreviewMove = { from, to -> latestPreviewMove.value(from, to) },
            onDrop = { from, to -> latestDrop.value(from, to) },
            onCancel = { latestCancel.value() }
        )
    }
    DisposableEffect(state) {
        onDispose { state.finish(commit = false) }
    }
    return state
}

@Composable
fun Modifier.dragHandle(dragDropListState: DragDropListState, index: Int): Modifier {
    val latestIndex = rememberUpdatedState(index)
    return pointerInput(dragDropListState) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var initialDelta = 0f
            val start = if (down.type == PointerType.Mouse) {
                awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
                    initialDelta = overSlop.y
                    change.consume()
                }
            } else {
                awaitLongPressOrCancellation(down.id)
            } ?: return@awaitEachGesture

            if (!dragDropListState.start(latestIndex.value)) return@awaitEachGesture
            var completed = false
            try {
                dragDropListState.drag(initialDelta)
                completed = drag(start.id) { change ->
                    dragDropListState.drag(change.positionChange().y)
                    change.consume()
                }
                if (completed) currentEvent.changes.forEach { it.consume() }
            } finally {
                dragDropListState.finish(commit = completed)
            }
        }
    }
}

fun Modifier.dragDropItemOffset(dragDropListState: DragDropListState, key: Any): Modifier = graphicsLayer {
    translationY = dragDropListState.offsetFor(key)
}
