package com.wavelength.music.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.PlaylistSummary
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs [TrackOptionsSheet]; shared across every screen that shows a track's three-dot menu so
 * favorite/download/playlist state and playback actions stay consistent everywhere. */
@HiltViewModel
class TrackActionsViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistSummary>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()

    fun isFavorite(trackId: String): Flow<Boolean> = repository.isFavorite(trackId)

    fun isDownloaded(trackId: String): Flow<Boolean> = repository.isDownloaded(trackId)

    fun toggleFavorite(track: Track, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch { repository.toggleFavorite(track, isCurrentlyFavorite) }
    }

    fun addToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch { repository.addTrackToPlaylist(playlistId, track) }
    }

    fun createPlaylistAndAdd(name: String, track: Track) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            repository.addTrackToPlaylist(id, track)
        }
    }

    fun playNext(track: Track) = playerController.playNext(track)

    fun addToQueue(track: Track) = playerController.addToQueue(track)

    fun download(track: Track) {
        viewModelScope.launch {
            _downloadingIds.update { it + track.id }
            repository.downloadTrack(track)
            _downloadingIds.update { it - track.id }
        }
    }

    fun removeDownload(track: Track) {
        viewModelScope.launch { repository.removeDownload(track.id) }
    }
}
