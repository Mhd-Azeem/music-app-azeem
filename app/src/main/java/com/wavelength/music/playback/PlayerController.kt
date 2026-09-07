package com.wavelength.music.playback

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.wavelength.music.activation.ActivationRepository
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.TrackSource
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.data.repository.ListeningTimeRepository
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
    private val settingsRepository: SettingsRepository,
    private val activationRepository: ActivationRepository,
    private val listeningTimeRepository: ListeningTimeRepository
) {
    private var controller: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var currentQueue: List<QueueEntry> = emptyList()
    private var nextInstanceId = 0L
    private var aiDjExtendJob: Job? = null

    private var targetVolume: Float = 1f
    private var crossfadeJob: Job? = null
    private var fadeOutTriggeredForIndex: Int = -1
    private var isFading: Boolean = false

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    // Incremented every time an online-track playback attempt is denied. The Compose layer observes
    // this value so the activation dialog is shown on every blocked attempt, not only the first
    // time a locked track becomes the current UI item.
    private val _activationRequiredSequence = MutableStateFlow(0L)
    val activationRequiredSequence: StateFlow<Long> = _activationRequiredSequence.asStateFlow()

    private val controllerScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var tickerJob: Job? = null
    private var listeningAccumulatorMs = 0L
    private var lastListeningTickMs = 0L

    private fun requiresActivation(track: Track?): Boolean =
        track?.source == TrackSource.JIOSAAVN && !activationRepository.isAccessActive()

    private fun blockOnlinePlayback(track: Track? = null) {
        crossfadeJob?.cancel()
        controller?.pause()
        _state.update { current ->
            if (track != null) current.copy(currentTrack = track, isPlaying = false)
            else current.copy(isPlaying = false)
        }
        _activationRequiredSequence.value = _activationRequiredSequence.value + 1L
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val index = controller?.currentMediaItemIndex ?: -1
            val track = currentQueue.getOrNull(index)?.track ?: _state.value.currentTrack
            if (isPlaying && requiresActivation(track)) {
                // This also catches play commands coming from the media notification/widget rather
                // than the Compose UI, so dismissing the activation dialog can never resume a
                // protected online stream.
                blockOnlinePlayback(track)
                return
            }
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

            // Automatic queue progression, skip buttons and notification controls can all move to
            // another media item. Enforce activation here before recording a play or continuing.
            if (requiresActivation(track)) {
                fadeOutTriggeredForIndex = -1
                blockOnlinePlayback(track)
                return
            }

            if (track != null) {
                controllerScope.launch { repository.recordPlayed(track) }
                if (settingsRepository.state.value.aiDjEnabled && index >= currentQueue.size - 2) {
                    extendQueueWithAiDj(track)
                }
            }
            fadeOutTriggeredForIndex = -1
            val crossfadeMs = settingsRepository.state.value.crossfadeDurationMs
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
        val track = currentQueue.getOrNull(index)?.track
        _state.update {
            it.copy(
                isPlaying = c.isPlaying,
                currentIndex = index,
                currentTrack = track,
                queue = currentQueue,
                positionMs = c.currentPosition.coerceAtLeast(0),
                durationMs = c.duration.coerceAtLeast(0),
                shuffleEnabled = c.shuffleModeEnabled,
                repeatMode = c.repeatMode.toRepeatMode(),
                volume = c.volume,
                playbackSpeed = c.playbackParameters.speed
            )
        }
        if (c.isPlaying && requiresActivation(track)) {
            blockOnlinePlayback(track)
        } else {
            updateTicker(c.isPlaying)
        }
    }

    private fun updateTicker(isPlaying: Boolean) {
        tickerJob?.cancel()
        if (!isPlaying) {
            flushListeningTime()
            if (crossfadeJob?.isActive == true) {
                crossfadeJob?.cancel()
                controller?.volume = targetVolume
            }
            return
        }
        lastListeningTickMs = SystemClock.elapsedRealtime()
        tickerJob = controllerScope.launch {
            while (isActive) {
                val nowTick = SystemClock.elapsedRealtime()
                val delta = (nowTick - lastListeningTickMs).coerceIn(0L, 2000L)
                lastListeningTickMs = nowTick
                listeningAccumulatorMs += delta
                if (listeningAccumulatorMs >= 10_000L) flushListeningTime()

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

    private fun flushListeningTime() {
        val pending = listeningAccumulatorMs
        if (pending <= 0L) return
        listeningAccumulatorMs = 0L
        listeningTimeRepository.recordListening(pending)
    }

    private fun maybeStartCrossfadeOut(c: MediaController) {
        val crossfadeMs = settingsRepository.state.value.crossfadeDurationMs
        if (crossfadeMs <= 0) return
        val duration = c.duration.takeIf { it != C.TIME_UNSET } ?: return
        if (!c.hasNextMediaItem() || duration <= crossfadeMs) return
        val currentIndex = c.currentMediaItemIndex
        if (fadeOutTriggeredForIndex == currentIndex) return
        val remaining = duration - c.currentPosition
        if (remaining in 0..crossfadeMs.toLong()) {
            fadeOutTriggeredForIndex = currentIndex
            startFade(from = targetVolume, to = 0f, durationMs = remaining.toInt().coerceIn(1, crossfadeMs))
        }
    }

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
                if (crossfadeJob === job) isFading = false
            }
        }
        crossfadeJob = job
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        val safeStartIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val requestedTrack = tracks[safeStartIndex]

        // Do not even hand a protected online URL to ExoPlayer until access is active. Previously
        // playback started first and Compose paused it afterwards, which allowed the stream to
        // continue after the dialog was dismissed on some devices.
        if (requiresActivation(requestedTrack)) {
            blockOnlinePlayback(requestedTrack)
            return
        }

        crossfadeJob?.cancel()
        fadeOutTriggeredForIndex = -1
        c.volume = targetVolume
        currentQueue = tracks.map { QueueEntry(nextInstanceId++, it) }
        val items = tracks.map { it.toMediaItem() }
        c.setMediaItems(items, safeStartIndex, 0L)
        c.prepare()
        c.play()
        _state.update { it.copy(queue = currentQueue) }
    }

    fun playPause() {
        val c = controller ?: return
        val currentTrack = currentQueue.getOrNull(c.currentMediaItemIndex)?.track ?: _state.value.currentTrack
        if (!c.isPlaying && requiresActivation(currentTrack)) {
            blockOnlinePlayback(currentTrack)
            return
        }
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
        val requestedTrack = currentQueue[index].track
        if (requiresActivation(requestedTrack)) {
            blockOnlinePlayback(requestedTrack)
            return
        }
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
        flushListeningTime()
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
