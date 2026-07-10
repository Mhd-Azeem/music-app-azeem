package com.wavelength.music.playback

import com.wavelength.music.data.model.Track

enum class RepeatMode { OFF, ONE, ALL }

/** Wraps a [Track] with an identity that's stable for as long as this particular queue slot
 * exists, independent of the track's own catalog id — the same song can appear more than once in
 * the queue (e.g. "Add to queue" twice), so `track.id` alone isn't a safe UI/drag-reorder key. */
data class QueueEntry(val instanceId: Long, val track: Track)

data class PlaybackUiState(
    val currentTrack: Track? = null,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val volume: Float = 1f,
    val playbackSpeed: Float = 1f
)
