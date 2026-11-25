package com.wavelength.music.playback

import com.squareup.moshi.JsonClass
import com.wavelength.music.data.model.Track

enum class RepeatMode { OFF, ONE, ALL }

/** Stable identity for one playback occurrence, independent of the track's catalog ID. */
@JsonClass(generateAdapter = true)
data class QueueEntry(val instanceId: Long, val track: Track)

data class PlaybackUiState(
    val currentTrack: Track? = null,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
    val currentEntryId: Long? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val volume: Float = 1f,
    val playbackSpeed: Float = 1f
)
