package com.wavelength.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
    private val settingsRepository: SettingsRepository
) {
    private var controller: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var currentQueue: List<QueueEntry> = emptyList()
    private var nextInstanceId = 0L
    private var aiDjExtendJob: Job? = null

    // The user's actual desired volume, distinct from whatever controller.volume momentarily is
    // mid-crossfade — fades ramp toward/away from this rather than a fixed 1f, and setVolume()
    // is the only thing allowed to change it.
    private var targetVolume: Float = 1f
    private var crossfadeJob: Job? = null
    private var fadeOutTriggeredForIndex: Int = -1
    // Suppresses onVolumeChanged's state update while a fade is actively stepping volume, so the
    // visible volume slider doesn't visibly animate down-and-up on every automatic track change.
    private var isFading: Boolean = false

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val controllerScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var tickerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            updateTicker(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = controller?.currentMediaItemIndex ?: -1
            val track = currentQueue.getOrNull(index)?.track
            _state.update {
                it.copy(
                    currentTrack = track,
                    currentIndex = index,
                    queue = currentQueue,
                    durationMs = controller?.duration?.coerceAtLeast(0) ?: 0L
                )
            }
            if (track != null) {
                controllerScope.launch { repository.recordPlayed(track) }
                if (settingsRepository.state.value.aiDjEnabled && index >= currentQueue.size - 2) {
                    extendQueueWithAiDj(track)
                }
            }
            fadeOutTriggeredForIndex = -1
            val crossfadeMs = settingsRepository.state.value.crossfadeDurationMs
            // Only fade in after a natural end-of-track progression (which is what triggered the
            // matching fade-out in maybeStartCrossfadeOut) — a manual skip/previous/queue-item
            // tap/seek also fires this callback, and fading those from silence would mean every
            // manual skip drops to silence and climbs back up instead of switching instantly.
            // REPEAT covers looping back to the start under repeat-one/repeat-all, which is just
            // as "natural" a progression as AUTO and should fade the same way.
            val isNaturalProgression = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            if (crossfadeMs > 0 && isNaturalProgression) {
                startFade(from = 0f, to = targetVolume, durationMs = crossfadeMs)
            } else {
                crossfadeJob?.cancel()
                controller?.volume = targetVolume
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _state.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _state.update { it.copy(repeatMode = repeatMode.toRepeatMode()) }
        }

        override fun onVolumeChanged(volume: Float) {
            if (!isFading) {
                _state.update { it.copy(volume = volume) }
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _state.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }
    }

    fun connect() {
        if (controller != null || controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                controller = future.get().also { it.addListener(playerListener) }
                syncStateFromController()
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun syncStateFromController() {
        val c = controller ?: return
        val index = c.currentMediaItemIndex
        targetVolume = c.volume
        _state.update {
            it.copy(
                isPlaying = c.isPlaying,
                currentIndex = index,
                currentTrack = currentQueue.getOrNull(index)?.track,
                queue = currentQueue,
                positionMs = c.currentPosition.coerceAtLeast(0),
                durationMs = c.duration.coerceAtLeast(0),
                shuffleEnabled = c.shuffleModeEnabled,
                repeatMode = c.repeatMode.toRepeatMode(),
                volume = c.volume,
                playbackSpeed = c.playbackParameters.speed
            )
        }
        updateTicker(c.isPlaying)
    }

    private fun updateTicker(isPlaying: Boolean) {
        tickerJob?.cancel()
        if (!isPlaying) {
            // A fade is a wall-clock countdown, not tied to playback position, so it would keep
            // running (and finish fading to silence) even while paused if left alone. Cancel it
            // and restore the real volume so resuming always starts audible.
            if (crossfadeJob?.isActive == true) {
                crossfadeJob?.cancel()
                controller?.volume = targetVolume
            }
            return
        }
        tickerJob = controllerScope.launch {
            while (isActive) {
                val c = controller
                if (c != null) {
                    _state.update {
                        it.copy(
                            positionMs = c.currentPosition.coerceAtLeast(0),
                            durationMs = c.duration.coerceAtLeast(0)
                        )
                    }
                    maybeStartCrossfadeOut(c)
                }
                delay(500)
            }
        }
    }

    /** Starts fading the current track's volume down once it's within the crossfade window of
     * ending, so it overlaps with the fade-in [onMediaItemTransition] starts for the next track.
     * [fadeOutTriggeredForIndex] guards against re-triggering every tick while still in that
     * window. */
    private fun maybeStartCrossfadeOut(c: MediaController) {
        val crossfadeMs = settingsRepository.state.value.crossfadeDurationMs
        if (crossfadeMs <= 0) return
        // A track whose duration isn't known yet (still resolving from the stream) reports
        // C.TIME_UNSET, a large negative sentinel — treat that the same as "duration unknown" and
        // wait for a later tick instead of letting it fall through the "too short to crossfade"
        // check below, which a naive `duration <= crossfadeMs` comparison would wrongly do.
        val duration = c.duration.takeIf { it != C.TIME_UNSET } ?: return
        // A track shorter than the crossfade window would start fading out again almost as soon
        // as it starts (its "remaining" time is already inside the window from the first tick),
        // cancelling whatever fade-in/volume-snap just happened — so those simply don't crossfade.
        if (!c.hasNextMediaItem() || duration <= crossfadeMs) return
        val currentIndex = c.currentMediaItemIndex
        if (fadeOutTriggeredForIndex == currentIndex) return
        val remaining = duration - c.currentPosition
        if (remaining in 0..crossfadeMs.toLong()) {
            fadeOutTriggeredForIndex = currentIndex
            startFade(from = targetVolume, to = 0f, durationMs = remaining.toInt().coerceIn(1, crossfadeMs))
        }
    }

    /** Ramps controller.volume from [from] to [to] over [durationMs] in ~50ms steps. Cancels any
     * fade already in progress, so a fade-in from a new track always wins over a stale fade-out. */
    private fun startFade(from: Float, to: Float, durationMs: Int) {
        crossfadeJob?.cancel()
        isFading = true
        lateinit var job: Job
        job = controllerScope.launch {
            try {
                val stepMs = 50
                val steps = (durationMs / stepMs).coerceAtLeast(1)
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    controller?.volume = (from + (to - from) * t).coerceIn(0f, 1f)
                    delay(stepMs.toLong())
                }
            } finally {
                // Cancelling the old job to start a new fade doesn't stop it instantly — it only
                // unwinds at its next suspension point, which can land after the new fade has
                // already set isFading = true. Only the still-current job may clear it, so a
                // late-arriving cancelled-job cleanup can't clobber a newer fade's flag.
                if (crossfadeJob === job) isFading = false
            }
        }
        crossfadeJob = job
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        crossfadeJob?.cancel()
        fadeOutTriggeredForIndex = -1
        c.volume = targetVolume
        currentQueue = tracks.map { QueueEntry(nextInstanceId++, it) }
        val items = tracks.map { it.toMediaItem() }
        c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        c.prepare()
        c.play()
        _state.update { it.copy(queue = currentQueue) }
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState == Player.STATE_IDLE) {
            c.prepare()
        }
        if (c.isPlaying) c.pause() else c.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        targetVolume = clamped
        crossfadeJob?.cancel()
        controller?.volume = clamped
        _state.update { it.copy(volume = clamped) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2f)
        controller?.setPlaybackSpeed(clamped)
        _state.update { it.copy(playbackSpeed = clamped) }
    }

    fun skipNext() {
        controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    }

    fun skipPrevious() {
        val c = controller ?: return
        if (c.currentPosition > 3000 || !c.hasPreviousMediaItem()) {
            c.seekTo(0)
        } else {
            c.seekToPreviousMediaItem()
        }
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun addToQueue(track: Track) {
        controller?.addMediaItem(track.toMediaItem())
        currentQueue = currentQueue + QueueEntry(nextInstanceId++, track)
        _state.update { it.copy(queue = currentQueue) }
    }

    /** When AI DJ is on and only a track or two is left in the queue, tops it up with more from
     * the artist that's just finishing, so playback never runs dry. Guarded by [aiDjExtendJob] so
     * back-to-back track transitions near the end of the queue can't fire overlapping searches. */
    private fun extendQueueWithAiDj(justPlayed: Track) {
        if (aiDjExtendJob?.isActive == true) return
        aiDjExtendJob = controllerScope.launch {
            val results = repository.searchTracks(justPlayed.artistName, limit = 10).getOrDefault(emptyList())
            val existingIds = currentQueue.map { it.track.id }.toSet()
            results.filterNot { it.id in existingIds }.take(5).forEach { addToQueue(it) }
        }
    }

    fun playQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in currentQueue.indices) return
        c.seekToDefaultPosition(index)
        c.play()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val c = controller ?: return
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return
        c.moveMediaItem(fromIndex, toIndex)
        currentQueue = currentQueue.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        _state.update { it.copy(queue = currentQueue, currentIndex = c.currentMediaItemIndex) }
    }

    /** Reorders the *upcoming* portion of the queue (currently-playing track and anything already
     * played are left alone) via a weighted-random shuffle that favors tracks with more listening
     * history — unlike [toggleShuffle]'s uniform-random ExoPlayer shuffle. Tracks with no play
     * history still get a small chance so the queue doesn't become entirely "greatest hits". */
    fun smartShuffleQueue() {
        val c = controller ?: return
        val remainingStart = c.currentMediaItemIndex + 1
        if (remainingStart >= currentQueue.size) return
        controllerScope.launch {
            val remaining = currentQueue.subList(remainingStart, currentQueue.size)
            val playCounts = repository.getPlayCounts(remaining.map { it.track.id })
            val shuffled = weightedShuffle(remaining) { entry -> playCounts[entry.track.id] ?: 0 }
            shuffled.forEachIndexed { offset, entry ->
                val targetIndex = remainingStart + offset
                val fromIndex = currentQueue.indexOfFirst { it.instanceId == entry.instanceId }
                if (fromIndex != targetIndex) moveQueueItem(fromIndex, targetIndex)
            }
        }
    }

    fun playNext(track: Track) {
        val c = controller ?: return
        val insertIndex = (c.currentMediaItemIndex + 1).coerceIn(0, c.mediaItemCount)
        c.addMediaItem(insertIndex, track.toMediaItem())
        currentQueue = currentQueue.toMutableList().apply {
            add(insertIndex.coerceAtMost(size), QueueEntry(nextInstanceId++, track))
        }
        _state.update { it.copy(queue = currentQueue) }
    }

    fun release() {
        tickerJob?.cancel()
        crossfadeJob?.cancel()
        aiDjExtendJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }
}

private fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}

/** Weighted random sampling without replacement: each remaining item's chance of being picked
 * next is proportional to `weightOf(item) + 1` (the `+1` keeps zero-play tracks reachable rather
 * than excluded outright, just less likely than frequently-played ones). */
private fun <T> weightedShuffle(items: List<T>, weightOf: (T) -> Int): List<T> {
    val pool = items.toMutableList()
    val weights = pool.map { (weightOf(it) + 1).toDouble() }.toMutableList()
    val result = ArrayList<T>(items.size)
    while (pool.isNotEmpty()) {
        val totalWeight = weights.sum()
        var roll = Math.random() * totalWeight
        var pickIndex = weights.lastIndex
        for (i in weights.indices) {
            roll -= weights[i]
            if (roll <= 0) {
                pickIndex = i
                break
            }
        }
        result.add(pool.removeAt(pickIndex))
        weights.removeAt(pickIndex)
    }
    return result
}
