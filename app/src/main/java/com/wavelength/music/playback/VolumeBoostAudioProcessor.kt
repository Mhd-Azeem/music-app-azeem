package com.wavelength.music.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.math.tanh

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
     * Perceptual loudness curve similar to a media-player "volume over 100%" control.
     *
     * A plain multiply followed by a peak limiter barely changes already-mastered tracks because
     * their peaks are already close to full scale. This normalized tanh transfer raises quieter
     * samples strongly while smoothly compressing peaks, so RMS/perceived loudness actually rises
     * without the hard-clipping screech from the old LoudnessEnhancer path.
     */
    private fun processSample(value: Float): Float {
        val currentGain = gain
        if (currentGain <= 1.0001f) return value.coerceIn(-1f, 1f)

        // queueInput already multiplies by gain. Undo that here so the drive is applied once.
        val original = (value / currentGain).coerceIn(-1f, 1f)
        val drive = currentGain.toDouble()
        val normalized = tanh(original * drive) / tanh(drive)
        return normalized.toFloat().coerceIn(-0.999f, 0.999f)
    }
}
