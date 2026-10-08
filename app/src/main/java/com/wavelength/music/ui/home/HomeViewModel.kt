package com.wavelength.music.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.ArtistStat
import com.wavelength.music.data.model.PlaylistSummary
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.playback.PlayerController
import com.wavelength.music.ui.components.ScreenState
import com.wavelength.music.ui.search.PendingSearchQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// JioSaavn search already returns language-relevant results for these terms, so "genre" here
// really means "language" — the thing that actually matters for this catalog.
val genreShortcuts = listOf(
    "tamil", "hindi", "telugu", "english", "punjabi", "malayalam", "kannada", "bengali"
)

/** Tapping one reuses the Genre screen's plain-text-search mechanism (the artist's name works
 * fine as a JioSaavn search query, same as [genreShortcuts]'s language tags do) — no dedicated
 * artist API or screen needed. There's no artist-photo endpoint on this JioSaavn deployment, so
 * these are shown as plain text chips (like recent searches) rather than photo avatars. */
val featuredArtists = listOf(
    "Anirudh Ravichander", "Sai Abhyankkar", "GV Prakash Kumar", "Vijay Antony",
    "Hiphop Tamizha", "Yuvan Shankar Raja", "Santhosh Narayanan", "D. Imman",
    "A.R. Rahman", "Thaman S"
)

data class RecentListeningMix(
    val seedArtist: String,
    val tracks: List<Track>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController,
    private val pendingSearchQuery: PendingSearchQuery
) : ViewModel() {

    private val _featured = MutableStateFlow<ScreenState<List<Track>>>(ScreenState.Loading)
    val featured: StateFlow<ScreenState<List<Track>>> = _featured.asStateFlow()

    private val _suggested = MutableStateFlow<ScreenState<List<Track>>>(ScreenState.Loading)
    val suggested: StateFlow<ScreenState<List<Track>>> = _suggested.asStateFlow()

    private val _recentListeningMixes = MutableStateFlow<List<RecentListeningMix>>(emptyList())
    val recentListeningMixes: StateFlow<List<RecentListeningMix>> = _recentListeningMixes.asStateFlow()

    val recentlyPlayed: StateFlow<List<Track>> = repository.observeRecentlyPlayed(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistSummary>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<String>> = repository.observeSearchHistory(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** "Most Played" smart playlist — a live, always-available Room aggregation, so unlike
     * [suggested]/[dailyMix] it needs no loading/error state of its own. */
    val mostPlayed: StateFlow<List<Track>> = repository.observeTopTracks(15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** "Recently Added" smart playlist, seeded from favorites (already ordered newest-first). */
    val recentlyAdded: StateFlow<List<Track>> = repository.observeFavorites()
        .map { it.take(15) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _topArtistTracks = MutableStateFlow<Pair<String, List<Track>>?>(null)
    val topArtistTracks: StateFlow<Pair<String, List<Track>>?> = _topArtistTracks.asStateFlow()

    private val _dailyMix = MutableStateFlow<ScreenState<List<Track>>>(ScreenState.Loading)
    val dailyMix: StateFlow<ScreenState<List<Track>>> = _dailyMix.asStateFlow()

    private val _topCharts = MutableStateFlow<Map<String, List<Track>>>(emptyMap())
    val topCharts: StateFlow<Map<String, List<Track>>> = _topCharts.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Snapshots of whatever [loadSuggested]/[loadDailyMix] were last driven by, so pull-to-refresh
    // can re-run those two searches on demand without needing its own separate query logic.
    private var latestRecentTracks: List<Track> = emptyList()
    private var latestTopArtists: List<ArtistStat> = emptyList()

    init {
        loadFeatured()
        loadTopCharts()
        viewModelScope.launch {
            repository.observeRecentlyPlayed(30).collectLatest { tracks ->
                latestRecentTracks = tracks
                loadSuggested(tracks)
            }
        }
        viewModelScope.launch {
            repository.observeTopArtists(2).collectLatest { artists ->
                latestTopArtists = artists
                loadDailyMix(artists)
                loadTopArtistTracks(artists.firstOrNull())
            }
        }
    }

    fun loadTopCharts() {
        viewModelScope.launch {
            val chartQueries = linkedMapOf(
                "Global Top 10" to "global top songs",
                "India Top 10" to "india top songs",
                "Tamil Top 10" to "top tamil songs",
                "Hindi Top 10" to "top hindi songs",
                "English Top 10" to "top english songs"
            )
            val loaded = coroutineScope {
                chartQueries.map { (label, query) ->
                    async {
                        val tracks = repository.searchTracks(query, limit = 15)
                            .getOrDefault(emptyList())
                            .distinctBy { track ->
                                Triple(
                                    track.name.lowercase().trim(),
                                    track.artistName.substringBefore(',').lowercase().trim(),
                                    track.language.lowercase()
                                )
                            }
                            .take(10)
                        label to tracks
                    }
                }.awaitAll().toMap()
            }
            _topCharts.value = loaded
        }
    }

    fun loadFeatured() {
        viewModelScope.launch {
            _featured.value = ScreenState.Loading
            fetchFeatured()
        }
    }

    private suspend fun fetchFeatured(forceRefresh: Boolean = false) {
        repository.getFeaturedTracks(20, forceRefresh = forceRefresh).fold(
            onSuccess = { tracks ->
                val deduped = tracks.distinctBy(::songIdentity)
                _featured.value = if (deduped.isEmpty()) ScreenState.Empty else ScreenState.Success(deduped)
            },
            onFailure = { e ->
                _featured.value = ScreenState.Error(e.message ?: "Something went wrong")
            }
        )
    }

    /** Pull-to-refresh: re-fetches Featured plus the two search-backed sections. Unlike
     * [loadFeatured]/the initial load, this never sets any section to [ScreenState.Loading] —
     * doing so would swap the whole `Success` branch in HomeScreen out for a full-screen
     * [com.wavelength.music.ui.components.LoadingView] mid-pull, which both looks broken and
     * hides the very sections meant to visibly refresh. Existing content stays on screen (the
     * pull-to-refresh spinner is the only loading indicator) until fresh data replaces it. Most
     * of the rest of Home (recently played, playlists, most played, recently added) is already a
     * live Room flow that updates on its own and needs no manual refresh. */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchFeatured(forceRefresh = true)
            loadSuggested(latestRecentTracks, showLoading = false, forceRefresh = true)
            loadDailyMix(latestTopArtists, showLoading = false, forceRefresh = true)
            loadTopCharts()
            _isRefreshing.value = false
        }
    }

    /** Seeds "suggested for you" from whichever artist appears most often in recent plays, then
     * searches JioSaavn for more from that artist, excluding tracks already recently played. */
    private suspend fun loadSuggested(
        recentTracks: List<Track>,
        showLoading: Boolean = true,
        forceRefresh: Boolean = false
    ) {
        val topArtists = recentTracks
            .groupingBy { it.artistName }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .filter { it.isNotBlank() }
            .take(3)
        if (topArtists.isEmpty()) {
            _suggested.value = ScreenState.Empty
            _recentListeningMixes.value = emptyList()
            return
        }
        if (showLoading) _suggested.value = ScreenState.Loading
        coroutineScope {
            val results = topArtists.map { artist ->
                async {
                    artist to repository.searchTracks(
                        artist,
                        limit = 18,
                        forceRefresh = forceRefresh
                    ).getOrDefault(emptyList())
                }
            }.awaitAll()
            val excludeIds = recentTracks.map { it.id }.toSet()

            val mixes = results.mapNotNull { (artist, tracks) ->
                val mixTracks = tracks
                    .distinctBy(::songIdentity)
                    .filterNot { it.id in excludeIds }
                    .filter { candidate ->
                        candidate.artistName.split(',').any { name ->
                            name.trim().contains(artist, ignoreCase = true) ||
                                artist.contains(name.trim(), ignoreCase = true)
                        }
                    }
                    .take(15)
                if (mixTracks.size >= 3) RecentListeningMix(artist, mixTracks) else null
            }
            _recentListeningMixes.value = mixes.take(3)

            val filtered = results
                .flatMap { it.second }
                .distinctBy(::songIdentity)
                .filterNot { it.id in excludeIds }
                .shuffled()
                .take(24)
            _suggested.value = if (filtered.isEmpty()) ScreenState.Empty else ScreenState.Success(filtered)
        }
    }

    private suspend fun loadTopArtistTracks(topArtist: ArtistStat?) {
        if (topArtist == null || topArtist.artistName.isBlank()) {
            _topArtistTracks.value = null
            return
        }
        val tracks = repository.searchTracks(topArtist.artistName, limit = 20)
            .getOrDefault(emptyList())
            .filter { track ->
                track.artistName.split(',').any { name ->
                    name.trim().contains(topArtist.artistName, ignoreCase = true) ||
                        topArtist.artistName.contains(name.trim(), ignoreCase = true)
                }
            }
            .distinctBy(::songIdentity)
            .take(10)
        _topArtistTracks.value = if (tracks.isEmpty()) null else topArtist.artistName to tracks
    }

    /** Mixes tracks from whichever 2 artists have the most plays across your whole listening
     * history (unlike [suggested], which only looks at your most-recent plays), giving a broader
     * "you'll probably like this too" mix. */
    private suspend fun loadDailyMix(
        topArtists: List<ArtistStat>,
        showLoading: Boolean = true,
        forceRefresh: Boolean = false
    ) {
        if (topArtists.isEmpty()) {
            _dailyMix.value = ScreenState.Empty
            return
        }
        if (showLoading) _dailyMix.value = ScreenState.Loading
        coroutineScope {
            val results = topArtists
                .map { artist -> async { repository.searchTracks(artist.artistName, limit = 15, forceRefresh = forceRefresh) } }
                .awaitAll()
            val combined = results.flatMap { it.getOrDefault(emptyList()) }.distinctBy(::songIdentity).shuffled()
            _dailyMix.value = if (combined.isEmpty()) ScreenState.Empty else ScreenState.Success(combined)
        }
    }

    private fun songIdentity(track: Track): String {
        val title = track.name.lowercase()
            .replace(Regex("""\([^)]*(from|movie|film|soundtrack|version|single|original)[^)]*\)""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\[[^]]*(from|movie|film|soundtrack|version|single|original)[^]]*]""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s*[-–—:]\s*(from|soundtrack|ost|single|song|version).*""", RegexOption.IGNORE_CASE), " ")
            .replace("&", " and ")
            .replace(Regex("""[^a-z0-9\p{L}]+"""), " ")
            .trim()
        val artist = track.artistName.substringBefore(',').lowercase()
            .replace(Regex("""[^a-z0-9\p{L}]+"""), " ").trim()
        return "$title|$artist"
    }

    fun playTrack(queue: List<Track>, index: Int) {
        playerController.playQueue(queue, index)
    }

    fun addMixToQueue(tracks: List<Track>) {
        tracks.forEach(playerController::addToQueue)
    }

    fun prepareSearch(query: String) {
        pendingSearchQuery.set(query)
    }
}
