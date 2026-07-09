package com.wavelength.music.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.ArtistStat
import com.wavelength.music.data.model.ListeningStats
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    repository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {

    val stats: StateFlow<ListeningStats> = repository.observeListeningStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListeningStats(0, 0, 0))

    val topArtists: StateFlow<List<ArtistStat>> = repository.observeTopArtists(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topTracks: StateFlow<List<Track>> = repository.observeTopTracks(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playTrack(queue: List<Track>, index: Int) = playerController.playQueue(queue, index)
}
