package com.wavelength.music.playback

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** Mirrors the device's actual STREAM_MUSIC volume (the same one the hardware volume rocker
 * controls) rather than [PlayerController]'s software mix gain, so the Now Playing volume slider
 * can optionally stay in sync with — and control — system volume instead of an app-only level.
 * Registers a [ContentObserver] on the system settings URI (there's no public, non-hidden intent
 * for "volume changed" specifically) so the slider also updates live when the hardware buttons are
 * pressed while Now Playing is open. */
@Singleton
class SystemVolumeController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

    private val _volume = MutableStateFlow(currentFraction())
    val volume: StateFlow<Float> = _volume.asStateFlow()

    init {
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    _volume.value = currentFraction()
                }
            }
        )
    }

    fun setVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (clamped * maxVolume).roundToInt(), 0)
        _volume.value = clamped
    }

    private fun currentFraction(): Float =
        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
}
