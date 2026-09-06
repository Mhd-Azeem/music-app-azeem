package com.wavelength.music.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.R
import com.wavelength.music.ui.components.TrackListScreen

@Composable
fun GenreScreen(
    onBack: () -> Unit,
    onTrackClick: () -> Unit,
    viewModel: GenreViewModel = hiltViewModel()
) {
    val state by viewModel.tracks.collectAsStateWithLifecycle()

    TrackListScreen(
        title = viewModel.label,
        subtitle = null,
        imageUrl = null,
        state = state,
        onBack = onBack,
        onRetry = viewModel::load,
        onPlayAll = {
            viewModel.playAll()
            onTrackClick()
        },
        onTrackClick = { index ->
            viewModel.playTrack(index)
            onTrackClick()
        },
        emptyMessage = stringResource(R.string.search_empty_hint),
        onLoadMore = viewModel::loadMore
    )
}
