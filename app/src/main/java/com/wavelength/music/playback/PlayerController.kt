package com.wavelength.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
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
    private val repository: MusicRepository
) {
    private var controller: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var currentQueue: List<Track> = emptyList()

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
            val track = currentQueue.getOrNull(index)
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
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _state.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _state.update { it.copy(repeatMode = repeatMode.toRepeatMode()) }
        }

        override fun onVolumeChanged(volume: Float) {
            _state.update { it.copy(volume = volume) }
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
        _state.update {
            it.copy(
                isPlaying = c.isPlaying,
                currentIndex = index,
                currentTrack = currentQueue.getOrNull(index),
                queue = currentQueue,
                positionMs = c.currentPosition.coerceAtLeast(0),
                durationMs = c.duration.coerceAtLeast(0),
                shuffleEnabled = c.shuffleModeEnabled,
                repeatMode = c.repeatMode.toRepeatMode(),
                volume = c.volume
            )
        }
        updateTicker(c.isPlaying)
    }

    private fun updateTicker(isPlaying: Boolean) {
        tickerJob?.cancel()
        if (!isPlaying) return
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
                }
                delay(500)
            }
        }
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        currentQueue = tracks
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

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        controller?.volume = clamped
        _state.update { it.copy(volume = clamped) }
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
        currentQueue = currentQueue + track
        _state.update { it.copy(queue = currentQueue) }
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

    fun playNext(track: Track) {
        val c = controller ?: return
        val insertIndex = (c.currentMediaItemIndex + 1).coerceIn(0, c.mediaItemCount)
        c.addMediaItem(insertIndex, track.toMediaItem())
        currentQueue = currentQueue.toMutableList().apply {
            add(insertIndex.coerceAtMost(size), track)
        }
        _state.update { it.copy(queue = currentQueue) }
    }

    fun release() {
        tickerJob?.cancel()
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
