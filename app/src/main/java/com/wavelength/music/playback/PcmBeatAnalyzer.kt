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

    private var lowPassAlpha = 0.025f
    private var lowPassState = 0f
    private var bassSquareSum = 0.0
    private var fullSquareSum = 0.0
    private var framesInWindow = 0
    private var targetFramesPerWindow = 512

    private var averageBass = 0f
    private var averageFull = 0f
    private var previousBass = 0f
    private var lastBeatAtMs = 0L
    private var sequence = 0L

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRateHz = sampleRateHz.coerceAtLeast(8_000)
        this.channelCount = channelCount.coerceAtLeast(1)
        this.encoding = encoding

        // ~11–14 ms analysis windows depending on sample rate.
        targetFramesPerWindow = (this.sampleRateHz / 86).coerceIn(128, 1024)
        val cutoffHz = 190f
        lowPassAlpha = (1f - exp((-2.0 * PI * cutoffHz / this.sampleRateHz).toFloat()))
            .coerceIn(0.005f, 0.25f)

        lowPassState = 0f
        bassSquareSum = 0.0
        fullSquareSum = 0.0
        framesInWindow = 0
        averageBass = 0f
        averageFull = 0f
        previousBass = 0f
        lastBeatAtMs = 0L
    }

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
            var mono = 0f
            repeat(channelCount) {
                mono += input.short / 32768f
            }
            analyzeFrame(mono / channelCount)
        }
    }

    private fun consumePcmFloat(input: ByteBuffer) {
        val bytesPerFrame = 4 * channelCount
        while (input.remaining() >= bytesPerFrame) {
            var mono = 0f
            repeat(channelCount) {
                mono += input.float.coerceIn(-1f, 1f)
            }
            analyzeFrame(mono / channelCount)
        }
    }

    private fun analyzeFrame(sample: Float) {
        // One-pole low-pass isolates kick/bass energy while full-band RMS prevents noise triggers.
        lowPassState += lowPassAlpha * (sample - lowPassState)
        bassSquareSum += (lowPassState * lowPassState).toDouble()
        fullSquareSum += (sample * sample).toDouble()
        framesInWindow++

        if (framesInWindow < targetFramesPerWindow) return

        val bassRms = sqrt(bassSquareSum / framesInWindow).toFloat()
        val fullRms = sqrt(fullSquareSum / framesInWindow).toFloat()

        bassSquareSum = 0.0
        fullSquareSum = 0.0
        framesInWindow = 0

        if (averageBass <= 0f) averageBass = bassRms.coerceAtLeast(0.004f)
        if (averageFull <= 0f) averageFull = fullRms.coerceAtLeast(0.008f)

        val bassRatio = bassRms / averageBass.coerceAtLeast(0.004f)
        val fullRatio = fullRms / averageFull.coerceAtLeast(0.008f)
        val bassRise = (bassRms - previousBass).coerceAtLeast(0f)
        val now = SystemClock.elapsedRealtime()

        // A beat requires a real low-frequency onset, enough overall program energy, and a
        // refractory period. There is no timer-generated pulse or inferred BPM clock here.
        val isBeat = bassRatio >= 1.12f &&
            bassRise >= maxOf(0.0025f, averageBass * 0.07f) &&
            fullRms >= 0.018f &&
            fullRatio >= 0.90f &&
            now - lastBeatAtMs >= 115L

        // Slow baseline adaptation follows song/mastering loudness without swallowing transients.
        val clippedBass = minOf(bassRms, averageBass * 1.40f)
        val clippedFull = minOf(fullRms, averageFull * 1.32f)
        averageBass = averageBass * 0.965f + clippedBass * 0.035f
        averageFull = averageFull * 0.97f + clippedFull * 0.03f
        previousBass = bassRms

        if (isBeat) {
            lastBeatAtMs = now
            sequence++
            val strength = (0.055f + (bassRatio - 1.12f) * 0.14f)
                .coerceIn(0.055f, 0.16f)
            _beatPulse.value = BeatPulse(sequence = sequence, strength = strength)
        }
    }
}
