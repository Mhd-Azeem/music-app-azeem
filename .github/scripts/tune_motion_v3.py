from pathlib import Path
import re

root = Path('.')
np = root / 'app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt'
text = np.read_text()

# 1) Replace parallax engine with orientation + gyroscope response.
start = text.index('    // Physical parallax only:')
end = text.index('    // Scale is animated only when the live FFT detector below reports a real audio onset.', start)
new_parallax = '''    // Spatial parallax: calibrated physical orientation plus gyroscope angular velocity.
    // The album art is deliberately rendered smaller while active so the extra movement has
    // breathing room, similar to the spatial/depth motion used by iPhone artwork effects.
    var sensorTiltX by remember { mutableFloatStateOf(0f) }
    var sensorTiltY by remember { mutableFloatStateOf(0f) }
    var sensorGyroX by remember { mutableFloatStateOf(0f) }
    var sensorGyroY by remember { mutableFloatStateOf(0f) }
    DisposableEffect(parallaxAlbumArt, context) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val gravityFallback = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val motionSensor = rotationSensor ?: gravityFallback
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val listener = object : SensorEventListener {
            private var baselinePitch: Float? = null
            private var baselineRoll: Float? = null
            private var baselineGravityX: Float? = null
            private var baselineGravityY: Float? = null
            private var filteredX = 0f
            private var filteredY = 0f
            private var filteredGyroX = 0f
            private var filteredGyroY = 0f
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            private fun angularDelta(value: Float, baseline: Float): Float {
                var delta = value - baseline
                val pi = Math.PI.toFloat()
                val twoPi = (Math.PI * 2.0).toFloat()
                while (delta > pi) delta -= twoPi
                while (delta < -pi) delta += twoPi
                return delta
            }

            override fun onSensorChanged(event: SensorEvent) {
                if (!parallaxAlbumArt) return

                if (event.sensor.type == Sensor.TYPE_GYROSCOPE && event.values.size >= 2) {
                    // Angular velocity gives the artwork a responsive inertial nudge while the
                    // phone is being moved. When motion stops the sensor itself reports ~0, so
                    // this is still entirely physical input rather than an artificial animation.
                    val gx = (event.values[1] / 2.2f).coerceIn(-1.35f, 1.35f)
                    val gy = (-event.values[0] / 2.2f).coerceIn(-1.35f, 1.35f)
                    filteredGyroX += (gx - filteredGyroX) * 0.46f
                    filteredGyroY += (gy - filteredGyroY) * 0.46f
                    sensorGyroX = filteredGyroX
                    sensorGyroY = filteredGyroY
                    return
                }

                val targetX: Float
                val targetY: Float
                if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR ||
                    event.sensor.type == Sensor.TYPE_ROTATION_VECTOR
                ) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val pitch = orientation[1]
                    val roll = orientation[2]
                    if (baselinePitch == null || baselineRoll == null) {
                        baselinePitch = pitch
                        baselineRoll = roll
                        return
                    }
                    // Reach full depth movement with about 8 degrees of phone tilt. This is
                    // intentionally much more sensitive than the previous gentle 14-degree range.
                    val maxTiltRad = Math.toRadians(8.0).toFloat()
                    targetX = (angularDelta(roll, baselineRoll!!) / maxTiltRad).coerceIn(-1.2f, 1.2f)
                    targetY = (angularDelta(pitch, baselinePitch!!) / maxTiltRad).coerceIn(-1.2f, 1.2f)
                } else {
                    if (event.values.size < 2) return
                    val gx = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
                    val gy = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                    if (baselineGravityX == null || baselineGravityY == null) {
                        baselineGravityX = gx
                        baselineGravityY = gy
                        return
                    }
                    targetX = ((gx - baselineGravityX!!) * 3.8f).coerceIn(-1.2f, 1.2f)
                    targetY = ((gy - baselineGravityY!!) * 3.8f).coerceIn(-1.2f, 1.2f)
                }

                filteredX += (targetX - filteredX) * 0.40f
                filteredY += (targetY - filteredY) * 0.40f
                sensorTiltX = filteredX
                sensorTiltY = filteredY
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (parallaxAlbumArt && motionSensor != null) {
            sensorManager.registerListener(listener, motionSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        if (parallaxAlbumArt && gyroSensor != null) {
            sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sensorManager.unregisterListener(listener)
            sensorTiltX = 0f
            sensorTiltY = 0f
            sensorGyroX = 0f
            sensorGyroY = 0f
        }
    }

'''
text = text[:start] + new_parallax + text[end:]

# 2) Replace beat detector with hybrid FFT + waveform real-onset detection.
start = text.index('    // FFT-based bass/onset detector.')
end = text.index('    val lyricsState by viewModel.lyrics.collectAsStateWithLifecycle()', start)
new_beat = '''    // Hybrid real-audio beat detector. FFT detects kick/bass onsets when available; waveform
    // energy provides a device-compatible fallback if a vendor does not deliver FFT callbacks.
    // Both paths share one refractory timer so a single beat cannot double-trigger the bounce.
    val beatDetectorState = remember { floatArrayOf(0f, 0f, 0f, 0f, 0f) }
    // 0 avgBass, 1 avgFlux, 2 previousBass, 3 avgRms, 4 previousRms
    val lastBeatAtMs = remember { longArrayOf(0L) }

    LaunchedEffect(visualizerFft, visualizerSamplingRateHz, beatBounceAlbumArt, state.isPlaying) {
        val fft = visualizerFft
        if (!beatBounceAlbumArt || !state.isPlaying || fft == null || fft.size < 64) {
            return@LaunchedEffect
        }

        val captureSize = fft.size
        val sampleRate = visualizerSamplingRateHz.coerceAtLeast(8_000)
        var bassMagnitudeSum = 0.0
        var bassBins = 0
        var lowMidMagnitudeSum = 0.0
        var lowMidBins = 0

        for (bin in 1 until captureSize / 2) {
            val frequencyHz = bin.toFloat() * sampleRate.toFloat() / captureSize.toFloat()
            if (frequencyHz > 520f) break
            val reIndex = bin * 2
            val imIndex = reIndex + 1
            if (imIndex >= fft.size) break
            val re = fft[reIndex].toInt().toFloat()
            val im = fft[imIndex].toInt().toFloat()
            val magnitude = kotlin.math.sqrt(re * re + im * im)
            when {
                frequencyHz in 35f..220f -> {
                    bassMagnitudeSum += magnitude
                    bassBins++
                }
                frequencyHz in 220f..520f -> {
                    lowMidMagnitudeSum += magnitude
                    lowMidBins++
                }
            }
        }

        if (bassBins == 0) return@LaunchedEffect
        val bass = (bassMagnitudeSum / bassBins).toFloat()
        val lowMid = if (lowMidBins > 0) (lowMidMagnitudeSum / lowMidBins).toFloat() else 0f

        var avgBass = beatDetectorState[0]
        var avgFlux = beatDetectorState[1]
        val previousBass = beatDetectorState[2]
        if (avgBass <= 0f) avgBass = bass.coerceAtLeast(1f)
        val flux = (bass - previousBass).coerceAtLeast(0f)
        if (avgFlux <= 0f) avgFlux = flux.coerceAtLeast(0.35f)

        val bassRatio = bass / avgBass.coerceAtLeast(1f)
        val fluxRatio = flux / avgFlux.coerceAtLeast(0.35f)
        val tonalContrast = bass / (lowMid + 1f)
        val now = SystemClock.elapsedRealtime()
        val isBeat = bassRatio >= 1.10f &&
            fluxRatio >= 1.16f &&
            bass >= 2.0f &&
            tonalContrast >= 0.52f &&
            now - lastBeatAtMs[0] >= 125L

        val clippedBass = minOf(bass, avgBass * 1.45f)
        val clippedFlux = minOf(flux, avgFlux * 2.1f)
        beatDetectorState[0] = avgBass * 0.95f + clippedBass * 0.05f
        beatDetectorState[1] = avgFlux * 0.91f + clippedFlux * 0.09f
        beatDetectorState[2] = bass

        if (isBeat) {
            lastBeatAtMs[0] = now
            beatPulseAmount = (0.060f + (bassRatio - 1.10f) * 0.10f)
                .coerceIn(0.060f, 0.145f)
            beatPulseSequence++
        }
    }

    LaunchedEffect(visualizerWaveform, beatBounceAlbumArt, state.isPlaying) {
        val waveform = visualizerWaveform
        if (!beatBounceAlbumArt || !state.isPlaying || waveform == null || waveform.size < 32) {
            return@LaunchedEffect
        }

        var squareSum = 0.0
        var peak = 0f
        waveform.forEach { sample ->
            val centered = (((sample.toInt() and 0xFF) - 128) / 128f)
            val a = kotlin.math.abs(centered)
            squareSum += (centered * centered).toDouble()
            if (a > peak) peak = a
        }
        val rms = kotlin.math.sqrt(squareSum / waveform.size).toFloat()
        var avgRms = beatDetectorState[3]
        val previousRms = beatDetectorState[4]
        if (avgRms <= 0f) avgRms = rms.coerceAtLeast(0.008f)
        val rise = (rms - previousRms).coerceAtLeast(0f)
        val ratio = rms / avgRms.coerceAtLeast(0.008f)
        val now = SystemClock.elapsedRealtime()

        val waveformBeat = ratio >= 1.13f &&
            rise >= maxOf(0.006f, avgRms * 0.08f) &&
            peak >= 0.20f &&
            now - lastBeatAtMs[0] >= 125L

        val clippedRms = minOf(rms, avgRms * 1.35f)
        beatDetectorState[3] = avgRms * 0.94f + clippedRms * 0.06f
        beatDetectorState[4] = rms

        if (waveformBeat) {
            lastBeatAtMs[0] = now
            beatPulseAmount = (0.058f + (ratio - 1.13f) * 0.12f)
                .coerceIn(0.058f, 0.135f)
            beatPulseSequence++
        }
    }

'''
text = text[:start] + new_beat + text[end:]

# 3) Increase spatial motion and shrink artwork to create movement room.
old_motion = '''                                                rotationX = if (parallaxAlbumArt) sensorTiltY * 5.5f else 0f
                                                rotationY = if (parallaxAlbumArt) -sensorTiltX * 7.5f else 0f
                                                translationX = if (parallaxAlbumArt) sensorTiltX * 12f else 0f
                                                translationY = if (parallaxAlbumArt) sensorTiltY * 8f else 0f'''
new_motion = '''                                                val spatialX = (sensorTiltX + sensorGyroX * 0.45f).coerceIn(-1.35f, 1.35f)
                                                val spatialY = (sensorTiltY + sensorGyroY * 0.45f).coerceIn(-1.35f, 1.35f)
                                                rotationX = if (parallaxAlbumArt) spatialY * 11f else 0f
                                                rotationY = if (parallaxAlbumArt) -spatialX * 14f else 0f
                                                translationX = if (parallaxAlbumArt) spatialX * 28f else 0f
                                                translationY = if (parallaxAlbumArt) spatialY * 22f else 0f'''
if old_motion not in text:
    raise SystemExit('vinyl motion block not found')
text = text.replace(old_motion, new_motion, 1)

old_motion2 = '''                                            rotationX = if (parallaxAlbumArt) sensorTiltY * 5.5f else 0f
                                            rotationY = if (parallaxAlbumArt) -sensorTiltX * 7.5f else 0f
                                            translationX = if (parallaxAlbumArt) sensorTiltX * 12f else 0f
                                            translationY = if (parallaxAlbumArt) sensorTiltY * 8f else 0f'''
new_motion2 = '''                                            val spatialX = (sensorTiltX + sensorGyroX * 0.45f).coerceIn(-1.35f, 1.35f)
                                            val spatialY = (sensorTiltY + sensorGyroY * 0.45f).coerceIn(-1.35f, 1.35f)
                                            rotationX = if (parallaxAlbumArt) spatialY * 11f else 0f
                                            rotationY = if (parallaxAlbumArt) -spatialX * 14f else 0f
                                            translationX = if (parallaxAlbumArt) spatialX * 28f else 0f
                                            translationY = if (parallaxAlbumArt) spatialY * 22f else 0f'''
if old_motion2 not in text:
    raise SystemExit('album motion block not found')
text = text.replace(old_motion2, new_motion2, 1)
text = text.replace('                                                    parallaxAlbumArt -> 1.025f', '                                                    parallaxAlbumArt -> 0.90f', 1)
text = text.replace('                                                parallaxAlbumArt -> 1.03f', '                                                parallaxAlbumArt -> 0.90f', 1)
np.write_text(text)

# 4) Add output-mix permission used by Visualizer session-0 fallback.
manifest = root / 'app/src/main/AndroidManifest.xml'
mt = manifest.read_text()
perm = '    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />\n'
if 'android.permission.MODIFY_AUDIO_SETTINGS' not in mt:
    mt = mt.replace('    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n', '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n' + perm)
manifest.write_text(mt)

# 5) Replace VisualizerController with specific-session -> output-mix fallback and normalized capture.
vc = root / 'app/src/main/java/com/wavelength/music/playback/VisualizerController.kt'
vc.write_text('''package com.wavelength.music.playback

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
''')
