package com.wavelength.music.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.wavelength.music.R
import com.wavelength.music.data.model.PlaylistSummary
import com.wavelength.music.data.model.Track
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.ErrorView
import com.wavelength.music.ui.components.TrackRow
import com.wavelength.music.ui.components.TrackOptionsSheet
import com.wavelength.music.ui.components.swipeHorizontal
import com.wavelength.music.ui.playlist.AddToPlaylistDialog
import kotlinx.coroutines.launch
import com.wavelength.music.ui.playlist.CreatePlaylistDialog

private val audioPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

@Composable
fun LibraryScreen(
    onTrackClick: () -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSwipeToSearch: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val localSongs by viewModel.localSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val downloadedTracks by viewModel.downloadedTracks.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var currentFolderId by remember { mutableStateOf<Long?>(null) }
    var trackForMenu by remember { mutableStateOf<Track?>(null) }
    var trackForQuickAdd by remember { mutableStateOf<Track?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scanQrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val content = result.contents ?: return@rememberLauncherForActivityResult
        scope.launch {
            viewModel.importPlaylistFromQr(content).fold(
                onSuccess = { count ->
                    Toast.makeText(context, "Imported $count tracks", Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showBulkAddToPlaylist by remember { mutableStateOf(false) }
    val tabs = listOf(
        stringResource(R.string.favorites),
        stringResource(R.string.recently_played),
        "Playlists",
        stringResource(R.string.my_device),
        "Downloads"
    )

    LaunchedEffect(selectedTab) {
        selectionMode = false
        selectedIds = emptySet()
        currentFolderId = null
    }

    val currentTracks = when (selectedTab) {
        0 -> favorites
        1 -> recentlyPlayed
        3 -> localSongs
        4 -> downloadedTracks
        else -> emptyList()
    }
    val selectedTracks = currentTracks.filter { it.id in selectedIds }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

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

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name -> viewModel.createPlaylist(name, currentFolderId) }
        )
    }

    if (showCreateFolderDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = viewModel::createFolder,
            title = "New folder"
        )
    }

    if (showBulkAddToPlaylist) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = {
                showBulkAddToPlaylist = false
                exitSelection()
            },
            onSelect = { playlistId -> viewModel.addTracksToPlaylist(playlistId, selectedTracks) },
            onCreateNew = { name -> viewModel.createPlaylistWithTracks(name, selectedTracks) }
        )
    }

    trackForMenu?.let { track ->
        TrackOptionsSheet(track = track, onDismiss = { trackForMenu = null })
    }

    trackForQuickAdd?.let { track ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { trackForQuickAdd = null },
            onSelect = { playlistId -> viewModel.addTracksToPlaylist(playlistId, listOf(track)) },
            onCreateNew = { name -> viewModel.createPlaylistWithTracks(name, listOf(track)) }
        )
    }

    val density = LocalDensity.current
    val tabSwipeThresholdPx = with(density) { 96.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeHorizontal(
                thresholdPx = tabSwipeThresholdPx,
                // Rows fall back to a plain, non-swipeable TrackRow while selecting (see
                // SwipeableTrackRow's usage below), which would otherwise leave a horizontal drag
                // free to reach this page-level gesture and navigate away mid-selection.
                onSwipeRight = if (selectionMode) null else onSwipeToSearch
            )
    ) {
    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selectedTracks.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { exitSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showBulkAddToPlaylist = true }) {
                            Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                        }
                        if (selectedTab == 0 || selectedTab == 1) {
                            IconButton(onClick = {
                                viewModel.downloadTracks(selectedTracks)
                                exitSelection()
                            }) {
                                Icon(Icons.Filled.Download, contentDescription = "Download")
                            }
                        }
                        when (selectedTab) {
                            0 -> IconButton(onClick = {
                                viewModel.removeFavorites(selectedTracks)
                                exitSelection()
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove from favorites")
                            }
                            1 -> IconButton(onClick = {
                                viewModel.removeTracksFromHistory(selectedTracks)
                                exitSelection()
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove from history")
                            }
                            4 -> IconButton(onClick = {
                                viewModel.removeDownloads(selectedTracks)
                                exitSelection()
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove downloads")
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.nav_library)) },
                    actions = {
                        if (selectedTab == 3 && hasPermission) {
                            IconButton(onClick = viewModel::rescanLocalLibrary) {
                                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.rescan_library))
                            }
                        }
                        if (selectedTab == 2 && currentFolderId == null) {
                            IconButton(onClick = {
                                scanQrLauncher.launch(
                                    ScanOptions()
                                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        .setPrompt("Scan a playlist QR code")
                                        .setBeepEnabled(false)
                                )
                            }) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan playlist QR code")
                            }
                            IconButton(onClick = { showCreateFolderDialog = true }) {
                                Icon(Icons.Filled.CreateNewFolder, contentDescription = "New folder")
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 2 && !selectionMode) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "New playlist")
                }
            }
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

            fun onToggleSelect(track: Track) {
                selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id
            }

            fun onEnterSelection(track: Track) {
                selectionMode = true
                selectedIds = setOf(track.id)
            }

            when (selectedTab) {
                0 -> TrackList(
                    tracks = favorites,
                    isFavoriteTab = true,
                    onTrackClick = { index ->
                        viewModel.playFrom(favorites, index)
                        onTrackClick()
                    },
                    onFavoriteClick = viewModel::removeFavorite,
                    onAddToPlaylistClick = { trackForQuickAdd = it },
                    onMoreClick = { trackForMenu = it },
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    onToggleSelect = ::onToggleSelect,
                    onEnterSelection = ::onEnterSelection
                )
                1 -> TrackList(
                    tracks = recentlyPlayed,
                    isFavoriteTab = false,
                    onTrackClick = { index ->
                        viewModel.playFrom(recentlyPlayed, index)
                        onTrackClick()
                    },
                    onFavoriteClick = null,
                    onAddToPlaylistClick = { trackForQuickAdd = it },
                    onMoreClick = { trackForMenu = it },
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    onToggleSelect = ::onToggleSelect,
                    onEnterSelection = ::onEnterSelection
                )
                2 -> PlaylistList(
                    playlists = playlists,
                    currentFolderId = currentFolderId,
                    onPlaylistClick = onPlaylistClick,
                    onFolderClick = { folderId -> currentFolderId = folderId },
                    onBackFromFolder = { currentFolderId = null },
                    onDelete = viewModel::deletePlaylist
                )
                3 -> when {
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
                        onFavoriteClick = null,
                        onAddToPlaylistClick = { trackForQuickAdd = it },
                        onMoreClick = { trackForMenu = it },
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        onToggleSelect = ::onToggleSelect,
                        onEnterSelection = ::onEnterSelection
                    )
                }
                else -> TrackList(
                    tracks = downloadedTracks,
                    isFavoriteTab = false,
                    onTrackClick = { index ->
                        viewModel.playFrom(downloadedTracks, index)
                        onTrackClick()
                    },
                    onFavoriteClick = null,
                    onAddToPlaylistClick = { trackForQuickAdd = it },
                    onMoreClick = { trackForMenu = it },
                    emptyMessage = "No downloads yet — use a track's three-dot menu to download it",
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    onToggleSelect = ::onToggleSelect,
                    onEnterSelection = ::onEnterSelection
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
    onFavoriteClick: ((Track) -> Unit)?,
    onAddToPlaylistClick: (Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onToggleSelect: (Track) -> Unit,
    onEnterSelection: (Track) -> Unit,
    emptyMessage: String? = null
) {
    if (tracks.isEmpty()) {
        if (emptyMessage != null) {
            EmptyView(modifier = Modifier.fillMaxSize(), message = emptyMessage)
        } else {
            EmptyView(modifier = Modifier.fillMaxSize())
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    track = track,
                    onClick = { if (selectionMode) onToggleSelect(track) else onTrackClick(index) },
                    onLongClick = { onEnterSelection(track) },
                    isFavorite = isFavoriteTab,
                    onFavoriteClick = if (selectionMode) null else onFavoriteClick?.let { { it(track) } },
                    onAddToPlaylistClick = if (selectionMode) null else { { onAddToPlaylistClick(track) } },
                    onMoreClick = if (selectionMode) null else { { onMoreClick(track) } },
                    isSelected = track.id in selectedIds,
                    showSelectionCheckbox = selectionMode
                )
            }
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: List<PlaylistSummary>,
    currentFolderId: Long?,
    onPlaylistClick: (Long) -> Unit,
    onFolderClick: (Long) -> Unit,
    onBackFromFolder: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val visible = playlists.filter { it.parentFolderId == currentFolderId }
    Column(modifier = Modifier.fillMaxSize()) {
        if (currentFolderId != null) {
            val folderName = playlists.firstOrNull { it.id == currentFolderId }?.name.orEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBackFromFolder)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back to all playlists")
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (visible.isEmpty()) {
            val message = if (currentFolderId == null) {
                "No playlists yet — tap + to create one"
            } else {
                "This folder is empty — tap + to add a playlist here"
            }
            EmptyView(modifier = Modifier.fillMaxSize(), message = message)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(visible, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (playlist.isFolder) onFolderClick(playlist.id) else onPlaylistClick(playlist.id)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playlist.isFolder) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!playlist.isFolder) {
                                Text(
                                    text = "${playlist.trackCount} tracks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onDelete(playlist.id) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = if (playlist.isFolder) "Delete folder" else "Delete playlist"
                            )
                        }
                    }
                }
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
