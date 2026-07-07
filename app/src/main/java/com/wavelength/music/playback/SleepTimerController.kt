package com.wavelength.music.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Pauses playback after a chosen duration, or at the end of the current track — a pure
 * client-side timer that just calls [PlayerController.pause] when it elapses. */
@Singleton
class SleepTimerController @Inject constructor(
    private val playerController: PlayerController
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var job: Job? = null

    private val _remainingMs = MutableStateFlow<Long?>(null)
    val remainingMs: StateFlow<Long?> = _remainingMs.asStateFlow()

    private val _isEndOfTrack = MutableStateFlow(false)
    val isEndOfTrack: StateFlow<Boolean> = _isEndOfTrack.asStateFlow()

    fun startCountdown(durationMs: Long) {
        cancel()
        _remainingMs.value = durationMs
        job = scope.launch {
            var remaining = durationMs
            while (isActive && remaining > 0) {
                delay(1000)
                remaining -= 1000
                _remainingMs.value = remaining.coerceAtLeast(0)
            }
            if (isActive) {
                playerController.pause()
                _remainingMs.value = null
            }
        }
    }

    fun startEndOfTrack() {
        cancel()
        _isEndOfTrack.value = true
        val startIndex = playerController.state.value.currentIndex
        job = scope.launch {
            playerController.state.collect { state ->
                if (state.currentIndex != startIndex) {
                    playerController.pause()
                    cancel()
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = null
        _isEndOfTrack.value = false
    }
}
