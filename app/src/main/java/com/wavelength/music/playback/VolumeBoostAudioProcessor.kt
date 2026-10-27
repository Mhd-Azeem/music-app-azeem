package com.wavelength.music.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * VLC-style software gain stage for AzMusic.
 *
 * Gain is applied directly to decoded PCM before AudioTrack. A soft limiter keeps boosted peaks
 * inside the digital range instead of letting them hard-clip into the harsh screeching produced
 * by large LoudnessEnhancer gains on some vendor audio stacks.
 */
@UnstableApi
@Singleton
class VolumeBoostAudioProcessor @Inject constructor() : BaseAudioProcessor() {

    @Volatile
    private var gain = 1f

    fun setBoostPercent(percent: Int) {
        gain = percent.coerceIn(100, 200) / 100f
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT,
            C.ENCODING_PCM_FLOAT -> inputAudioFormat
            else -> AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val currentGain = gain
        val output = replaceOutputBuffer(inputBuffer.remaining())
        output.order(ByteOrder.nativeOrder())
        inputBuffer.order(ByteOrder.nativeOrder())

        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_FLOAT -> {
                while (inputBuffer.remaining() >= 4) {
                    val sample = inputBuffer.float
                    output.putFloat(processSample(sample * currentGain))
                }
            }
            C.ENCODING_PCM_16BIT -> {
                while (inputBuffer.remaining() >= 2) {
                    val sample = inputBuffer.short / 32768f
                    val processed = processSample(sample * currentGain)
                    val pcm = (processed * 32767f)
                        .roundToInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    output.putShort(pcm.toShort())
                }
            }
        }

        output.flip()
    }

    /**
     * Transparent below 92% full scale, then smoothly compresses peaks toward 1.0.
     * This preserves normal dynamics while preventing boosted samples from hard clipping.
     */
    private fun processSample(value: Float): Float {
        val magnitude = abs(value)
        if (magnitude <= SOFT_LIMIT_START) return value

        val excess = magnitude - SOFT_LIMIT_START
        val compressed = SOFT_LIMIT_START +
            (1f - SOFT_LIMIT_START) *
            (1f - exp((-excess / (1f - SOFT_LIMIT_START)).toDouble()).toFloat())
        return sign(value) * compressed.coerceAtMost(0.999f)
    }

    private companion object {
        const val SOFT_LIMIT_START = 0.92f
    }
}
