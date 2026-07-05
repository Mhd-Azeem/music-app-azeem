package com.wavelength.music.ui.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.ui.components.ScreenState
import com.wavelength.music.ui.components.TrackListScreen

@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    onTrackClick: () -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TrackListScreen(
        title = (state as? ScreenState.Success)?.data?.album?.name ?: "Album",
        subtitle = (state as? ScreenState.Success)?.data?.album?.artistName,
        imageUrl = (state as? ScreenState.Success)?.data?.album?.imageUrl,
        state = when (val s = state) {
            is ScreenState.Success -> ScreenState.Success(s.data.tracks)
            is ScreenState.Loading -> ScreenState.Loading
            is ScreenState.Empty -> ScreenState.Empty
            is ScreenState.Error -> ScreenState.Error(s.message)
        },
        onBack = onBack,
        onRetry = viewModel::load,
        onPlayAll = {
            viewModel.playAll()
            onTrackClick()
        },
        onTrackClick = { index ->
            viewModel.playTrack(index)
            onTrackClick()
        }
    )
}
