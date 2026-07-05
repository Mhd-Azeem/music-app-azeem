package com.wavelength.music.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {

    val favorites: StateFlow<List<Track>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Track>> = repository.observeRecentlyPlayed(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playFrom(queue: List<Track>, index: Int) {
        playerController.playQueue(queue, index)
    }

    fun removeFavorite(track: Track) {
        viewModelScope.launch { repository.toggleFavorite(track, isCurrentlyFavorite = true) }
    }
}
