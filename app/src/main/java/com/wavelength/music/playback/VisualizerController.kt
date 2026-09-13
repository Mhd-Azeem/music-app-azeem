package com.wavelength.music.playback

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Wraps [Visualizer] for live waveform and FFT capture from playback. */
@Singleton
class VisualizerController @Inject constructor() {
    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = 0
    private var captureEnabled = false

    private val _waveform = MutableStateFlow<ByteArray?>(null)
    val waveform: StateFlow<ByteArray?> = _waveform.asStateFlow()

    private val _fft = MutableStateFlow<ByteArray?>(null)
    val fft: StateFlow<ByteArray?> = _fft.asStateFlow()

    private val _samplingRateHz = MutableStateFlow(44_100)
    val samplingRateHz: StateFlow<Int> = _samplingRateHz.asStateFlow()

    private val _isSupported = MutableStateFlow(false)
    val isSupported: StateFlow<Boolean> = _isSupported.asStateFlow()

    fun onAudioSessionIdChanged(sessionId: Int) {
        if (sessionId == currentSessionId && visualizer != null) return
        releaseVisualizerOnly()
        currentSessionId = sessionId
        attachIfReady()
    }

    fun setCaptureEnabled(enabled: Boolean) {
        captureEnabled = enabled
        if (enabled) attachIfReady() else release()
    }

    private fun attachIfReady() {
        if (!captureEnabled || visualizer != null) return

        // Prefer ExoPlayer's own audio session. Some vendor audio stacks reject Visualizer on
        // that session; in that case retry session 0 (the device output mix) so Beat Bounce still
        // receives real audio. Session 0 is why MODIFY_AUDIO_SETTINGS is declared in the manifest.
        val candidates = buildList {
            if (currentSessionId > 0) add(currentSessionId)
            add(0)
        }.distinct()

        for (sessionId in candidates) {
            val attached = runCatching {
                val v = Visualizer(sessionId)
                v.enabled = false
                v.captureSize = Visualizer.getCaptureSizeRange()[1]
                v.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                val result = v.setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int
                        ) {
                            _samplingRateHz.value = (samplingRate / 1000).coerceAtLeast(1)
                            _waveform.value = waveform?.copyOf()
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int
                        ) {
                            _samplingRateHz.value = (samplingRate / 1000).coerceAtLeast(1)
                            _fft.value = fft?.copyOf()
                        }
                    },
                    Visualizer.getMaxCaptureRate(),
                    true,
                    true
                )
                check(result == Visualizer.SUCCESS) { "Visualizer listener error: $result" }
                v.enabled = true
                visualizer = v
                true
            }.getOrElse { false }

            if (attached) {
                _isSupported.value = true
                return
            }
        }

        _isSupported.value = false
        _waveform.value = null
        _fft.value = null
    }

    private fun releaseVisualizerOnly() {
        runCatching { visualizer?.enabled = false }
        runCatching { visualizer?.release() }
        visualizer = null
    }

    private fun release() {
        releaseVisualizerOnly()
        _waveform.value = null
        _fft.value = null
    }
}
