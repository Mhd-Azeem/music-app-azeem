package com.wavelength.music.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMillibel: Int,
    val minLevelMillibel: Int,
    val maxLevelMillibel: Int
)

enum class EqualizerMode { ADVANCED, SIMPLE }

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
        if (sessionId == currentSessionId && equalizer != null) return
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

    private fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
    }

    private fun bandKey(index: Int) = "band_$index"

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_BASS = "bass_strength"
        const val KEY_MODE = "mode"
    }
}
