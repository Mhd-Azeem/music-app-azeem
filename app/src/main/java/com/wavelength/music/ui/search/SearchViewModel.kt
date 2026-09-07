package com.wavelength.music.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.playback.PlayerController
import com.wavelength.music.ui.components.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController,
    private val pendingSearchQuery: PendingSearchQuery
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<ScreenState<List<Track>>>(ScreenState.Empty)
    val results: StateFlow<ScreenState<List<Track>>> = _results.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("All")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    val searchHistory: StateFlow<List<String>> = repository.observeSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    // Paging state for the currently-displayed query only — reset on every new search, not tied
    // to _query directly since onQueryChange fires on every keystroke but a page only ever
    // belongs to the query that was actually committed to _results.
    private var pagedQuery: String = ""
    private var currentPage = 0
    private var canLoadMore = true

    /** Called from [SearchScreen] every time it's actually navigated to (via `LaunchedEffect`),
     * not just once from `init` — bottom-nav tabs reuse the same ViewModel instance across
     * revisits (`restoreState`/`launchSingleTop`), so a query set by a later Home chip tap would
     * otherwise never be picked up once this ViewModel already exists from an earlier visit. */
    fun consumePendingSearch() {
        pendingSearchQuery.consume()?.let {
            onQueryChange(it)
            commitSearch()
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _results.value = ScreenState.Empty
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _results.value = ScreenState.Loading
            runSearch(newQuery)
        }
    }

    private suspend fun runSearch(q: String) {
        pagedQuery = q
        currentPage = 0
        canLoadMore = true
        repository.searchTracks(q, page = 0, limit = SEARCH_PAGE_SIZE).fold(
            onSuccess = { tracks ->
                // distinctBy guards against the unofficial JioSaavn API occasionally returning
                // overlapping/duplicate ids within one result set — the list below is keyed by
                // track.id in Compose, which would crash on a duplicate.
                val deduped = rankTracks(dedupeTracks(tracks), q)
                canLoadMore = tracks.size >= SEARCH_PAGE_SIZE
                _results.value = if (deduped.isEmpty()) ScreenState.Empty else ScreenState.Success(deduped)
            },
            onFailure = { e ->
                _results.value = ScreenState.Error(e.message ?: "Something went wrong")
            }
        )
    }

    /** Called as the user scrolls near the bottom of the results list. A no-op while already
     * loading, once the last page came back short of a full page (nothing more to fetch), or
     * before any search has actually completed successfully. */
    fun loadMore() {
        val current = _results.value
        if (current !is ScreenState.Success || _isLoadingMore.value || !canLoadMore) return
        val q = pagedQuery
        val nextPage = currentPage + 1
        viewModelScope.launch {
            _isLoadingMore.value = true
            repository.searchTracks(q, page = nextPage, limit = SEARCH_PAGE_SIZE).fold(
                onSuccess = { newTracks ->
                    // A newer search may have started (and possibly already finished) while this
                    // page request was in flight - drop this response rather than clobbering it
                    // with stale results merged on top.
                    if (pagedQuery == q) {
                        currentPage = nextPage
                        canLoadMore = newTracks.size >= SEARCH_PAGE_SIZE
                        val latest = (_results.value as? ScreenState.Success)?.data ?: current.data
                        _results.value = ScreenState.Success(rankTracks(dedupeTracks(latest + newTracks), q))
                    }
                },
                onFailure = {
                    // Leave existing results on screen; just stop trying to page further until
                    // the user retries by scrolling again next time results reload from scratch.
                    if (pagedQuery == q) canLoadMore = false
                }
            )
            _isLoadingMore.value = false
        }
    }

    fun selectLanguage(language: String) {
        _selectedLanguage.value = language
    }

    fun availableLanguages(tracks: List<Track>): List<String> {
        val detected = tracks.mapNotNull { track ->
            track.language.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
        }.distinct().sorted()
        return listOf("All") + detected
    }

    fun filteredTracks(tracks: List<Track>): List<Track> {
        val selected = _selectedLanguage.value
        if (selected == "All") return tracks
        return tracks.filter { it.language.equals(selected, ignoreCase = true) }
    }

    private fun dedupeTracks(tracks: List<Track>): List<Track> {
        val seen = linkedSetOf<String>()
        return tracks.filter { track ->
            val canonicalTitle = canonicalSongTitle(track.name)
            val canonicalLanguage = track.language.lowercase().trim()

            // JioSaavn often returns the same recording several times with different IDs,
            // album metadata, featured-artist ordering, or suffixes such as "(From ...)",
            // "- Single", "Original Motion Picture Soundtrack", etc. For search results,
            // title + language is intentionally the primary identity so those copies collapse.
            val key = "$canonicalLanguage|$canonicalTitle"
            key.isNotBlank() && seen.add(key)
        }
    }

    private fun rankTracks(tracks: List<Track>, query: String): List<Track> {
        val q = normalizeForMatch(query)
        if (q.isBlank()) return tracks
        val words = q.split(' ').filter { it.isNotBlank() }

        fun score(track: Track): Int {
            val title = normalizeForMatch(track.name)
            val artist = normalizeForMatch(track.artistName)
            val album = normalizeForMatch(track.albumName)
            var score = 0
            if (title == q) score += 1000
            if (artist == q) score += 700
            if (title.startsWith(q)) score += 450
            if (artist.startsWith(q)) score += 350
            if (title.contains(q)) score += 250
            if (artist.contains(q)) score += 200
            if (album.contains(q)) score += 100
            score += words.count { it in title } * 60
            score += words.count { it in artist } * 40
            if (track.source != com.wavelength.music.data.model.TrackSource.JIOSAAVN) score += 80
            return score
        }

        return tracks.withIndex()
            .sortedWith(compareByDescending<IndexedValue<Track>> { score(it.value) }.thenBy { it.index })
            .map { it.value }
    }

    private fun normalizeForMatch(value: String): String = value
        .lowercase()
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9\\p{L}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun canonicalSongTitle(raw: String): String {
        return raw
            .lowercase()
            // Remove bracketed metadata/version labels.
            .replace(Regex("\\([^)]*(from|movie|film|soundtrack|version|remix|mix|edit|single|theme|ost|original|lofi|lo-fi|slowed|reverb|karaoke|instrumental)[^)]*\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\[[^]]*(from|movie|film|soundtrack|version|remix|mix|edit|single|theme|ost|original|lofi|lo-fi|slowed|reverb|karaoke|instrumental)[^]]*]", RegexOption.IGNORE_CASE), " ")
            // Remove common dash suffixes added by catalog metadata.
            .replace(Regex("\\s*[-–—:]\\s*(from|original motion picture soundtrack|motion picture soundtrack|soundtrack|ost|single|song|theme|version|remix|mix|edit|lofi|lo-fi|slowed|reverb|karaoke|instrumental).*", RegexOption.IGNORE_CASE), " ")
            // Remove explicit 'from <movie>' tail even without punctuation.
            .replace(Regex("\\s+from\\s+.+$", RegexOption.IGNORE_CASE), " ")
            // Normalize punctuation/spacing so tiny naming differences collapse.
            .replace("&", " and ")
            .replace(Regex("[^a-z0-9\\p{L}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun retry() {
        val q = _query.value
        if (q.isNotBlank()) {
            viewModelScope.launch {
                _results.value = ScreenState.Loading
                runSearch(q)
            }
        }
    }

    fun playTrack(queue: List<Track>, index: Int) {
        playerController.playQueue(queue, index)
    }

    fun commitSearch() {
        val q = _query.value.trim()
        if (q.isNotEmpty()) {
            viewModelScope.launch { repository.recordSearch(q) }
        }
    }

    fun onHistoryItemClick(historyQuery: String) {
        onQueryChange(historyQuery)
        commitSearch()
    }

    fun removeHistoryEntry(historyQuery: String) {
        viewModelScope.launch { repository.removeSearchHistoryEntry(historyQuery) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearSearchHistory() }
    }

    private companion object {
        const val SEARCH_PAGE_SIZE = 20
    }
}
