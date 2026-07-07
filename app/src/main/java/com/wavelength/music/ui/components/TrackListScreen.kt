package com.wavelength.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wavelength.music.R
import com.wavelength.music.data.model.Track

@Composable
fun TrackListScreen(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    state: ScreenState<List<Track>>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayAll: () -> Unit,
    onTrackClick: (Int) -> Unit,
    emptyMessage: String? = null,
    onRemoveFromPlaylist: ((Track) -> Unit)? = null
) {
    var trackForMenu by remember { mutableStateOf<Track?>(null) }

    trackForMenu?.let { track ->
        TrackOptionsSheet(
            track = track,
            onDismiss = { trackForMenu = null },
            onRemoveFromPlaylist = onRemoveFromPlaylist?.let {
                {
                    it(track)
                    trackForMenu = null
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (state) {
            is ScreenState.Loading -> LoadingView(modifier = Modifier.padding(padding))
            is ScreenState.Error -> ErrorView(
                onRetry = onRetry,
                modifier = Modifier.padding(padding),
                message = state.message
            )
            is ScreenState.Empty -> if (emptyMessage != null) {
                EmptyView(modifier = Modifier.padding(padding), message = emptyMessage)
            } else {
                EmptyView(modifier = Modifier.padding(padding))
            }
            is ScreenState.Success -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = title,
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Button(
                            onClick = onPlayAll,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text(
                                text = stringResource(R.string.play_all),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
                itemsIndexed(state.data) { index, track ->
                    TrackRow(
                        track = track,
                        onClick = { onTrackClick(index) },
                        onMoreClick = { trackForMenu = track }
                    )
                }
            }
        }
    }
}
