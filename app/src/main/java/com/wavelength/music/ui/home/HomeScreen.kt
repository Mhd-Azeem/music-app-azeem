package com.wavelength.music.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.R
import com.wavelength.music.data.model.Track
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.ErrorView
import com.wavelength.music.ui.components.LoadingView
import com.wavelength.music.ui.components.ScreenState
import com.wavelength.music.ui.components.SectionHeader
import com.wavelength.music.ui.components.TrackCard

@Composable
fun HomeScreen(
    onTrackClick: () -> Unit,
    onGenreClick: (tag: String, label: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val featured by viewModel.featured.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { padding ->
        when (val state = featured) {
            is ScreenState.Loading -> LoadingView(modifier = Modifier.padding(padding))
            is ScreenState.Error -> ErrorView(
                onRetry = viewModel::loadFeatured,
                modifier = Modifier.padding(padding),
                message = state.message
            )
            is ScreenState.Empty -> EmptyView(modifier = Modifier.padding(padding))
            is ScreenState.Success -> HomeContent(
                padding = padding,
                featuredTracks = state.data,
                recentlyPlayed = recentlyPlayed,
                onTrackClick = { index, queue ->
                    viewModel.playTrack(queue, index)
                    onTrackClick()
                },
                onGenreClick = onGenreClick
            )
        }
    }
}

@Composable
private fun HomeContent(
    padding: PaddingValues,
    featuredTracks: List<Track>,
    recentlyPlayed: List<Track>,
    onTrackClick: (Int, List<Track>) -> Unit,
    onGenreClick: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        item { SectionHeader(stringResource(R.string.featured_tracks)) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(featuredTracks) { index, track ->
                    TrackCard(track = track, onClick = { onTrackClick(index, featuredTracks) })
                }
            }
        }

        item { SectionHeader(stringResource(R.string.genres_moods)) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(genreShortcuts) { tag ->
                    val label = tag.replaceFirstChar { it.uppercase() }
                    AssistChip(onClick = { onGenreClick(tag, label) }, label = { Text(label) })
                }
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.recently_played)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(recentlyPlayed) { index, track ->
                        TrackCard(track = track, onClick = { onTrackClick(index, recentlyPlayed) })
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}
