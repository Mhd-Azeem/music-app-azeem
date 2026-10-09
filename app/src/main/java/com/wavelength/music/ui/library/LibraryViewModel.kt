package com.wavelength.music.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.PlaylistSummary
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.data.repository.QrPlaylistRepository
import com.wavelength.music.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DeviceSongSort {
    NAME,
    SIZE,
    MODIFIED_DATE
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController,
    private val qrPlaylistRepository: QrPlaylistRepository
) : ViewModel() {

    val favorites: StateFlow<List<Track>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Track>> = repository.observeRecentlyPlayed(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localSongs: StateFlow<List<Track>> = repository.observeLocalSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deviceSearchQuery = MutableStateFlow("")
    val deviceSearchQuery: StateFlow<String> = _deviceSearchQuery.asStateFlow()

    private val _deviceSort = MutableStateFlow(DeviceSongSort.NAME)
    val deviceSort: StateFlow<DeviceSongSort> = _deviceSort.asStateFlow()

    private val _deviceSortAscending = MutableStateFlow(true)
    val deviceSortAscending: StateFlow<Boolean> = _deviceSortAscending.asStateFlow()

    val deviceSongs: StateFlow<List<Track>> = combine(
        localSongs,
        _deviceSearchQuery,
        _deviceSort,
        _deviceSortAscending
    ) { tracks, query, sort, ascending ->
        val normalizedQuery = query.trim()
        val filtered = if (normalizedQuery.isBlank()) {
            tracks
        } else {
            tracks.filter { track ->
                track.name.contains(normalizedQuery, ignoreCase = true) ||
                    track.artistName.contains(normalizedQuery, ignoreCase = true) ||
                    track.albumName.contains(normalizedQuery, ignoreCase = true)
            }
        }

        val comparator = when (sort) {
            DeviceSongSort.NAME -> compareBy<Track> { it.name.lowercase() }
            DeviceSongSort.SIZE -> compareBy { it.fileSizeBytes }
            DeviceSongSort.MODIFIED_DATE -> compareBy { it.modifiedAtSeconds }
        }
        if (ascending) filtered.sortedWith(comparator) else filtered.sortedWith(comparator.reversed())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistSummary>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedTracks: StateFlow<List<Track>> = repository.observeDownloadedTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun createPlaylist(name: String, parentFolderId: Long? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.createPlaylist(trimmed, parentFolderId) }
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.createFolder(trimmed) }
    }

    fun movePlaylistToFolder(playlistId: Long, folderId: Long?) {
        viewModelScope.launch { repository.movePlaylistToFolder(playlistId, folderId) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { repository.deletePlaylist(playlistId) }
    }

    suspend fun importPlaylistFromQr(content: String): Result<Int> =
        qrPlaylistRepository.importFromQr(content)

    fun playFrom(queue: List<Track>, index: Int) {
        playerController.playQueue(queue, index)
    }

    fun removeFavorite(track: Track) {
        viewModelScope.launch { repository.toggleFavorite(track, isCurrentlyFavorite = true) }
    }

    fun toggleFavoriteQuick(track: Track) {
        viewModelScope.launch { repository.toggleFavoriteAuto(track) }
    }

    fun removeFromHistory(track: Track) {
        viewModelScope.launch { repository.removeFromRecentlyPlayed(track.id) }
    }

    fun removeDownload(track: Track) {
        viewModelScope.launch { repository.removeDownload(track.id) }
    }

    /** Re-scans MediaStore for on-device audio. Call once permission is granted, and whenever
     * the user taps "Rescan library" afterwards. */
    fun setDeviceSearchQuery(query: String) {
        _deviceSearchQuery.value = query
    }

    fun setDeviceSort(sort: DeviceSongSort) {
        _deviceSort.value = sort
    }

    fun setDeviceSortAscending(ascending: Boolean) {
        _deviceSortAscending.value = ascending
    }

    fun rescanLocalLibrary() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            repository.rescanLocalLibrary()
            _isScanning.value = false
        }
    }

    // --- Multi-select bulk actions ---------------------------------------------------------------

    fun addTracksToPlaylist(playlistId: Long, tracks: List<Track>) {
        viewModelScope.launch { tracks.forEach { repository.addTrackToPlaylist(playlistId, it) } }
    }

    fun createPlaylistWithTracks(name: String, tracks: List<Track>) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            tracks.forEach { repository.addTrackToPlaylist(id, it) }
        }
    }

    fun downloadTracks(tracks: List<Track>) {
        viewModelScope.launch { tracks.forEach { repository.downloadTrack(it) } }
    }

    fun removeFavorites(tracks: List<Track>) {
        viewModelScope.launch { tracks.forEach { repository.toggleFavorite(it, isCurrentlyFavorite = true) } }
    }

    fun removeTracksFromHistory(tracks: List<Track>) {
        viewModelScope.launch { tracks.forEach { repository.removeFromRecentlyPlayed(it.id) } }
    }

    fun removeDownloads(tracks: List<Track>) {
        viewModelScope.launch { tracks.forEach { repository.removeDownload(it.id) } }
    }
}
