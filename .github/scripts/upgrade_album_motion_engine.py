from pathlib import Path

path = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
text = path.read_text()


def replace_once(old: str, new: str):
    global text
    if old not in text:
        raise SystemExit(f'Pattern not found:\n{old[:300]}')
    text = text.replace(old, new, 1)

old_sensor = '''    // Real sensor-driven parallax. Prefer the gravity sensor (stable, already filtered by
    // Android), then fall back to the accelerometer on devices without TYPE_GRAVITY.
    var sensorTiltX by remember { mutableFloatStateOf(0f) }
    var sensorTiltY by remember { mutableFloatStateOf(0f) }
    DisposableEffect(parallaxAlbumArt, context) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val motionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            private var filteredX = 0f
            private var filteredY = 0f

            override fun onSensorChanged(event: SensorEvent) {
                if (!parallaxAlbumArt || event.values.size < 2) return
                // Normalize gravity to roughly -1..1 and low-pass it so the art follows the
                // phone naturally without jittering from tiny hand movements.
                val nx = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
                val ny = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                filteredX += (nx - filteredX) * 0.16f
                filteredY += (ny - filteredY) * 0.16f
                sensorTiltX = filteredX
                sensorTiltY = filteredY
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (parallaxAlbumArt && motionSensor != null) {
            sensorManager.registerListener(listener, motionSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sensorManager.unregisterListener(listener)
            sensorTiltX = 0f
            sensorTiltY = 0f
        }
    }
'''

new_sensor = '''    // Physical parallax only: use the phone's orientation sensor and calibrate the current
    // holding angle as neutral when the effect is enabled. There is no timer-driven fallback.
    var sensorTiltX by remember { mutableFloatStateOf(0f) }
    var sensorTiltY by remember { mutableFloatStateOf(0f) }
    DisposableEffect(parallaxAlbumArt, context) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val gravityFallback = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val motionSensor = rotationSensor ?: gravityFallback

        val listener = object : SensorEventListener {
            private var baselinePitch: Float? = null
            private var baselineRoll: Float? = null
            private var baselineGravityX: Float? = null
            private var baselineGravityY: Float? = null
            private var filteredX = 0f
            private var filteredY = 0f
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
                    val maxTiltRad = Math.toRadians(14.0).toFloat()
                    targetX = (angularDelta(roll, baselineRoll!!) / maxTiltRad).coerceIn(-1f, 1f)
                    targetY = (angularDelta(pitch, baselinePitch!!) / maxTiltRad).coerceIn(-1f, 1f)
                } else {
                    if (event.values.size < 2) return
                    val gx = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
                    val gy = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                    if (baselineGravityX == null || baselineGravityY == null) {
                        baselineGravityX = gx
                        baselineGravityY = gy
                        return
                    }
                    targetX = ((gx - baselineGravityX!!) * 2.4f).coerceIn(-1f, 1f)
                    targetY = ((gy - baselineGravityY!!) * 2.4f).coerceIn(-1f, 1f)
                }

                // Smooth sensor noise while still following hand movement immediately.
                filteredX += (targetX - filteredX) * 0.28f
                filteredY += (targetY - filteredY) * 0.28f
                sensorTiltX = filteredX
                sensorTiltY = filteredY
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (parallaxAlbumArt && motionSensor != null) {
            sensorManager.registerListener(listener, motionSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sensorManager.unregisterListener(listener)
            sensorTiltX = 0f
            sensorTiltY = 0f
        }
    }
'''
replace_once(old_sensor, new_sensor)

old_bounce_anim = '''    // Scale is animated only when the live audio detector below reports a real transient.
    val beatBounceScale = remember { Animatable(1f) }
    var beatPulseSequence by remember { mutableStateOf(0L) }
    LaunchedEffect(beatPulseSequence, beatBounceAlbumArt, state.isPlaying) {
        if (beatBounceAlbumArt && state.isPlaying && beatPulseSequence > 0L) {
            beatBounceScale.snapTo(1f)
            beatBounceScale.animateTo(
                1.085f,
                animationSpec = tween(durationMillis = 70, easing = LinearEasing)
            )
'''
new_bounce_anim = '''    // Scale is animated only when the live FFT detector below reports a real audio onset.
    val beatBounceScale = remember { Animatable(1f) }
    var beatPulseSequence by remember { mutableStateOf(0L) }
    var beatPulseAmount by remember { mutableFloatStateOf(0.075f) }
    LaunchedEffect(beatPulseSequence, beatBounceAlbumArt, state.isPlaying) {
        if (beatBounceAlbumArt && state.isPlaying && beatPulseSequence > 0L) {
            beatBounceScale.snapTo(1f)
            beatBounceScale.animateTo(
                1f + beatPulseAmount,
                animationSpec = tween(durationMillis = 62, easing = LinearEasing)
            )
'''
replace_once(old_bounce_anim, new_bounce_anim)

old_collect = '''    val visualizerWaveform by viewModel.visualizerWaveform.collectAsStateWithLifecycle()
    DisposableEffect(needsAudioCapture, hasRecordAudioPermission) {
'''
new_collect = '''    val visualizerWaveform by viewModel.visualizerWaveform.collectAsStateWithLifecycle()
    val visualizerFft by viewModel.visualizerFft.collectAsStateWithLifecycle()
    val visualizerSamplingRateHz by viewModel.visualizerSamplingRateHz.collectAsStateWithLifecycle()
    DisposableEffect(needsAudioCapture, hasRecordAudioPermission) {
'''
replace_once(old_collect, new_collect)

old_detector = '''    // Real beat detection from the currently playing audio. Visualizer waveform samples are
    // unsigned 8-bit PCM centered at 128. RMS gives short-term energy; an adaptive baseline
    // follows song loudness, so a bounce fires on transients instead of a fixed timer.
    val beatDetectorState = remember { floatArrayOf(0.035f) }
    val lastBeatAtMs = remember { longArrayOf(0L) }
    LaunchedEffect(visualizerWaveform, beatBounceAlbumArt, state.isPlaying) {
        val waveform = visualizerWaveform
        if (!beatBounceAlbumArt || !state.isPlaying || waveform == null || waveform.size < 32) {
            return@LaunchedEffect
        }

        var squareSum = 0.0
        var peak = 0f
        waveform.forEach { sample ->
            val centered = (((sample.toInt() and 0xFF) - 128) / 128f)
            val absolute = kotlin.math.abs(centered)
            squareSum += (centered * centered).toDouble()
            if (absolute > peak) peak = absolute
        }
        val rms = kotlin.math.sqrt(squareSum / waveform.size).toFloat()
        val baseline = beatDetectorState[0].coerceAtLeast(0.015f)
        val threshold = maxOf(0.055f, baseline * 1.38f)
        val now = SystemClock.elapsedRealtime()
        val isTransientBeat = rms > threshold && peak > 0.25f && now - lastBeatAtMs[0] >= 170L

        // Do not let a loud beat immediately drag the baseline up to itself. This keeps the
        // detector sensitive to the next rhythmic transient while still adapting across songs.
        val baselineSample = minOf(rms, baseline * 1.22f)
        beatDetectorState[0] = baseline * 0.92f + baselineSample * 0.08f

        if (isTransientBeat) {
            lastBeatAtMs[0] = now
            beatPulseSequence++
        }
    }
'''

new_detector = '''    // FFT-based bass/onset detector. It measures real spectral energy around kick/bass
    // frequencies and compares each frame with an adaptive baseline plus positive spectral flux.
    // No BPM clock, delay loop or synthetic pulse is used.
    val beatDetectorState = remember { floatArrayOf(0f, 0f, 0f) } // avgBass, avgFlux, previousBass
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

        // Android Visualizer packs FFT bins as real/imaginary byte pairs at 2*k, 2*k+1.
        for (bin in 1 until captureSize / 2) {
            val frequencyHz = bin.toFloat() * sampleRate.toFloat() / captureSize.toFloat()
            if (frequencyHz > 420f) break
            val reIndex = bin * 2
            val imIndex = reIndex + 1
            if (imIndex >= fft.size) break
            val re = fft[reIndex].toInt().toFloat()
            val im = fft[imIndex].toInt().toFloat()
            val magnitude = kotlin.math.sqrt(re * re + im * im)
            when {
                frequencyHz in 42f..190f -> {
                    bassMagnitudeSum += magnitude
                    bassBins++
                }
                frequencyHz in 190f..420f -> {
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
        if (avgFlux <= 0f) avgFlux = flux.coerceAtLeast(0.5f)

        val bassRatio = bass / avgBass.coerceAtLeast(1f)
        val fluxRatio = flux / avgFlux.coerceAtLeast(0.5f)
        val tonalContrast = bass / (lowMid + 1f)
        val now = SystemClock.elapsedRealtime()

        val isBeat = bassRatio >= 1.20f &&
            fluxRatio >= 1.32f &&
            bass >= 3.5f &&
            tonalContrast >= 0.72f &&
            now - lastBeatAtMs[0] >= 155L

        // Slow adaptation keeps the threshold tied to the current song rather than volume level.
        val clippedBass = minOf(bass, avgBass * 1.35f)
        val clippedFlux = minOf(flux, avgFlux * 1.8f)
        beatDetectorState[0] = avgBass * 0.94f + clippedBass * 0.06f
        beatDetectorState[1] = avgFlux * 0.90f + clippedFlux * 0.10f
        beatDetectorState[2] = bass

        if (isBeat) {
            lastBeatAtMs[0] = now
            // Stronger detected bass onsets create a slightly stronger physical-looking pulse.
            beatPulseAmount = (0.055f + (bassRatio - 1.20f) * 0.08f)
                .coerceIn(0.055f, 0.13f)
            beatPulseSequence++
        }
    }
'''
replace_once(old_detector, new_detector)

path.write_text(text)
print('Upgraded NowPlayingScreen to rotation-vector parallax and FFT beat detection.')
