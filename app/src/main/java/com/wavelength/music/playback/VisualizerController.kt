package com.wavelength.music.playback

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Wraps [Visualizer] for live waveform and FFT capture from the active playback session. */
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
        release()
        currentSessionId = sessionId
        attachIfReady()
    }

    /** Enable capture only while a UI feature needs live playback analysis. */
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
                        // Android reports this sampling rate in milliHertz.
                        _samplingRateHz.value = (samplingRate / 1000).coerceAtLeast(1)
                        _waveform.value = waveform?.copyOf()
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        _samplingRateHz.value = (samplingRate / 1000).coerceAtLeast(1)
                        _fft.value = fft?.copyOf()
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                true
            )
            v.enabled = true
            visualizer = v
            _isSupported.value = true
        }.onFailure {
            _isSupported.value = false
            _waveform.value = null
            _fft.value = null
        }
    }

    private fun release() {
        runCatching { visualizer?.release() }
        visualizer = null
        _waveform.value = null
        _fft.value = null
    }
}
