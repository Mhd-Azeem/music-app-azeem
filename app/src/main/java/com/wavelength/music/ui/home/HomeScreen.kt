package com.wavelength.music.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.R
import com.wavelength.music.data.model.PlaylistSummary
import com.wavelength.music.data.model.Track
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.ErrorView
import com.wavelength.music.ui.components.LoadingView
import com.wavelength.music.ui.components.ScreenState
import com.wavelength.music.ui.components.SectionHeader
import com.wavelength.music.ui.components.QuickAddToPlaylistDialog
import com.wavelength.music.ui.components.TrackCard
import com.wavelength.music.ui.components.TrackOptionsSheet
import com.wavelength.music.ui.components.swipeHorizontal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTrackClick: () -> Unit,
    onGenreClick: (tag: String, label: String) -> Unit,
    onSettingsClick: () -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onSwipeToSearch: () -> Unit = {},
    onSwipeToLibrary: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val featured by viewModel.featured.collectAsStateWithLifecycle()
    val suggested by viewModel.suggested.collectAsStateWithLifecycle()
    val dailyMix by viewModel.dailyMix.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        val density = LocalDensity.current
        val tabSwipeThresholdPx = with(density) { 96.dp.toPx() }
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .swipeHorizontal(
                    thresholdPx = tabSwipeThresholdPx,
                    onSwipeLeft = onSwipeToSearch,
                    onSwipeRight = onSwipeToLibrary
                ),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                )
            }
        ) {
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
                    suggestedTracks = (suggested as? ScreenState.Success)?.data.orEmpty(),
                    dailyMixTracks = (dailyMix as? ScreenState.Success)?.data.orEmpty(),
                    mostPlayed = mostPlayed,
                    recentlyAdded = recentlyAdded,
                    recentlyPlayed = recentlyPlayed,
                    playlists = playlists.filter { !it.isFolder },
                    searchHistory = searchHistory,
                    onTrackClick = { index, queue ->
                        viewModel.playTrack(queue, index)
                        onTrackClick()
                    },
                    onGenreClick = onGenreClick,
                    onPlaylistClick = onPlaylistClick,
                    onSearchHistoryClick = { query ->
                        viewModel.prepareSearch(query)
                        onSearchClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    padding: PaddingValues,
    featuredTracks: List<Track>,
    suggestedTracks: List<Track>,
    dailyMixTracks: List<Track>,
    mostPlayed: List<Track>,
    recentlyAdded: List<Track>,
    recentlyPlayed: List<Track>,
    playlists: List<PlaylistSummary>,
    searchHistory: List<String>,
    onTrackClick: (Int, List<Track>) -> Unit,
    onGenreClick: (String, String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSearchHistoryClick: (String) -> Unit
) {
    var trackForMenu by remember { mutableStateOf<Track?>(null) }
    var trackForQuickAdd by remember { mutableStateOf<Track?>(null) }

    trackForMenu?.let { track ->
        TrackOptionsSheet(track = track, onDismiss = { trackForMenu = null })
    }
    trackForQuickAdd?.let { track ->
        QuickAddToPlaylistDialog(track = track, onDismiss = { trackForQuickAdd = null })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        if (searchHistory.isNotEmpty()) {
            item { SectionHeader("Recent searches") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchHistory, key = { it }) { query ->
                        AssistChip(onClick = { onSearchHistoryClick(query) }, label = { Text(query) })
                    }
                }
            }
        }

        item { SectionHeader("Artists") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(featuredArtists, key = { it }) { artist ->
                    AssistChip(onClick = { onSearchHistoryClick(artist) }, label = { Text(artist) })
                }
            }
        }

        item { SectionHeader(stringResource(R.string.featured_tracks)) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(featuredTracks, key = { _, track -> track.id }) { index, track ->
                    TrackCard(
                        track = track,
                        onClick = { onTrackClick(index, featuredTracks) },
                        onAddToPlaylistClick = { trackForQuickAdd = track },
                        onMoreClick = { trackForMenu = track }
                    )
                }
            }
        }

        item { SectionHeader(stringResource(R.string.genres_moods)) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(genreShortcuts, key = { it }) { tag ->
                    val label = tag.replaceFirstChar { it.uppercase() }
                    AssistChip(onClick = { onGenreClick(tag, label) }, label = { Text(label) })
                }
            }
        }

        if (playlists.isNotEmpty()) {
            item { SectionHeader("Your playlists") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(playlist = playlist, onClick = { onPlaylistClick(playlist.id) })
                    }
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
                    itemsIndexed(recentlyPlayed, key = { _, track -> track.id }) { index, track ->
                        TrackCard(
                            track = track,
                            onClick = { onTrackClick(index, recentlyPlayed) },
                            onAddToPlaylistClick = { trackForQuickAdd = track },
                            onMoreClick = { trackForMenu = track }
                        )
                    }
                }
            }
        }

        if (suggestedTracks.isNotEmpty()) {
            item { SectionHeader("Suggested for you") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(suggestedTracks, key = { _, track -> track.id }) { index, track ->
                        TrackCard(
                            track = track,
                            onClick = { onTrackClick(index, suggestedTracks) },
                            onAddToPlaylistClick = { trackForQuickAdd = track },
                            onMoreClick = { trackForMenu = track }
                        )
                    }
                }
            }
        }

        if (dailyMixTracks.isNotEmpty()) {
            item { SectionHeader("Daily Mix") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(dailyMixTracks, key = { _, track -> track.id }) { index, track ->
                        TrackCard(
                            track = track,
                            onClick = { onTrackClick(index, dailyMixTracks) },
                            onAddToPlaylistClick = { trackForQuickAdd = track },
                            onMoreClick = { trackForMenu = track }
                        )
                    }
                }
            }
        }

        if (mostPlayed.isNotEmpty()) {
            item { SectionHeader("Most played") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(mostPlayed, key = { _, track -> track.id }) { index, track ->
                        TrackCard(
                            track = track,
                            onClick = { onTrackClick(index, mostPlayed) },
                            onAddToPlaylistClick = { trackForQuickAdd = track },
                            onMoreClick = { trackForMenu = track }
                        )
                    }
                }
            }
        }

        if (recentlyAdded.isNotEmpty()) {
            item { SectionHeader("Recently added") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(recentlyAdded, key = { _, track -> track.id }) { index, track ->
                        TrackCard(
                            track = track,
                            onClick = { onTrackClick(index, recentlyAdded) },
                            onAddToPlaylistClick = { trackForQuickAdd = track },
                            onMoreClick = { trackForMenu = track }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

@Composable
private fun PlaylistCard(playlist: PlaylistSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Filled.QueueMusic, contentDescription = null)
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "${playlist.trackCount} tracks",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
