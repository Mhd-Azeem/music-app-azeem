package com.wavelength.music.ui.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.ui.components.TrackListScreen

@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onTrackClick: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val state by viewModel.tracks.collectAsStateWithLifecycle()

    TrackListScreen(
        title = name,
        subtitle = null,
        imageUrl = null,
        state = state,
        onBack = onBack,
        onRetry = {},
        onPlayAll = {
            viewModel.playAll()
            onTrackClick()
        },
        onTrackClick = { index ->
            viewModel.playTrack(index)
            onTrackClick()
        },
        onRemoveFromPlaylist = viewModel::removeTrack
    )
}
