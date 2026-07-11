package com.wavelength.music.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.roundToInt

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMillibel: Int,
    val minLevelMillibel: Int,
    val maxLevelMillibel: Int
)

enum class EqualizerMode { ADVANCED, SIMPLE }

/** Each curve is 5 gain fractions (-1..1) sampled evenly from the lowest to the highest band;
 * devices with a different band count get these interpolated across whatever bands they have. */
enum class EqualizerPreset(val label: String, val curve: List<Float>) {
    FLAT("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    ROCK("Rock", listOf(0.6f, 0.3f, -0.2f, 0.2f, 0.5f)),
    POP("Pop", listOf(-0.1f, 0.3f, 0.4f, 0.2f, -0.1f)),
    CLASSICAL("Classical", listOf(0.2f, 0.1f, -0.1f, 0.1f, 0.3f)),
    JAZZ("Jazz", listOf(0.3f, 0.2f, 0f, 0.2f, 0.3f)),
    BASS_BOOSTER("Bass Booster", listOf(0.9f, 0.6f, 0.1f, 0f, 0f)),
    VOCAL("Vocal", listOf(-0.2f, 0f, 0.5f, 0.4f, -0.1f))
}

/** Wraps the platform Equalizer/BassBoost AudioEffects, (re)attached to whichever audio session
 * the ExoPlayer instance in [PlaybackService] is currently using. Devices vary wildly in how many
 * bands they support (or whether these effects exist at all), so every call is defensive — a
 * device that doesn't support one of these just reports it as unsupported instead of crashing. */
@Singleton
class EqualizerController @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("wavelength_equalizer", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = 0

    private val _isSupported = MutableStateFlow(false)
    val isSupported: StateFlow<Boolean> = _isSupported.asStateFlow()

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _bands = MutableStateFlow<List<EqualizerBand>>(emptyList())
    val bands: StateFlow<List<EqualizerBand>> = _bands.asStateFlow()

    private val _bassBoostSupported = MutableStateFlow(false)
    val bassBoostSupported: StateFlow<Boolean> = _bassBoostSupported.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(prefs.getInt(KEY_BASS, 0))
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _volumeBoostSupported = MutableStateFlow(false)
    val volumeBoostSupported: StateFlow<Boolean> = _volumeBoostSupported.asStateFlow()

    private val _volumeBoostEnabled = MutableStateFlow(prefs.getBoolean(KEY_VOLUME_BOOST_ENABLED, false))
    val volumeBoostEnabled: StateFlow<Boolean> = _volumeBoostEnabled.asStateFlow()

    /** 100 = original volume, no boost; up to 400 = 4x amplitude (~+12dB). */
    private val _volumeBoostPercent = MutableStateFlow(prefs.getInt(KEY_VOLUME_BOOST, 100))
    val volumeBoostPercent: StateFlow<Int> = _volumeBoostPercent.asStateFlow()

    private val _mode = MutableStateFlow(
        runCatching {
            EqualizerMode.valueOf(prefs.getString(KEY_MODE, null) ?: EqualizerMode.ADVANCED.name)
        }.getOrDefault(EqualizerMode.ADVANCED)
    )
    val mode: StateFlow<EqualizerMode> = _mode.asStateFlow()

    fun setMode(mode: EqualizerMode) {
        _mode.value = mode
        prefs.edit { putString(KEY_MODE, mode.name) }
    }

    /** Called from [PlaybackService] whenever ExoPlayer's audio session id changes (including the
     * first time it becomes non-zero once playback actually starts). */
    fun onAudioSessionIdChanged(sessionId: Int) {
        if (sessionId == currentSessionId && equalizer != null && bassBoost != null && loudnessEnhancer != null) {
            return
        }
        release()
        currentSessionId = sessionId
        if (sessionId == 0) return

        runCatching {
            val eq = Equalizer(0, sessionId)
            val bandCount = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val loadedBands = (0 until bandCount).map { i ->
                val saved = prefs.getInt(bandKey(i), 0).toShort().coerceIn(range[0], range[1])
                eq.setBandLevel(i.toShort(), saved)
                EqualizerBand(
                    index = i,
                    centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000,
                    levelMillibel = saved.toInt(),
                    minLevelMillibel = range[0].toInt(),
                    maxLevelMillibel = range[1].toInt()
                )
            }
            eq.enabled = _enabled.value
            equalizer = eq
            _bands.value = loadedBands
            _isSupported.value = true
        }.onFailure {
            _isSupported.value = false
            _bands.value = emptyList()
        }

        runCatching {
            val bb = BassBoost(0, sessionId)
            _bassBoostSupported.value = bb.strengthSupported
            if (bb.strengthSupported) {
                bb.setStrength(_bassBoostStrength.value.toShort())
            }
            bb.enabled = _enabled.value
            bassBoost = bb
        }.onFailure {
            _bassBoostSupported.value = false
        }

        createLoudnessEnhancer(sessionId)
    }

    /** Broken out so it can be retried later by [ensureLoudnessEnhancer] — [LoudnessEnhancer]
     * construction can fail transiently even when [Equalizer]/[BassBoost] on the same session
     * succeed, and without a retry path that failure would silently disable the boost for the
     * rest of the playback session (every later toggle/slider change just no-ops against a null
     * effect). */
    private fun createLoudnessEnhancer(sessionId: Int) {
        runCatching {
            val le = LoudnessEnhancer(sessionId)
            le.setTargetGain(percentToMillibel(_volumeBoostPercent.value))
            le.enabled = _volumeBoostEnabled.value && _volumeBoostPercent.value > 100
            loudnessEnhancer = le
            _volumeBoostSupported.value = true
        }.onFailure {
            Log.w(TAG, "LoudnessEnhancer init failed for session $sessionId", it)
            _volumeBoostSupported.value = false
        }
    }

    /** Returns the current [LoudnessEnhancer], (re)constructing it first if it's missing but a
     * real audio session is active — see [createLoudnessEnhancer]'s doc for why this retry is
     * needed rather than trusting whatever got set during the last session change. */
    private fun ensureLoudnessEnhancer(): LoudnessEnhancer? {
        loudnessEnhancer?.let { return it }
        if (currentSessionId == 0) return null
        createLoudnessEnhancer(currentSessionId)
        return loudnessEnhancer
    }

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        prefs.edit { putBoolean(KEY_ENABLED, value) }
        runCatching { equalizer?.enabled = value }
        runCatching { bassBoost?.enabled = value }
    }

    fun setBandLevel(bandIndex: Int, levelMillibel: Int) {
        runCatching { equalizer?.setBandLevel(bandIndex.toShort(), levelMillibel.toShort()) }
        prefs.edit { putInt(bandKey(bandIndex), levelMillibel) }
        _bands.value = _bands.value.map {
            if (it.index == bandIndex) it.copy(levelMillibel = levelMillibel) else it
        }
    }

    fun setBassBoostStrength(strength: Int) {
        runCatching { bassBoost?.setStrength(strength.toShort()) }
        prefs.edit { putInt(KEY_BASS, strength) }
        _bassBoostStrength.value = strength
    }

    /** [percent] is 100 (no boost) to 400 (4x amplitude). The effect only actually runs when both
     * [setVolumeBoostEnabled] is on and percent is above 100, so it costs nothing otherwise. */
    fun setVolumeBoostPercent(percent: Int) {
        val clamped = percent.coerceIn(100, 400)
        runCatching {
            ensureLoudnessEnhancer()?.let { le ->
                le.setTargetGain(percentToMillibel(clamped))
                le.enabled = _volumeBoostEnabled.value && clamped > 100
            }
        }
        prefs.edit { putInt(KEY_VOLUME_BOOST, clamped) }
        _volumeBoostPercent.value = clamped
    }

    fun setVolumeBoostEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_VOLUME_BOOST_ENABLED, enabled) }
        _volumeBoostEnabled.value = enabled
        runCatching {
            ensureLoudnessEnhancer()?.enabled = enabled && _volumeBoostPercent.value > 100
        }
    }

    /** Treats [percent] as a linear amplitude ratio (100% = 1x = 0dB) and converts to the
     * millibels [LoudnessEnhancer.setTargetGain] expects. */
    private fun percentToMillibel(percent: Int): Int {
        val ratio = percent / 100f
        return (2000 * log10(ratio)).roundToInt()
    }

    /** Flattens every band and bass boost back to 0, without changing the enabled/mode state. */
    fun reset() {
        val currentBands = _bands.value
        prefs.edit {
            currentBands.forEach { band ->
                runCatching { equalizer?.setBandLevel(band.index.toShort(), 0) }
                putInt(bandKey(band.index), 0)
            }
            putInt(KEY_BASS, 0)
        }
        _bands.value = currentBands.map { it.copy(levelMillibel = 0) }
        runCatching { bassBoost?.setStrength(0) }
        _bassBoostStrength.value = 0
    }

    /** Maps [preset]'s 5-point curve proportionally across however many bands this device
     * actually reports, since band count varies by hardware. */
    fun applyPreset(preset: EqualizerPreset) {
        val currentBands = _bands.value
        if (currentBands.isEmpty()) return
        val curve = preset.curve
        val updated = currentBands.mapIndexed { position, band ->
            val fraction = sampleCurve(curve, if (currentBands.size > 1) {
                position.toFloat() / (currentBands.size - 1)
            } else {
                0f
            })
            val magnitude = if (fraction >= 0f) band.maxLevelMillibel else -band.minLevelMillibel
            val level = (fraction * magnitude).roundToInt().coerceIn(band.minLevelMillibel, band.maxLevelMillibel)
            runCatching { equalizer?.setBandLevel(band.index.toShort(), level.toShort()) }
            band.copy(levelMillibel = level)
        }
        prefs.edit { updated.forEach { putInt(bandKey(it.index), it.levelMillibel) } }
        _bands.value = updated

        val bassFraction = curve.first().coerceAtLeast(0f)
        val bassStrength = (bassFraction * 1000).roundToInt().coerceIn(0, 1000)
        runCatching { bassBoost?.setStrength(bassStrength.toShort()) }
        prefs.edit { putInt(KEY_BASS, bassStrength) }
        _bassBoostStrength.value = bassStrength
    }

    private fun sampleCurve(curve: List<Float>, position: Float): Float {
        val scaledPosition = position * (curve.size - 1)
        val lowerIndex = scaledPosition.toInt().coerceIn(0, curve.size - 1)
        val upperIndex = (lowerIndex + 1).coerceAtMost(curve.size - 1)
        val t = scaledPosition - lowerIndex
        return curve[lowerIndex] * (1 - t) + curve[upperIndex] * t
    }

    private fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
    }

    private fun bandKey(index: Int) = "band_$index"

    private companion object {
        const val TAG = "EqualizerController"
        const val KEY_ENABLED = "enabled"
        const val KEY_BASS = "bass_strength"
        const val KEY_MODE = "mode"
        const val KEY_VOLUME_BOOST = "volume_boost_percent"
        const val KEY_VOLUME_BOOST_ENABLED = "volume_boost_enabled"
    }
}
