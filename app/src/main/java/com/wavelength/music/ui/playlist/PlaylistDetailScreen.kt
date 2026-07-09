package com.wavelength.music.ui.playlist

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.ui.components.TrackListScreen
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onTrackClick: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val state by viewModel.tracks.collectAsStateWithLifecycle()
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    qrBitmap?.let { bitmap ->
        AlertDialog(
            onDismissRequest = { qrBitmap = null },
            title = { Text("Scan to import") },
            text = {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR code for $name",
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { qrBitmap = null }) { Text("Close") }
            }
        )
    }

    qrError?.let { message ->
        AlertDialog(
            onDismissRequest = { qrError = null },
            title = { Text("Couldn't create QR code") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { qrError = null }) { Text("OK") }
            }
        )
    }

    TrackListScreen(
        title = name,
        subtitle = null,
        imageUrl = null,
        state = state,
        onBack = onBack,
        onRetry = {},
        onPlayAll = {
            viewModel.playAll()
            onTrackClick()
        },
        onTrackClick = { index ->
            viewModel.playTrack(index)
            onTrackClick()
        },
        onRemoveFromPlaylist = viewModel::removeTrack,
        onReorder = viewModel::moveTrack,
        actions = {
            IconButton(onClick = {
                scope.launch {
                    viewModel.generateQrCode().fold(
                        onSuccess = { bitmap -> qrBitmap = bitmap },
                        onFailure = { e -> qrError = e.message ?: "Something went wrong" }
                    )
                }
            }) {
                Icon(Icons.Filled.QrCode, contentDescription = "Share via QR code")
            }
        }
    )
}
