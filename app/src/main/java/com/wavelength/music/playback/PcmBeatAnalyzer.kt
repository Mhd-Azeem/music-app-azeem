package com.wavelength.music.playback

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Beat detector fed directly by ExoPlayer's decoded PCM stream through [TeeAudioProcessor].
 *
 * This deliberately does not use Android's Visualizer API, RECORD_AUDIO or the microphone.
 * If ExoPlayer is producing audible PCM, this class sees the same samples before AudioTrack.
 */
@Singleton
class PcmBeatAnalyzer @Inject constructor() : TeeAudioProcessor.AudioBufferSink {

    data class BeatPulse(
        val sequence: Long = 0L,
        val strength: Float = 0f
    )

    private val _beatPulse = MutableStateFlow(BeatPulse())
    val beatPulse: StateFlow<BeatPulse> = _beatPulse.asStateFlow()

    private val _bassLevel = MutableStateFlow(0f)
    val bassLevel: StateFlow<Float> = _bassLevel.asStateFlow()

    private var sampleRateHz = 44_100
    private var channelCount = 2
    private var encoding = C.ENCODING_PCM_16BIT

    // Two low-pass filters are subtracted to form a simple 45-220 Hz bass/kick band-pass.
    // Per-channel filter state avoids left/right phase cancellation before analysis.
    private var low220Alpha = 0.03f
    private var low45Alpha = 0.006f
    private var low220State = FloatArray(2)
    private var low45State = FloatArray(2)

    private var bassSquareSum = 0.0
    private var fullSquareSum = 0.0
    private var framesInWindow = 0
    private var targetFramesPerWindow = 384

    private var averageBass = 0f
    private var averageFull = 0f
    private var previousBass = 0f
    private var previousFull = 0f
    private var lastBeatAtMs = 0L
    private var sequence = 0L

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRateHz = sampleRateHz.coerceAtLeast(8_000)
        this.channelCount = channelCount.coerceAtLeast(1)
        this.encoding = encoding

        // ~7-9 ms windows catch bass attacks without making the detector sluggish.
        targetFramesPerWindow = (this.sampleRateHz / 125).coerceIn(128, 768)
        low220Alpha = onePoleAlpha(220f)
        low45Alpha = onePoleAlpha(45f)
        low220State = FloatArray(this.channelCount)
        low45State = FloatArray(this.channelCount)

        bassSquareSum = 0.0
        fullSquareSum = 0.0
        framesInWindow = 0
        averageBass = 0f
        averageFull = 0f
        previousBass = 0f
        previousFull = 0f
        lastBeatAtMs = 0L
        _bassLevel.value = 0f
    }

    private fun onePoleAlpha(cutoffHz: Float): Float =
        (1f - exp((-2.0 * PI * cutoffHz / sampleRateHz).toFloat()))
            .coerceIn(0.001f, 0.35f)

    override fun handleBuffer(buffer: ByteBuffer) {
        val input = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        when (encoding) {
            C.ENCODING_PCM_16BIT -> consumePcm16(input)
            C.ENCODING_PCM_FLOAT -> consumePcmFloat(input)
            else -> Unit
        }
    }

    private fun consumePcm16(input: ByteBuffer) {
        val bytesPerFrame = 2 * channelCount
        while (input.remaining() >= bytesPerFrame) {
            var bassEnergy = 0.0
            var fullEnergy = 0.0
            for (channel in 0 until channelCount) {
                val sample = input.short / 32768f
                val band = filterBass(channel, sample)
                bassEnergy += (band * band).toDouble()
                fullEnergy += (sample * sample).toDouble()
            }
            analyzeFrameEnergy(
                bassEnergy / channelCount,
                fullEnergy / channelCount
            )
        }
    }

    private fun consumePcmFloat(input: ByteBuffer) {
        val bytesPerFrame = 4 * channelCount
        while (input.remaining() >= bytesPerFrame) {
            var bassEnergy = 0.0
            var fullEnergy = 0.0
            for (channel in 0 until channelCount) {
                val sample = input.float.coerceIn(-1f, 1f)
                val band = filterBass(channel, sample)
                bassEnergy += (band * band).toDouble()
                fullEnergy += (sample * sample).toDouble()
            }
            analyzeFrameEnergy(
                bassEnergy / channelCount,
                fullEnergy / channelCount
            )
        }
    }

    private fun filterBass(channel: Int, sample: Float): Float {
        low220State[channel] += low220Alpha * (sample - low220State[channel])
        low45State[channel] += low45Alpha * (sample - low45State[channel])
        return low220State[channel] - low45State[channel]
    }

    private fun analyzeFrameEnergy(bassEnergy: Double, fullEnergy: Double) {
        bassSquareSum += bassEnergy
        fullSquareSum += fullEnergy
        framesInWindow++

        if (framesInWindow < targetFramesPerWindow) return

        val bassRms = sqrt(bassSquareSum / framesInWindow).toFloat()
        val fullRms = sqrt(fullSquareSum / framesInWindow).toFloat()

        bassSquareSum = 0.0
        fullSquareSum = 0.0
        framesInWindow = 0

        if (averageBass <= 0f) averageBass = bassRms.coerceAtLeast(0.0025f)
        if (averageFull <= 0f) averageFull = fullRms.coerceAtLeast(0.006f)

        val bassRatio = bassRms / averageBass.coerceAtLeast(0.0025f)
        val fullRatio = fullRms / averageFull.coerceAtLeast(0.006f)
        val bassRise = (bassRms - previousBass).coerceAtLeast(0f)
        val fullRise = (fullRms - previousFull).coerceAtLeast(0f)
        val now = SystemClock.elapsedRealtime()

        // Continuous 0..1 bass intensity for Bass Zoom. Relative energy handles quiet masters;
        // absolute energy prevents a tiny amount of bass from looking huge merely because the
        // whole song is quiet. Smooth attack/release avoids jitter while preserving real dynamics.
        val relativeBass = ((bassRatio - 0.82f) / 0.95f).coerceIn(0f, 1f)
        val absoluteBass = ((bassRms - 0.006f) / 0.085f).coerceIn(0f, 1f)
        val measuredBass = (relativeBass * 0.68f + absoluteBass * 0.32f).coerceIn(0f, 1f)
        val previousLevel = _bassLevel.value
        val smoothing = if (measuredBass > previousLevel) 0.38f else 0.16f
        _bassLevel.value = previousLevel + (measuredBass - previousLevel) * smoothing

        // Measure how bass-heavy this instant really is, not just whether some transient happened.
        // This prevents light vocals/snare/quiet bass from producing the same visual jump as a kick.
        val bassShare = (bassRms / (fullRms + 0.0005f)).coerceIn(0f, 1.5f)
        val beatAbsoluteBass = ((bassRms - 0.0065f) / 0.055f).coerceIn(0f, 1f)
        val beatRelativeBass = ((bassRatio - 1.02f) / 0.55f).coerceIn(0f, 1f)
        val bassDominance = ((bassShare - 0.08f) / 0.42f).coerceIn(0f, 1f)
        val bassIntensity = (
            beatAbsoluteBass * 0.52f +
                beatRelativeBass * 0.30f +
                bassDominance * 0.18f
            ).coerceIn(0f, 1f)

        // Primary detector: real kick/bass onset. Secondary detector only participates when the
        // signal already contains meaningful bass, so broad drum/vocal transients stay calm.
        val bassOnset = bassRatio >= 1.08f &&
            bassRise >= max(0.00125f, averageBass * 0.040f) &&
            fullRatio >= 0.82f &&
            bassRms >= 0.007f

        val broadOnset = bassIntensity >= 0.42f &&
            fullRatio >= 1.14f &&
            fullRise >= max(0.0020f, averageFull * 0.045f) &&
            bassRatio >= 0.98f

        // Low-bass material stays calm. Medium/high bass must have a real onset and enough
        // measured bass intensity before any visual pulse is emitted.
        val isBeat = (bassOnset || broadOnset) &&
            bassIntensity >= 0.24f &&
            fullRms >= 0.010f &&
            now - lastBeatAtMs >= 108L

        // Baselines adapt slowly so loud masters and quiet songs both work while attacks remain
        // visible to the detector. Clamp transients before feeding them into the baseline.
        val clippedBass = minOf(bassRms, averageBass * 1.30f)
        val clippedFull = minOf(fullRms, averageFull * 1.24f)
        averageBass = averageBass * 0.972f + clippedBass * 0.028f
        averageFull = averageFull * 0.975f + clippedFull * 0.025f
        previousBass = bassRms
        previousFull = fullRms

        if (isBeat) {
            lastBeatAtMs = now
            sequence++

            // Visual strength follows measured bass intensity with a curved response:
            // medium bass stays restrained, while genuinely strong bass grows much more.
            val curved = bassIntensity.pow(1.65f)
            val strength = (0.055f + curved * 0.105f)
                .coerceIn(0.055f, 0.16f)
            _beatPulse.value = BeatPulse(sequence = sequence, strength = strength)
        }
    }
}
