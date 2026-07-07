package com.wavelength.music.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.wavelength.music.data.model.PlaylistSummary

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistSummary>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onCreateNew: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = {
                showCreateDialog = false
                onDismiss()
            },
            onCreate = onCreateNew
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column {
                PlaylistOptionRow(
                    onClick = { showCreateDialog = true },
                    icon = Icons.Filled.Add,
                    label = "New playlist"
                )
                if (playlists.isNotEmpty()) {
                    LazyColumn {
                        items(playlists, key = { it.id }) { playlist ->
                            PlaylistOptionRow(
                                onClick = {
                                    onSelect(playlist.id)
                                    onDismiss()
                                },
                                icon = null,
                                label = "${playlist.name} (${playlist.trackCount})"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PlaylistOptionRow(onClick: () -> Unit, icon: ImageVector?, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
        }
        Text(label)
    }
}
