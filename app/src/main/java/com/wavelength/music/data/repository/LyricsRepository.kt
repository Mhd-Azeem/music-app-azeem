package com.wavelength.music.data.repository

import com.wavelength.music.data.cache.TtlCache
import com.wavelength.music.data.model.LyricLine
import com.wavelength.music.data.model.parseLrc
import com.wavelength.music.data.remote.lrclib.LrcLibApiService
import com.wavelength.music.data.remote.lrclib.LrcLibResponseDto
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Singleton
data class LyricsSongCandidate(
    val trackName: String,
    val artistName: String,
    val albumName: String
)

class LyricsRepository @Inject constructor(
    private val api: LrcLibApiService
) {
    private val lyricsCache = TtlCache<String, List<LyricLine>>()

    suspend fun getLyrics(
        trackName: String,
        artistName: String,
        albumName: String,
        durationSeconds: Int
    ): Result<List<LyricLine>> = runCatching {
        val cleanTitle = cleanTitle(trackName)
        val primaryArtist = artistName.substringBefore(',').trim()
        val cacheKey = "${normalize(cleanTitle)}|${normalize(primaryArtist)}|${durationSeconds.coerceAtLeast(0)}"

        lyricsCache.getOrPut(
            key = cacheKey,
            freshForMs = TimeUnit.HOURS.toMillis(24),
            staleForMs = TimeUnit.DAYS.toMillis(7)
        ) {
            fetchLyrics(cleanTitle, primaryArtist, albumName, durationSeconds)
        }
    }

    suspend fun searchSongCandidatesByLyrics(query: String, limit: Int = 5): List<LyricsSongCandidate> {
        val rawQuery = query.trim()
        if (rawQuery.length < 6) return emptyList()
        val normalizedQuery = normalizeLyricsText(rawQuery)
        if (normalizedQuery.isBlank()) return emptyList()

        val matches = runCatching { api.searchLyricsByQuery(rawQuery) }.getOrDefault(emptyList())

        return matches
            .mapNotNull { item ->
                val title = item.trackName?.trim().orEmpty()
                val artist = item.artistName?.trim().orEmpty()
                if (title.isBlank() || artist.isBlank()) return@mapNotNull null

                val searchableLyrics = normalizeLyricsText(
                    listOfNotNull(item.plainLyrics, item.syncedLyrics).joinToString(" ")
                )
                if (!searchableLyrics.contains(normalizedQuery)) return@mapNotNull null

                LyricsSongCandidate(
                    trackName = title,
                    artistName = artist,
                    albumName = item.albumName?.trim().orEmpty()
                )
            }
            .distinctBy { normalize(it.trackName) + "|" + normalize(it.artistName) }
            .take(limit)
    }

    private suspend fun fetchLyrics(
        cleanTitle: String,
        primaryArtist: String,
        albumName: String,
        durationSeconds: Int
    ): List<LyricLine> {
        val candidates = mutableListOf<LrcLibResponseDto>()

        // 1) Best exact match: include duration only when it is actually useful.
        if (durationSeconds >= 30) {
            runCatching {
                api.getLyrics(
                    trackName = cleanTitle,
                    artistName = primaryArtist,
                    albumName = albumName.takeIf { it.isNotBlank() },
                    durationSeconds = durationSeconds
                )
            }.getOrNull()?.let(candidates::add)
        }

        // 2) Retry exact matching without duration. Unknown/incorrect JioSaavn duration should
        // never turn into a fake 1-second signature, which makes LRCLIB reject a valid song.
        runCatching {
            api.getLyrics(
                trackName = cleanTitle,
                artistName = primaryArtist,
                albumName = albumName.takeIf { it.isNotBlank() },
                durationSeconds = null
            )
        }.getOrNull()?.let(candidates::add)

        // 3) Broader search fallback. This helps movie/album metadata and artist formatting that
        // differs between JioSaavn and LRCLIB.
        val searchMatches = runCatching {
            api.searchLyrics(cleanTitle, primaryArtist.takeIf { it.isNotBlank() })
        }.getOrDefault(emptyList())

        candidates += searchMatches.sortedByDescending { scoreMatch(it, cleanTitle, primaryArtist, durationSeconds) }

        val best = candidates.firstOrNull {
            !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank()
        } ?: throw Exception("No lyrics found for this track")

        val synced = best.syncedLyrics
        if (!synced.isNullOrBlank()) {
            val parsed = parseLrc(synced)
            if (parsed.isNotEmpty()) return parsed
        }

        val plain = best.plainLyrics
        if (!plain.isNullOrBlank()) {
            // Static fallback: preserve every line so users still get lyrics when LRCLIB has no
            // timestamps. Long.MAX_VALUE keeps the existing synced highlighter from auto-advancing.
            return plain.lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { LyricLine(Long.MAX_VALUE, it) }
                .toList()
        }

        throw Exception("No lyrics found for this track")
    }

    private fun scoreMatch(
        item: LrcLibResponseDto,
        title: String,
        artist: String,
        durationSeconds: Int
    ): Int {
        var score = 0
        if (normalize(item.trackName.orEmpty()) == normalize(title)) score += 100
        if (normalize(item.artistName.orEmpty()).contains(normalize(artist))) score += 40
        if (durationSeconds >= 30 && item.duration != null &&
            kotlin.math.abs(item.duration - durationSeconds) <= 3.0
        ) score += 30
        if (!item.syncedLyrics.isNullOrBlank()) score += 20
        if (!item.plainLyrics.isNullOrBlank()) score += 10
        return score
    }

    private fun cleanTitle(raw: String): String = raw
        .replace(Regex("\\([^)]*(from|soundtrack|version|remix|mix|edit|single|ost)[^)]*\\)", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("\\[[^]]*(from|soundtrack|version|remix|mix|edit|single|ost)[^]]*]", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("\\s*[-–—:]\\s*(from|soundtrack|ost|single|version|remix|mix|edit).*", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun normalizeLyricsText(value: String): String = value
        .lowercase()
        .replace(Regex("\\[[^]]*]"), " ")
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9\\p{L}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun normalize(value: String): String = value
        .lowercase()
        .replace("&", "and")
        .replace(Regex("[^a-z0-9\\p{L}]+"), "")
}
