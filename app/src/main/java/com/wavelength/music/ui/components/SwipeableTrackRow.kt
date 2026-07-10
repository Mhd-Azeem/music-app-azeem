package com.wavelength.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wavelength.music.data.model.Track

/** Wraps [TrackRow] with swipe gestures: swipe start-to-end toggles favorite, swipe end-to-start
 * removes (meaning depends on the tab: unfavorite, remove from history, or remove a download).
 * Falls back to a plain [TrackRow] when both swipe actions are null (e.g. multi-select mode,
 * where a swipe gesture would conflict with tap-to-select), or when in selection mode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTrackRow(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    showSelectionCheckbox: Boolean = false,
    onSwipeToFavorite: (() -> Unit)? = null,
    onSwipeToRemove: (() -> Unit)? = null
) {
    if (showSelectionCheckbox || (onSwipeToFavorite == null && onSwipeToRemove == null)) {
        TrackRow(
            track = track,
            onClick = onClick,
            modifier = modifier,
            isFavorite = isFavorite,
            onFavoriteClick = onFavoriteClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onMoreClick = onMoreClick,
            onLongClick = onLongClick,
            isSelected = isSelected,
            showSelectionCheckbox = showSelectionCheckbox
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onSwipeToFavorite?.invoke()
                SwipeToDismissBoxValue.EndToStart -> onSwipeToRemove?.invoke()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = onSwipeToFavorite != null,
        enableDismissFromEndToStart = onSwipeToRemove != null,
        backgroundContent = { SwipeBackground(dismissState.dismissDirection) }
    ) {
        TrackRow(
            track = track,
            onClick = onClick,
            isFavorite = isFavorite,
            onFavoriteClick = onFavoriteClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onMoreClick = onMoreClick,
            onLongClick = onLongClick
        )
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val (color, icon, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd ->
            Triple(MaterialTheme.colorScheme.primary, Icons.Filled.Favorite, Alignment.CenterStart)
        SwipeToDismissBoxValue.EndToStart ->
            Triple(MaterialTheme.colorScheme.error, Icons.Filled.Delete, Alignment.CenterEnd)
        SwipeToDismissBoxValue.Settled ->
            Triple(Color.Transparent, null, Alignment.Center)
    }
    Box(
        modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
    }
}
