package com.wavelength.music.playback

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Wraps `android.media.audiofx.Visualizer`, following the same (re)attach-on-audio-session-
 * change pattern as [EqualizerController]. Capture is opt-in via [setCaptureEnabled] — the
 * caller (Now Playing screen) only turns it on while a visualizer is actually visible AND the
 * user holds RECORD_AUDIO, since capturing costs battery/CPU and needs that runtime permission on
 * modern Android even for the app's own audio session. */
@Singleton
class VisualizerController @Inject constructor() {
    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = 0
    private var captureEnabled = false

    private val _waveform = MutableStateFlow<ByteArray?>(null)
    val waveform: StateFlow<ByteArray?> = _waveform.asStateFlow()

    private val _isSupported = MutableStateFlow(false)
    val isSupported: StateFlow<Boolean> = _isSupported.asStateFlow()

    fun onAudioSessionIdChanged(sessionId: Int) {
        if (sessionId == currentSessionId && visualizer != null) return
        release()
        currentSessionId = sessionId
        attachIfReady()
    }

    /** Call with `true` only while a visualizer UI is actually on screen and RECORD_AUDIO is
     * granted; `false` releases it immediately so it isn't capturing in the background. */
    fun setCaptureEnabled(enabled: Boolean) {
        captureEnabled = enabled
        if (enabled) {
            attachIfReady()
        } else {
            release()
        }
    }

    private fun attachIfReady() {
        if (!captureEnabled || currentSessionId == 0 || visualizer != null) return
        runCatching {
            val v = Visualizer(currentSessionId)
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        _waveform.value = waveform?.copyOf()
                    }

                    override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        // Waveform capture only — a bar/wave UI doesn't need frequency data.
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                false
            )
            v.enabled = true
            visualizer = v
            _isSupported.value = true
        }.onFailure {
            _isSupported.value = false
        }
    }

    private fun release() {
        runCatching { visualizer?.release() }
        visualizer = null
        _waveform.value = null
    }
}
