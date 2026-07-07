package com.wavelength.music.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.data.model.Track
import com.wavelength.music.ui.playlist.AddToPlaylistDialog

/** Three-dot track menu shown from every track list in the app: queueing, favorites, playlists,
 * offline download, sharing, and song info. [onRemoveFromPlaylist] is only shown when supplied,
 * for screens (like a playlist's own track list) where that action makes sense. */
@Composable
fun TrackOptionsSheet(
    track: Track,
    onDismiss: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    viewModel: TrackActionsViewModel = hiltViewModel()
) {
    val isFavorite by remember(track.id) { viewModel.isFavorite(track.id) }
        .collectAsStateWithLifecycle(false)
    val isDownloaded by remember(track.id) { viewModel.isDownloaded(track.id) }
        .collectAsStateWithLifecycle(false)
    val downloadingIds by viewModel.downloadingIds.collectAsStateWithLifecycle()
    val isDownloading = track.id in downloadingIds
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = {
                showAddToPlaylist = false
                onDismiss()
            },
            onSelect = { playlistId -> viewModel.addToPlaylist(playlistId, track) },
            onCreateNew = { name -> viewModel.createPlaylistAndAdd(name, track) }
        )
        return
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                showInfo = false
                onDismiss()
            },
            title = { Text(track.name) },
            text = {
                Column {
                    Text("Artist: ${track.artistName}")
                    if (track.albumName.isNotBlank()) {
                        Text("Album: ${track.albumName}")
                    }
                    if (track.durationSeconds > 0) {
                        Text("Duration: ${formatTrackDuration(track.durationSeconds)}")
                    }
                    Text("Source: ${track.source.name.lowercase().replaceFirstChar { it.uppercase() }}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    onDismiss()
                }) { Text("Close") }
            }
        )
        return
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TrackOptionRow(Icons.Filled.PlaylistPlay, "Play next") {
                viewModel.playNext(track)
                onDismiss()
            }
            TrackOptionRow(Icons.Filled.QueueMusic, "Add to queue") {
                viewModel.addToQueue(track)
                onDismiss()
            }
            TrackOptionRow(
                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = if (isFavorite) "Remove from favorites" else "Add to favorites"
            ) {
                viewModel.toggleFavorite(track, isFavorite)
                onDismiss()
            }
            TrackOptionRow(Icons.Filled.PlaylistAdd, "Add to playlist") {
                showAddToPlaylist = true
            }
            when {
                isDownloading -> Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("Downloading…", modifier = Modifier.padding(start = 20.dp))
                }
                isDownloaded -> TrackOptionRow(Icons.Filled.Delete, "Remove download") {
                    viewModel.removeDownload(track)
                    onDismiss()
                }
                else -> TrackOptionRow(Icons.Filled.Download, "Download") {
                    viewModel.download(track)
                }
            }
            TrackOptionRow(Icons.Filled.Share, "Share") {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${track.name} by ${track.artistName}")
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
                onDismiss()
            }
            TrackOptionRow(Icons.Filled.Info, "Song info") {
                showInfo = true
            }
            if (onMoveUp != null) {
                TrackOptionRow(Icons.Filled.KeyboardArrowUp, "Move up") {
                    onMoveUp()
                    onDismiss()
                }
            }
            if (onMoveDown != null) {
                TrackOptionRow(Icons.Filled.KeyboardArrowDown, "Move down") {
                    onMoveDown()
                    onDismiss()
                }
            }
            if (onRemoveFromPlaylist != null) {
                TrackOptionRow(Icons.Filled.Delete, "Remove from playlist") {
                    onRemoveFromPlaylist()
                }
            }
        }
    }
}

@Composable
private fun TrackOptionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatTrackDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}
