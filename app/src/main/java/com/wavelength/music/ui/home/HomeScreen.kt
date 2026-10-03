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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.R
import com.wavelength.music.data.model.PlaylistSummary
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.VisualThemeMode
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.ErrorView
import com.wavelength.music.ui.components.LoadingView
import com.wavelength.music.ui.components.ScreenState
import com.wavelength.music.ui.components.SectionHeader
import com.wavelength.music.ui.components.QuickAddToPlaylistDialog
import com.wavelength.music.ui.components.TrackCard
import com.wavelength.music.ui.components.TrackOptionsSheet
import com.wavelength.music.ui.components.swipeHorizontal
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.wavelength.music.ui.components.TrackRow
import com.wavelength.music.ui.design.UiDesignConfig
import com.wavelength.music.ui.settings.AppSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTrackClick: () -> Unit,
    onGenreClick: (tag: String, label: String) -> Unit,
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onStartListeningViewAll: () -> Unit,
    onSwipeToSearch: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val featured by viewModel.featured.collectAsStateWithLifecycle()
    val suggested by viewModel.suggested.collectAsStateWithLifecycle()
    val dailyMix by viewModel.dailyMix.collectAsStateWithLifecycle()
    val topCharts by viewModel.topCharts.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val settingsViewModel: AppSettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val headerBrush = when (settings.visualThemeMode) {
                    VisualThemeMode.SOLID -> Brush.linearGradient(
                        listOf(Color(0xFF181818), Color(0xFF101010))
                    )
                    VisualThemeMode.LIQUID -> Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.58f),
                            Color(0xCC29436B),
                            Color(0xAA142033)
                        )
                    )
                    VisualThemeMode.GLASSMORPHISM -> Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                            Color(0x66152D48)
                        )
                    )
                    VisualThemeMode.NEOMORPHISM -> Brush.linearGradient(
                        listOf(Color(0xFF38414F), Color(0xFF272E39), Color(0xFF1A1F27))
                    )
                    VisualThemeMode.AMOLED -> Brush.linearGradient(
                        listOf(Color.Black, Color(0xFF050505))
                    )
                    VisualThemeMode.ALBUM_ADAPTIVE -> Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                            Color(0xCC090C12)
                        )
                    )
                    VisualThemeMode.AURORA -> Brush.linearGradient(
                        listOf(
                            Color(0xDD4338CA),
                            Color(0xDD7E22CE),
                            Color(0xBB0891B2)
                        )
                    )
                }

                val headerRadius = when (settings.visualThemeMode) {
                    VisualThemeMode.SOLID -> 10.dp
                    VisualThemeMode.LIQUID -> 28.dp
                    VisualThemeMode.GLASSMORPHISM -> 32.dp
                    VisualThemeMode.NEOMORPHISM -> 20.dp
                    VisualThemeMode.AMOLED -> 3.dp
                    VisualThemeMode.ALBUM_ADAPTIVE -> 22.dp
                    VisualThemeMode.AURORA -> 34.dp
                }
                val headerShape = RoundedCornerShape(headerRadius)
                val headerBorder = when (settings.visualThemeMode) {
                    VisualThemeMode.SOLID -> MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                    VisualThemeMode.LIQUID -> Color.White.copy(alpha = 0.34f)
                    VisualThemeMode.GLASSMORPHISM -> Color.White.copy(alpha = 0.60f)
                    VisualThemeMode.NEOMORPHISM -> Color.White.copy(alpha = 0.10f)
                    VisualThemeMode.AMOLED -> Color.White.copy(alpha = 0.22f)
                    VisualThemeMode.ALBUM_ADAPTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.96f)
                    VisualThemeMode.AURORA -> Color.White.copy(alpha = 0.72f)
                }
                val headerElevation = when (settings.visualThemeMode) {
                    VisualThemeMode.AMOLED -> 0.dp
                    VisualThemeMode.SOLID -> 4.dp
                    VisualThemeMode.NEOMORPHISM -> 18.dp
                    VisualThemeMode.AURORA -> 20.dp
                    VisualThemeMode.ALBUM_ADAPTIVE -> 14.dp
                    else -> 12.dp
                }
                val settingsShape = when (settings.visualThemeMode) {
                    VisualThemeMode.SOLID -> RoundedCornerShape(10.dp)
                    VisualThemeMode.LIQUID -> RoundedCornerShape(26.dp)
                    VisualThemeMode.GLASSMORPHISM -> CircleShape
                    VisualThemeMode.NEOMORPHISM -> RoundedCornerShape(16.dp)
                    VisualThemeMode.AMOLED -> RoundedCornerShape(2.dp)
                    VisualThemeMode.ALBUM_ADAPTIVE -> RoundedCornerShape(18.dp)
                    VisualThemeMode.AURORA -> CircleShape
                }

                Box(
                    modifier = Modifier
                        .shadow(
                            headerElevation,
                            headerShape,
                            ambientColor = if (settings.visualThemeMode == VisualThemeMode.AURORA) Color(0xFF7C3AED) else Color.Black,
                            spotColor = if (settings.visualThemeMode == VisualThemeMode.ALBUM_ADAPTIVE) MaterialTheme.colorScheme.primary else Color.Black
                        )
                        .clip(headerShape)
                        .background(headerBrush)
                        .border(
                            if (settings.visualThemeMode == VisualThemeMode.AMOLED) 1.dp else 1.3.dp,
                            headerBorder,
                            headerShape
                        )
                        .padding(
                            horizontal = when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED, VisualThemeMode.SOLID -> 14.dp
                                VisualThemeMode.AURORA -> 20.dp
                                else -> 18.dp
                            },
                            vertical = when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED -> 9.dp
                                else -> 11.dp
                            }
                        )
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .shadow(
                            headerElevation,
                            headerShape,
                            ambientColor = if (settings.visualThemeMode == VisualThemeMode.AURORA) Color(0xFF7C3AED) else Color.Black,
                            spotColor = if (settings.visualThemeMode == VisualThemeMode.ALBUM_ADAPTIVE) MaterialTheme.colorScheme.primary else Color.Black
                        )
                        .clip(headerShape)
                        .background(headerBrush)
                        .border(
                            if (settings.visualThemeMode == VisualThemeMode.AMOLED) 1.dp else 1.3.dp,
                            headerBorder,
                            headerShape
                        )
                        .clickable(onClick = onStatisticsClick)
                        .padding(
                            horizontal = when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED, VisualThemeMode.SOLID -> 13.dp
                                else -> 17.dp
                            },
                            vertical = when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED -> 9.dp
                                else -> 11.dp
                            }
                        )
                ) {
                    Text(
                        "Stats",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Box(
                    modifier = Modifier
                        .size(
                            when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED -> 44.dp
                                VisualThemeMode.AURORA -> 52.dp
                                else -> 48.dp
                            }
                        )
                        .shadow(
                            headerElevation,
                            settingsShape,
                            ambientColor = if (settings.visualThemeMode == VisualThemeMode.AURORA) Color(0xFF7C3AED) else Color.Black,
                            spotColor = if (settings.visualThemeMode == VisualThemeMode.ALBUM_ADAPTIVE) MaterialTheme.colorScheme.primary else Color.Black
                        )
                        .clip(settingsShape)
                        .background(headerBrush)
                        .border(
                            if (settings.visualThemeMode == VisualThemeMode.AMOLED) 1.dp else 1.3.dp,
                            headerBorder,
                            settingsShape
                        )
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(
                            when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED -> 22.dp
                                VisualThemeMode.AURORA -> 27.dp
                                else -> 25.dp
                            }
                        )
                    )
                }
            }
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
                    onSwipeLeft = onSwipeToSearch
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
                    topCharts = topCharts,
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
                    },
                    onStartListeningViewAll = onStartListeningViewAll
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
    topCharts: Map<String, List<Track>>,
    mostPlayed: List<Track>,
    recentlyAdded: List<Track>,
    recentlyPlayed: List<Track>,
    playlists: List<PlaylistSummary>,
    searchHistory: List<String>,
    onTrackClick: (Int, List<Track>) -> Unit,
    onGenreClick: (String, String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSearchHistoryClick: (String) -> Unit,
    onStartListeningViewAll: () -> Unit
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
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AssistChip(
                        onClick = { },
                        label = { Text("All") },
                        leadingIcon = { Text("✓") }
                    )
                }
                items(listOf("Tamil", "Hindi", "English", "Malayalam", "Telugu"), key = { it }) { language ->
                    AssistChip(
                        onClick = { onGenreClick(language.lowercase(), language) },
                        label = { Text(language) }
                    )
                }
            }
        }

        item { SectionHeader("Quick picks") }
        featuredTracks.take(6).chunked(2).forEach { pair ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { track ->
                        QuickTrackTile(
                            track = track,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val index = featuredTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                onTrackClick(index, featuredTracks)
                            }
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        val starterTracks = if (dailyMixTracks.isNotEmpty()) dailyMixTracks else featuredTracks
        if (starterTracks.isNotEmpty()) {
            item { SectionHeader("To get you started") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(starterTracks.take(10), key = { _, track -> "starter:" + track.id }) { index, track ->
                        TrackCard(
                            track = track,
                            onClick = { onTrackClick(index, starterTracks) },
                            onAddToPlaylistClick = { trackForQuickAdd = track },
                            onMoreClick = { trackForMenu = track }
                        )
                    }
                }
            }
        }

        val startListening = if (recentlyPlayed.isNotEmpty()) recentlyPlayed else featuredTracks
        if (startListening.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Start listening", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onStartListeningViewAll) { Text("View all") }
                }
            }
            itemsIndexed(startListening.take(5), key = { _, track -> "start:" + track.id }) { index, track ->
                TrackRow(
                    track = track,
                    onClick = { onTrackClick(index, startListening) },
                    onAddToPlaylistClick = { trackForQuickAdd = track },
                    onMoreClick = { trackForMenu = track }
                )
            }
        }

        val todayTracks = if (suggestedTracks.isNotEmpty()) suggestedTracks else featuredTracks
        if (todayTracks.isNotEmpty()) {
            item { SectionHeader("Recommended for today") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(todayTracks.take(12), key = { _, track -> "today:" + track.id }) { index, track ->
                        TrackCard(
                            track = track,
                            onClick = { onTrackClick(index, todayTracks) },
                            onAddToPlaylistClick = { trackForQuickAdd = track },
                            onMoreClick = { trackForMenu = track }
                        )
                    }
                }
            }
        }

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

        artistGroups.forEach { (language, artists) ->
            item { SectionHeader("$language artists") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(artists, key = { language + ":" + it }) { artist ->
                        ArtistCircleTile(
                            artist = artist,
                            onClick = { onGenreClick(artist, artist) }
                        )
                    }
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

        topCharts.forEach { (chartName, chartTracks) ->
            if (chartTracks.isNotEmpty()) {
                item { SectionHeader(chartName) }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(chartTracks, key = { _, track -> chartName + ":" + track.id }) { index, track ->
                            TrackCard(
                                track = track,
                                onClick = { onTrackClick(index, chartTracks) },
                                onAddToPlaylistClick = { trackForQuickAdd = track },
                                onMoreClick = { trackForMenu = track }
                            )
                        }
                    }
                }
            }
        }

        if (suggestedTracks.isNotEmpty()) {
            item { SectionHeader("Based on your recent listening") }
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
        shape = RoundedCornerShape(UiDesignConfig.PLAYLIST_CARD_RADIUS_DP.dp),
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


@Composable
private fun QuickTrackTile(
    track: Track,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(UiDesignConfig.QUICK_TILE_HEIGHT_DP.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(UiDesignConfig.QUICK_TILE_RADIUS_DP.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = track.albumArtUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(UiDesignConfig.QUICK_TILE_HEIGHT_DP.dp)
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 9.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    track.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artistName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


private val artistGroups = linkedMapOf(
    "🎵 Tamil" to listOf(
        "Anirudh Ravichander", "A. R. Rahman", "Yuvan Shankar Raja", "Sai Abhyankkar",
        "Hiphop Tamizha", "G. V. Prakash Kumar", "Harris Jayaraj", "Santhosh Narayanan",
        "Ilaiyaraaja", "Dhanush", "Sid Sriram", "Pradeep Kumar"
    ),
    "🌎 English" to listOf(
        "The Weeknd", "Taylor Swift", "Bruno Mars", "Ed Sheeran", "Billie Eilish",
        "Ariana Grande", "Justin Bieber", "Dua Lipa", "Drake", "Post Malone",
        "Sabrina Carpenter", "Olivia Rodrigo"
    ),
    "🌴 Malayalam" to listOf(
        "Sushin Shyam", "Vineeth Sreenivasan", "K. S. Harisankar", "Shaan Rahman",
        "Gopi Sundar", "Jakes Bejoy", "Hesham Abdul Wahab", "Vijay Yesudas",
        "K. S. Chithra", "Dabzee", "Fejo", "Sithara Krishnakumar"
    ),
    "🇮🇳 Hindi" to listOf(
        "Arijit Singh", "Shreya Ghoshal", "Pritam", "Vishal Mishra", "Jubin Nautiyal",
        "Sonu Nigam", "Atif Aslam", "Armaan Malik", "Amit Trivedi", "Badshah",
        "Yo Yo Honey Singh", "Diljit Dosanjh"
    )
)

@Composable
private fun ArtistCircleTile(artist: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(88.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val portraitUrl = artistPortraits[artist]
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Initials stay behind the image, so a failed/slow network image still has
            // a useful visual fallback instead of an empty circle.
            Text(
                text = artist.split(" ").filter { it.isNotBlank() }.take(2)
                    .joinToString("") { it.first().uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            if (portraitUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(portraitUrl)
                        .addHeader("User-Agent", "AzMusic/1.0 (https://github.com/Mhd-Azeem/music-app-azeem)")
                        .crossfade(true)
                        .build(),
                    contentDescription = "$artist portrait",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
        Text(
            text = artist,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
    }
}


/*
 * Tamil artist portrait pilot. These URLs resolve through Wikimedia Commons to freely
 * reusable images. Artists without a verified Commons portrait deliberately fall back
 * to the initials tile until a suitable licensed image is available.
 */
private val artistPortraits = mapOf(
    // Tamil
    "Anirudh Ravichander" to "https://upload.wikimedia.org/wikipedia/commons/d/d4/Anirudh_Ravichander_at_Audi_Ritz_Style_Awards_2017_%28cropped%29.jpg",
    "A. R. Rahman" to "https://upload.wikimedia.org/wikipedia/commons/0/07/A._R._Rahman.jpg",
    "Yuvan Shankar Raja" to "https://upload.wikimedia.org/wikipedia/commons/b/b6/Yuvan_Shankar_Raja_exclusive_HQ_Photos_Silverscreen.jpg",
    "Hiphop Tamizha" to "https://upload.wikimedia.org/wikipedia/commons/9/96/Hiphop_Tamizha_Adhi_-_Hiphop_Tamizha_Aambala_audio_launch_%28cropped%29.jpg",
    "G. V. Prakash Kumar" to "https://upload.wikimedia.org/wikipedia/commons/2/20/GV_Prakash_Kumar.webp",
    "Harris Jayaraj" to "https://upload.wikimedia.org/wikipedia/commons/9/9d/Harris_Jayaraj.jpg",
    "Santhosh Narayanan" to "https://upload.wikimedia.org/wikipedia/commons/9/99/Santhosh_Narayanan.png",
    "Ilaiyaraaja" to "https://upload.wikimedia.org/wikipedia/commons/2/26/Ilaiyaraaja_at_Merku_Thodarchi_Malai_Press_Meet.jpg",
    "Dhanush" to "https://upload.wikimedia.org/wikipedia/commons/8/86/Dhanush_at_the_%E2%80%98Asuran%E2%80%99_Success_Meet.jpg",
    "Sid Sriram" to "https://upload.wikimedia.org/wikipedia/commons/c/c3/Sid_Sriram.jpg",

    // English
    "The Weeknd" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/The%20Weeknd%20Jimmy%20Fallon%202025%20(cropped).png",
    "Taylor Swift" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Taylor%20Swift%20at%20the%202023%20MTV%20Video%20Music%20Awards%204.png",
    "Bruno Mars" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Bruno%20Mars%20Las%20Vegas%202010.jpg",
    "Ed Sheeran" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Ed%20Sheeran-6886%20(48712908917).jpg",
    "Billie Eilish" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Billie%20Eilish%202019%20by%20Glenn%20Francis.jpg",
    "Ariana Grande" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Ariana%20Grande%20during%20the%20Sweetener%20World%20Tour%20in%202019.png",
    "Justin Bieber" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Justin%20Bieber%20in%202015.jpg",
    "Dua Lipa" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/DuaLipaO2020522%20(7%20of%2030)%20(52090543112)%20(cropped).jpg",
    "Drake" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Drake%20July%202016.jpg",
    "Post Malone" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Post%20Malone%20June%202018.jpg",
    "Sabrina Carpenter" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Sabrina%20Carpenter%20Vogue%202025%20(cropped).jpg",
    "Olivia Rodrigo" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Olivia%20Rodrigo%20@%20Theatre%20at%20Ace%20Hotel%2010%2009%202023%20(53476356280).jpg",

    // Malayalam
    "Vineeth Sreenivasan" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Vineeth%20Sreenivasan.jpg",
    "Vijay Yesudas" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Vijay%20Yesudas.jpg",
    "K. S. Chithra" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/K.%20S.%20Chithra%20at%20Vijay%20Awards.jpg",
    "Sithara Krishnakumar" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Sithara%20Krishnakumar.jpg",

    // Hindi
    "Arijit Singh" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Arijit%20Singh%20(cropped).jpg",
    "Shreya Ghoshal" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Shreya%20Ghoshal%20at%20the%2066th%20Filmfare%20Awards.jpg",
    "Sonu Nigam" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Sonu%20Nigam%20at%20the%20Mirchi%20Music%20Awards%202016.jpg",
    "Atif Aslam" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Atif%20Aslam%20at%20Badlapur%20success%20bash.jpg",
    "Armaan Malik" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Armaan%20Malik%20at%20the%20Global%20Indian%20Music%20Academy%20Awards.jpg",
    "Amit Trivedi" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Amit%20Trivedi%20at%20the%2061st%20Filmfare%20Awards.jpg",
    "Badshah" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Badshah%20at%20the%20HT%20Most%20Stylish%20Awards%202018.jpg",
    "Yo Yo Honey Singh" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Yo%20Yo%20Honey%20Singh%20at%20the%20Mirchi%20Music%20Awards%202014.jpg",
    "Diljit Dosanjh" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Diljit%20Dosanjh%20at%20the%20trailer%20launch%20of%20Phillauri.jpg"
)
