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

        // ~7-9 ms windows are short enough to catch kick/snare attacks without becoming noisy.
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

        // Primary detector: kick/bass onset. Secondary detector: broader drum transient for songs
        // where the kick is light but the rhythmic attack is still obvious. Both are PCM-driven.
        val bassOnset = bassRatio >= 1.045f &&
            bassRise >= max(0.0008f, averageBass * 0.025f) &&
            fullRatio >= 0.78f

        val broadOnset = fullRatio >= 1.10f &&
            fullRise >= max(0.0015f, averageFull * 0.035f) &&
            bassRatio >= 0.90f

        val isBeat = (bassOnset || broadOnset) &&
            fullRms >= 0.006f &&
            now - lastBeatAtMs >= 92L

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
            val onsetScore = max(
                (bassRatio - 1f) * 1.8f,
                (fullRatio - 1f) * 1.25f
            )
            val strength = (0.045f + onsetScore * 0.12f)
                .coerceIn(0.045f, 0.155f)
            _beatPulse.value = BeatPulse(sequence = sequence, strength = strength)
        }
    }
}
