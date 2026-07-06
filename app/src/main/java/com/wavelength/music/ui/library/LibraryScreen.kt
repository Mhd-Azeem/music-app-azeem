package com.wavelength.music.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.R
import com.wavelength.music.data.model.Track
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.ErrorView
import com.wavelength.music.ui.components.TrackRow

private val audioPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

@Composable
fun LibraryScreen(
    onTrackClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val localSongs by viewModel.localSongs.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.favorites),
        stringResource(R.string.recently_played),
        stringResource(R.string.my_device)
    )

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.rescanLocalLibrary()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && localSongs.isEmpty()) {
            viewModel.rescanLocalLibrary()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_library)) },
                actions = {
                    if (selectedTab == 2 && hasPermission) {
                        IconButton(onClick = viewModel::rescanLocalLibrary) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.rescan_library))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> TrackList(
                    tracks = favorites,
                    isFavoriteTab = true,
                    onTrackClick = { index ->
                        viewModel.playFrom(favorites, index)
                        onTrackClick()
                    },
                    onFavoriteClick = viewModel::removeFavorite
                )
                1 -> TrackList(
                    tracks = recentlyPlayed,
                    isFavoriteTab = false,
                    onTrackClick = { index ->
                        viewModel.playFrom(recentlyPlayed, index)
                        onTrackClick()
                    },
                    onFavoriteClick = null
                )
                else -> when {
                    !hasPermission -> PermissionPrompt { permissionLauncher.launch(audioPermission) }
                    isScanning -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    else -> TrackList(
                        tracks = localSongs,
                        isFavoriteTab = false,
                        onTrackClick = { index ->
                            viewModel.playFrom(localSongs, index)
                            onTrackClick()
                        },
                        onFavoriteClick = null
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    isFavoriteTab: Boolean,
    onTrackClick: (Int) -> Unit,
    onFavoriteClick: ((Track) -> Unit)?
) {
    if (tracks.isEmpty()) {
        EmptyView(modifier = Modifier.fillMaxSize())
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(tracks) { index, track ->
                TrackRow(
                    track = track,
                    onClick = { onTrackClick(index) },
                    isFavorite = isFavoriteTab,
                    onFavoriteClick = onFavoriteClick?.let { { it(track) } }
                )
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    ErrorView(
        onRetry = onRequestPermission,
        modifier = Modifier.fillMaxSize(),
        message = stringResource(R.string.local_permission_rationale),
        actionLabel = stringResource(R.string.grant_permission)
    )
}
