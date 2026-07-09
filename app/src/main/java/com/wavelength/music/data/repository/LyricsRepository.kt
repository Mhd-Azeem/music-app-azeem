package com.wavelength.music.data.repository

import com.wavelength.music.data.model.LyricLine
import com.wavelength.music.data.model.parseLrc
import com.wavelength.music.data.remote.lrclib.LrcLibApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    private val api: LrcLibApiService
) {
    suspend fun getSyncedLyrics(
        trackName: String,
        artistName: String,
        durationSeconds: Int
    ): Result<List<LyricLine>> {
        // lrclib returns HTTP 404 (not a 200 with null fields) for the common "no match" case,
        // which is most tracks — surfacing that as a raw HttpException message would show the
        // user something like "HTTP 404 " instead of an explanation, so it's normalized here.
        val response = runCatching {
            api.getLyrics(trackName, artistName, durationSeconds.coerceAtLeast(1))
        }.getOrNull()
        val synced = response?.syncedLyrics
        if (synced.isNullOrBlank()) return Result.failure(Exception("No synced lyrics found for this track"))
        return runCatching { parseLrc(synced) }
    }
}
