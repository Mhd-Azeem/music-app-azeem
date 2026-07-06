package com.wavelength.music.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wavelength.music.R
import com.wavelength.music.data.model.Album
import com.wavelength.music.data.model.Artist
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.ErrorView
import com.wavelength.music.ui.components.LoadingView
import com.wavelength.music.ui.components.ScreenState
import com.wavelength.music.ui.components.SectionHeader
import com.wavelength.music.ui.components.TrackRow

@Composable
fun SearchScreen(
    onTrackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = results) {
            is ScreenState.Loading -> LoadingView(modifier = Modifier.padding(padding))
            is ScreenState.Error -> ErrorView(
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding),
                message = state.message
            )
            is ScreenState.Empty -> EmptyView(
                modifier = Modifier.padding(padding),
                message = if (query.isBlank()) stringResource(R.string.empty_results) else stringResource(R.string.search_empty_hint)
            )
            is ScreenState.Success -> {
                val data = state.data
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (data.artists.isNotEmpty()) {
                        item { SectionHeader("Artists") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(data.artists) { artist ->
                                    ArtistChip(artist = artist, onClick = { onArtistClick(artist.id) })
                                }
                            }
                        }
                    }
                    if (data.albums.isNotEmpty()) {
                        item { SectionHeader("Albums") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(data.albums) { album ->
                                    AlbumChip(album = album, onClick = { onAlbumClick(album.id) })
                                }
                            }
                        }
                    }
                    if (data.tracks.isNotEmpty()) {
                        item { SectionHeader("Tracks") }
                        if (data.jioSaavnUnavailable) {
                            item {
                                Text(
                                    text = stringResource(R.string.jiosaavn_unavailable),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                        itemsIndexed(data.tracks) { index, track ->
                            TrackRow(
                                track = track,
                                onClick = {
                                    viewModel.playTrack(data.tracks, index)
                                    onTrackClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistChip(artist: Artist, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.imageUrl,
            contentDescription = artist.name,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp).size(width = 88.dp, height = 16.dp)
        )
    }
}

@Composable
private fun AlbumChip(album: Album, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = album.imageUrl,
            contentDescription = album.name,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            text = album.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp).size(width = 100.dp, height = 16.dp)
        )
    }
}
