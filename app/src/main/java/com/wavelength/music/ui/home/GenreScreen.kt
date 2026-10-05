package com.wavelength.music.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.R
import com.wavelength.music.ui.components.TrackListScreen

@Composable
fun GenreScreen(
    onBack: () -> Unit,
    onTrackClick: () -> Unit,
    onBrowseArtist: (String) -> Unit = {},
    onBrowseAlbum: (String) -> Unit = {},
    isArtistPage: Boolean = false,
    viewModel: GenreViewModel = hiltViewModel()
) {
    val state by viewModel.tracks.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isFavoriteAlbum by viewModel.isFavoriteAlbum.collectAsStateWithLifecycle()
    val artistLanguages = listOf("All", "Tamil", "Malayalam", "Hindi", "English", "Telugu", "Kannada")

    TrackListScreen(
        title = viewModel.label,
        subtitle = if (isArtistPage) "Top songs" else null,
        imageUrl = null,
        state = state,
        onBack = onBack,
        onRetry = viewModel::load,
        onPlayAll = {
            viewModel.playAll()
            onTrackClick()
        },
        onShuffle = {
            viewModel.shuffleAll()
            onTrackClick()
        },
        onTrackClick = { index ->
            viewModel.playTrack(index)
            onTrackClick()
        },
        emptyMessage = if (isArtistPage && selectedLanguage != "All") {
            "No songs found in this language"
        } else {
            stringResource(R.string.search_empty_hint)
        },
        onLoadMore = viewModel::loadMore,
        isLoadingMore = isLoadingMore,
        onRemoveFromPlaylist = viewModel::hideTrack,
        onBrowseArtist = onBrowseArtist,
        onBrowseAlbum = onBrowseAlbum,
        headerExtra = if (isArtistPage) {
            {
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    artistLanguages.forEach { language ->
                        FilterChip(
                            selected = selectedLanguage == language,
                            onClick = { viewModel.setLanguage(language) },
                            label = { Text(language) }
                        )
                    }
                }
            }
        } else null,
        actions = {
            if (!isArtistPage) {
                IconButton(onClick = viewModel::toggleFavoriteAlbum) {
                    Icon(
                        if (isFavoriteAlbum) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavoriteAlbum) "Remove favorite album" else "Add favorite album"
                    )
                }
            }
        }
    )
}
