package com.wavelength.music.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.data.model.Track
import com.wavelength.music.ui.playlist.AddToPlaylistDialog

/** Lets a track be added to a playlist directly from a "+" quick-action button, without opening
 * the full three-dot [TrackOptionsSheet]. Shares [TrackActionsViewModel] so playlist state and
 * the add/create actions stay consistent with the three-dot menu's own "Add to playlist" entry. */
@Composable
fun QuickAddToPlaylistDialog(
    track: Track,
    onDismiss: () -> Unit,
    viewModel: TrackActionsViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    AddToPlaylistDialog(
        playlists = playlists,
        onDismiss = onDismiss,
        onSelect = { playlistId -> viewModel.addToPlaylist(playlistId, track) },
        onCreateNew = { name -> viewModel.createPlaylistAndAdd(name, track) }
    )
}
