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
import com.wavelength.music.data.repository.AppExperienceMode
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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val activationRepository: ActivationRepository,
    private val listeningTimeRepository: ListeningTimeRepository,
    private val snapshotStore: QueueSnapshotStore
) {
    private var controller: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var currentQueue: List<QueueEntry> = emptyList()
    private var activePlaylistId: Long? = null
    private var nextInstanceId = 0L
    private var queueGeneration = 0L
    private var recordedEntry = false
    private var autoQueueSuppressed = false
    private var radioSeed: Track? = null
    private var radioPage = 0
    private var radioExhausted = false
    private var aiDjExtendJob: Job? = null
    private var resumeAfterGap: Job? = null
    private var lastCheckpointMs = 0L

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

    private fun notifyActivationRequired() {
        _activationRequiredSequence.value += 1L
    }

    private fun blockOnlinePlayback() {
        resumeAfterGap?.cancel()
        crossfadeJob?.cancel()
        controller?.pause()
        _state.update { it.copy(isPlaying = false) }
        notifyActivationRequired()
    }

    private fun entryAt(c: MediaController, index: Int): QueueEntry? =
        currentQueue.getOrNull(index)?.takeIf { index in 0 until c.mediaItemCount &&
            c.getMediaItemAt(index).mediaId == it.instanceId.toString() }

    private fun publishQueue(c: MediaController, updatePosition: Boolean = false) {
        val index = c.currentMediaItemIndex
        val entry = entryAt(c, index)
        _state.update {
            it.copy(
                queue = currentQueue,
                currentIndex = if (entry != null) index else -1,
                currentEntryId = entry?.instanceId,
                currentTrack = entry?.track,
                positionMs = if (updatePosition) c.currentPosition.coerceAtLeast(0L) else it.positionMs,
                durationMs = c.duration.coerceAtLeast(0L),
                shuffleEnabled = c.shuffleModeEnabled,
                repeatMode = c.repeatMode.toRepeatMode()
            )
        }
        checkpoint(c)
    }

    private fun checkpoint(c: MediaController) {
        snapshotStore.save(
            QueueSnapshot(
                currentQueue,
                entryAt(c, c.currentMediaItemIndex)?.instanceId,
                c.currentPosition.coerceAtLeast(0L),
                c.shuffleModeEnabled,
                c.repeatMode,
                activePlaylistId,
                autoQueueSuppressed,
                radioSeed
            )
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val c = controller ?: return
            val track = entryAt(c, c.currentMediaItemIndex)?.track
            if (isPlaying && requiresActivation(track)) {
                // This also catches play commands coming from the media notification/widget rather
                // than the Compose UI, so dismissing the activation dialog can never resume a
                // protected online stream.
                blockOnlinePlayback()
                return
            }
            if (isPlaying && track != null) recordPlaybackStart(track)
            if (isPlaying && radioSeed != null) ensureSmartQueue()
            _state.update { it.copy(isPlaying = isPlaying) }
            updateTicker(isPlaying)
            if (!isPlaying) checkpoint(c)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) controller?.let(::checkpoint)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val c = controller ?: return
            val index = c.currentMediaItemIndex
            val track = entryAt(c, index)?.track
            resumeAfterGap?.cancel()
            recordedEntry = false
            publishQueue(c, updatePosition = true)

            // Automatic queue progression, skip buttons and notification controls can all move to
            // another media item. Enforce activation here before recording a play or continuing.
            if (requiresActivation(track)) {
                fadeOutTriggeredForIndex = -1
                blockOnlinePlayback()
                return
            }

            if (track != null) {
                if (c.isPlaying) recordPlaybackStart(track)
                ensureSmartQueue()
            }
            fadeOutTriggeredForIndex = -1
            val crossfadeMs = settingsRepository.state.value.crossfadeDurationMs
            val gaplessEnabled = settingsRepository.state.value.gaplessPlaybackEnabled
            val isNaturalProgression = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            if (crossfadeMs > 0 && isNaturalProgression) {
                startFade(from = 0f, to = targetVolume, durationMs = crossfadeMs)
            } else {
                crossfadeJob?.cancel()
                controller?.volume = targetVolume
                // Media3 playlists are naturally gapless for supported streams. When the user
                // explicitly disables Gapless Playback, add a short intentional separation only
                // for automatic transitions. Manual skips remain immediate.
                if (isNaturalProgression && !gaplessEnabled) {
                    val transitionId = mediaItem?.mediaId
                    c.pause()
                    resumeAfterGap = controllerScope.launch {
                        delay(350L)
                        val active = controller
                        if (active != null && active.currentMediaItem?.mediaId == transitionId &&
                            !requiresActivation(track)
                        ) {
                            active.play()
                        }
                    }
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            val c = controller ?: return
            if (shuffleModeEnabled && !_state.value.shuffleEnabled) {
                _state.update { it.copy(shuffleEnabled = true) }
                val start = upcomingStart(c)
                reorderUpcoming(c, currentQueue.drop(start).shuffled())
            }
            publishQueue(c)
        }

        override fun onEvents(player: Player, events: Player.Events) {
            val c = controller ?: return
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY
                )
            ) {
                publishQueue(c, updatePosition = events.contains(Player.EVENT_POSITION_DISCONTINUITY))
            }
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

    private fun recordPlaybackStart(track: Track) {
        if (recordedEntry) return
        recordedEntry = true
        controllerScope.launch {
            repository.recordPlayed(track)
            activationRepository.reportUsage(playCountDelta = 1)
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun connect() {
        if (controller != null || controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(disconnected: MediaController) {
                    if (controller !== disconnected) return
                    disconnected.removeListener(playerListener)
                    controller = null
                    controllerFuture = null
                    resumeAfterGap?.cancel()
                    updateTicker(false)
                    _state.update { it.copy(isPlaying = false, isBuffering = false) }
                    connect()
                }
            })
            .buildAsync()
        controllerFuture = future
        future.addListener(
            {
                controllerScope.launch {
                    val connected = runCatching { future.get() }.getOrNull()
                    if (controllerFuture !== future) return@launch
                    if (connected == null) {
                        controllerFuture = null
                        MediaController.releaseFuture(future)
                        return@launch
                    }
                    val saved = withContext(Dispatchers.IO) { snapshotStore.load() }
                    if (controllerFuture !== future) return@launch
                    controller = connected
                    restoreQueue(connected, saved)
                    connected.addListener(playerListener)
                    syncStateFromController()
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun restoreQueue(c: MediaController, saved: QueueSnapshot?) {
        if (c.mediaItemCount > 0 && currentQueue.isNotEmpty()) return
        val entries = saved?.entries.orEmpty()
        nextInstanceId = maxOf(nextInstanceId, (entries.maxOfOrNull { it.instanceId } ?: -1L) + 1L)
        activePlaylistId = saved?.playlistId
        autoQueueSuppressed = saved?.autoQueueSuppressed ?: false
        radioSeed = saved?.radioSeed
        radioPage = 0
        radioExhausted = false
        if (c.mediaItemCount > 0) {
            val byId = entries.associateBy { it.instanceId.toString() }
            currentQueue = (0 until c.mediaItemCount).mapNotNull { byId[c.getMediaItemAt(it).mediaId] }
            return
        }
        if (saved == null || entries.isEmpty()) return
        currentQueue = entries
        val index = entries.indexOfFirst { it.instanceId == saved.currentEntryId }
        c.setMediaItems(entries.map { it.mediaItem() }, index.coerceAtLeast(0), saved.positionMs)
        c.shuffleModeEnabled = saved.shuffled
        c.repeatMode = saved.repeatMode
        // Leave the restored player idle until the user presses play.
    }

    private fun syncStateFromController() {
        val c = controller ?: return
        targetVolume = c.volume
        publishQueue(c, updatePosition = true)
        _state.update {
            it.copy(
                isPlaying = c.isPlaying,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                volume = c.volume,
                playbackSpeed = c.playbackParameters.speed
            )
        }
        if (c.isPlaying && requiresActivation(entryAt(c, c.currentMediaItemIndex)?.track)) {
            blockOnlinePlayback()
        }
        updateTicker(c.isPlaying)
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
                    if (nowTick - lastCheckpointMs >= 5_000L) {
                        checkpoint(c)
                        lastCheckpointMs = nowTick
                    }
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
        controllerScope.launch {
            activationRepository.reportUsage(listenedMsDelta = pending)
        }
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

    fun playQueue(tracks: List<Track>, startIndex: Int = 0, playlistId: Long? = null): Long? {
        val c = controller ?: return null
        if (tracks.isEmpty()) return null
        val safeStartIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val requestedTrack = tracks[safeStartIndex]

        // Do not even hand a protected online URL to ExoPlayer until access is active. Previously
        // playback started first and Compose paused it afterwards, which allowed the stream to
        // continue after the dialog was dismissed on some devices.
        if (requiresActivation(requestedTrack)) {
            notifyActivationRequired()
            return null
        }

        crossfadeJob?.cancel()
        fadeOutTriggeredForIndex = -1
        c.volume = targetVolume
        aiDjExtendJob?.cancel()
        resumeAfterGap?.cancel()
        queueGeneration++
        autoQueueSuppressed = false
        radioSeed = null
        radioPage = 0
        radioExhausted = false
        activePlaylistId = playlistId
        val entries = tracks.distinctQueueEntries(safeStartIndex)
        val selected = entries.first
        val order = entries.second
        currentQueue = if (c.shuffleModeEnabled) listOf(selected) + order.filter { it != selected }.shuffled()
        else order
        c.setMediaItems(currentQueue.map { it.mediaItem() }, currentQueue.indexOf(selected), 0L)
        c.prepare()
        c.play()
        publishQueue(c, updatePosition = true)
        return queueGeneration
    }

    fun playPause() {
        val c = controller ?: return
        val currentTrack = currentQueue.getOrNull(c.currentMediaItemIndex)?.track ?: _state.value.currentTrack
        if (resumeAfterGap?.isActive == true) {
            resumeAfterGap?.cancel()
            return
        }
        if ((!c.playWhenReady || c.playbackState == Player.STATE_IDLE) && requiresActivation(currentTrack)) {
            blockOnlinePlayback()
            return
        }
        if (c.mediaItemCount == 0) return
        if (c.playbackState == Player.STATE_IDLE) {
            c.prepare()
            c.play()
            return
        }
        if (c.playWhenReady && c.playbackState != Player.STATE_ENDED) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
            c.play()
        }
    }

    fun resumePlayback() {
        val c = controller ?: return
        if (resumeAfterGap?.isActive == true) return
        if (!c.playWhenReady || c.playbackState == Player.STATE_IDLE || c.playbackState == Player.STATE_ENDED) {
            playPause()
        }
    }

    fun pause() {
        resumeAfterGap?.cancel()
        controller?.pause()
    }

    fun seekTo(positionMs: Long) {
        resumeAfterGap?.cancel()
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
        resumeAfterGap?.cancel()
        val c = controller ?: return
        if (!c.hasNextMediaItem()) return
        c.seekToNextMediaItem()
        if (c.playWhenReady && c.playbackState == Player.STATE_IDLE) c.prepare()
    }

    fun skipPrevious() {
        resumeAfterGap?.cancel()
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

    private fun upcomingStart(c: MediaController): Int =
        if (c.mediaItemCount == 0) 0 else c.currentMediaItemIndex + 1

    private fun QueueEntry.mediaItem(): MediaItem = track.toMediaItem(instanceId.toString())

    private fun List<Track>.distinctQueueEntries(startIndex: Int): Pair<QueueEntry, List<QueueEntry>> {
        val selected = this[startIndex]
        val entries = distinctTracksForPlayback(this, startIndex).map { QueueEntry(nextInstanceId++, it) }
        return entries.first { it.track.id == selected.id } to entries
    }

    fun appendRecommendations(generation: Long, tracks: List<Track>) {
        val c = controller ?: return
        if (generation == queueGeneration) addUpcoming(c, tracks, currentQueue.size, recommended = true)
    }

    fun addToQueue(track: Track) {
        val c = controller ?: return
        val start = upcomingStart(c)
        val tail = currentQueue.drop(start)
        val firstRecommended = tail.indexOfFirst { it.recommended }
        val target = if (firstRecommended < 0 || settingsRepository.state.value.experienceMode == AppExperienceMode.MINIMAL) {
            currentQueue.size
        } else start + firstRecommended
        val existing = tail.indexOfFirst { it.track.id == track.id }
        if (existing >= 0 && tail[existing].recommended) {
            val position = start + existing
            val entry = promoteToManual(position)
            val destination = if (position < target) target - 1 else target
            if (position == destination) publishQueue(c)
            else moveQueueItem(entry.instanceId, currentQueue[destination].instanceId)
        } else {
            addUpcoming(c, listOf(track), target)
        }
    }

    private fun promoteToManual(index: Int): QueueEntry {
        val entry = currentQueue[index]
        if (!entry.recommended) return entry
        return entry.copy(recommended = false).also { manual ->
            currentQueue = currentQueue.toMutableList().apply { set(index, manual) }
        }
    }

    private fun addUpcoming(c: MediaController, tracks: List<Track>, index: Int, recommended: Boolean = false) {
        val existing = currentQueue.asSequence().drop(c.currentMediaItemIndex.coerceAtLeast(0))
            .map { it.track.id }.toSet()
        val added = newUpcomingTracks(tracks, existing).map { QueueEntry(nextInstanceId++, it, recommended) }
        if (added.isEmpty()) return
        currentQueue = currentQueue.toMutableList().apply { addAll(index, added) }
        c.addMediaItems(index, added.map { it.mediaItem() })
        publishQueue(c)
    }

    fun ensureSmartQueue() {
        val c = controller ?: return
        if (autoQueueSuppressed || currentQueue.size - upcomingStart(c) > 2) return
        val track = entryAt(c, c.currentMediaItemIndex)?.track ?: return
        val settings = settingsRepository.state.value
        val seed = radioSeed
        if (seed != null) {
            if (settings.experienceMode == AppExperienceMode.NORMAL) refillRadio(seed)
        } else if (settings.aiDjEnabled) {
            extendQueueWithAiDj(track)
        }
    }

    fun startSongRadio(track: Track) {
        if (settingsRepository.state.value.experienceMode != AppExperienceMode.NORMAL) return
        if (!activationRepository.isAccessActive()) {
            notifyActivationRequired()
            return
        }
        playQueue(listOf(track)) ?: return
        aiDjExtendJob?.cancel()
        aiDjExtendJob = null
        radioSeed = track
        controller?.let(::checkpoint)
        ensureSmartQueue()
    }

    private fun refillRadio(seed: Track) {
        if (radioExhausted || aiDjExtendJob?.isActive == true || !activationRepository.isAccessActive()) return
        val artist = seed.artistName.substringBefore(',').trim()
        val album = seed.albumName.trim()
        val generation = queueGeneration
        aiDjExtendJob = controllerScope.launch {
            repeat(4) {
                val page = radioPage
                val query = if (page == 0 && album.isNotBlank()) album else artist.ifBlank { album }
                if (query.isBlank()) return@launch
                val catalogPage = if (artist.isNotBlank() && album.isNotBlank() && page > 0) page - 1 else page
                val results = repository.searchOnlineTracks(query, page = catalogPage, limit = 20)
                    .getOrElse { return@launch }
                if (generation != queueGeneration ||
                    settingsRepository.state.value.experienceMode != AppExperienceMode.NORMAL) return@launch
                radioPage++
                val ids = currentQueue.mapTo(hashSetOf()) { it.track.id }
                val related = results.filter { candidate ->
                    candidate.audioUrl.isNotBlank() && ids.add(candidate.id) &&
                        ((album.isNotBlank() && candidate.albumName.equals(album, ignoreCase = true)) ||
                            (artist.isNotBlank() && candidate.artistName.split(',').any { it.trim().equals(artist, ignoreCase = true) }))
                }
                if (related.isNotEmpty()) {
                    appendRecommendations(generation, related)
                    return@launch
                }
                if (results.isEmpty() && page > 0) {
                    radioExhausted = true
                    return@launch
                }
            }
        }
    }

    private fun extendQueueWithAiDj(justPlayed: Track) {
        if (autoQueueSuppressed || aiDjExtendJob?.isActive == true || justPlayed.artistName.isBlank()) return
        val generation = queueGeneration
        aiDjExtendJob = controllerScope.launch {
            val results = repository.searchTracks(justPlayed.artistName, limit = 10).getOrDefault(emptyList())
            val c = controller ?: return@launch
            if (generation != queueGeneration || !settingsRepository.state.value.aiDjEnabled) return@launch
            val existingIds = currentQueue.map { it.track.id }.toHashSet()
            val tracks = results.filter { existingIds.add(it.id) }.take(5)
            addUpcoming(c, tracks, currentQueue.size, recommended = true)
        }
    }

    fun playQueueItem(entryId: Long) {
        val c = controller ?: return
        val index = currentQueue.indexOfFirst { it.instanceId == entryId }
        val requestedTrack = entryAt(c, index)?.track ?: return
        if (requiresActivation(requestedTrack)) {
            notifyActivationRequired()
            return
        }
        resumeAfterGap?.cancel()
        c.seekToDefaultPosition(index)
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    fun moveQueueItem(entryId: Long, targetId: Long) {
        val c = controller ?: return
        val start = upcomingStart(c)
        val from = currentQueue.indexOfFirst { it.instanceId == entryId }
        val to = currentQueue.indexOfFirst { it.instanceId == targetId }
        if (from < start || to < start || from == to) return
        currentQueue = currentQueue.toMutableList().apply { add(to, removeAt(from)) }
        c.moveMediaItem(from, to)
        publishQueue(c)
    }

    fun removeQueueItem(entryId: Long) {
        val c = controller ?: return
        val index = currentQueue.indexOfFirst { it.instanceId == entryId }
        if (index < upcomingStart(c)) return
        currentQueue = currentQueue.toMutableList().apply { removeAt(index) }
        c.removeMediaItem(index)
        publishQueue(c)
    }

    fun clearUpcoming() {
        val c = controller ?: return
        queueGeneration++
        autoQueueSuppressed = true
        aiDjExtendJob?.cancel()
        val start = upcomingStart(c)
        val end = currentQueue.size
        if (start >= end) {
            checkpoint(c)
            return
        }
        currentQueue = currentQueue.take(start)
        c.removeMediaItems(start, end)
        publishQueue(c)
    }

    fun smartShuffleQueue() {
        val c = controller ?: return
        val remaining = currentQueue.drop(upcomingStart(c))
        if (remaining.size < 2) return
        controllerScope.launch {
            val playCounts = repository.getPlayCounts(remaining.map { it.track.id })
            val shuffled = withContext(Dispatchers.Default) {
                weightedShuffle(remaining) { entry -> playCounts[entry.track.id] ?: 0 }
            }
            if (controller === c && currentQueue.drop(upcomingStart(c)) == remaining) {
                reorderUpcoming(c, shuffled)
                publishQueue(c)
            }
        }
    }

    fun syncPlaylistOrder(playlistId: Long, orderedTrackIds: List<String>) {
        val c = controller ?: return
        if (activePlaylistId != playlistId) return
        val ranks = orderedTrackIds.withIndex().associate { it.value to it.index }
        val upcoming = currentQueue.drop(upcomingStart(c))
        val playlistEntries = upcoming.filter { it.track.id in ranks }
            .sortedBy { ranks.getValue(it.track.id) }.iterator()
        val reordered = upcoming.map { if (it.track.id in ranks) playlistEntries.next() else it }
        reorderUpcoming(c, reordered)
        publishQueue(c)
    }

    private fun reorderUpcoming(c: MediaController, reordered: List<QueueEntry>) {
        val start = upcomingStart(c)
        if (reordered.size < 2 || currentQueue.drop(start) == reordered) return
        // Replace only the future range. The active item, its buffer and its position stay intact.
        val end = currentQueue.size
        currentQueue = currentQueue.take(start) + reordered
        c.replaceMediaItems(start, end, reordered.map { it.mediaItem() })
    }

    fun playNext(track: Track) {
        val c = controller ?: return
        val start = upcomingStart(c)
        val offset = currentQueue.asSequence().drop(start).indexOfFirst { it.track.id == track.id }
        val index = if (offset < 0) -1 else start + offset
        if (index >= start) {
            val entry = promoteToManual(index)
            if (index > start) moveQueueItem(entry.instanceId, currentQueue[start].instanceId)
            else publishQueue(c)
        } else if (entryAt(c, c.currentMediaItemIndex)?.track?.id != track.id) {
            addUpcoming(c, listOf(track), start)
        }
    }

    fun release() {
        controller?.let(::checkpoint)
        resumeAfterGap?.cancel()
        flushListeningTime()
        tickerJob?.cancel()
        crossfadeJob?.cancel()
        aiDjExtendJob?.cancel()
        controller?.removeListener(playerListener)
        val future = controllerFuture
        controller = null
        controllerFuture = null
        future?.let { MediaController.releaseFuture(it) }
    }
}

private fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}
