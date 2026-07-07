package com.wavelength.music.ui.settings

import androidx.lifecycle.ViewModel
import com.wavelength.music.playback.EqualizerBand
import com.wavelength.music.playback.EqualizerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val equalizerController: EqualizerController
) : ViewModel() {

    val isSupported: StateFlow<Boolean> = equalizerController.isSupported
    val enabled: StateFlow<Boolean> = equalizerController.enabled
    val bands: StateFlow<List<EqualizerBand>> = equalizerController.bands
    val bassBoostSupported: StateFlow<Boolean> = equalizerController.bassBoostSupported
    val bassBoostStrength: StateFlow<Int> = equalizerController.bassBoostStrength

    fun setEnabled(value: Boolean) = equalizerController.setEnabled(value)

    fun setBandLevel(bandIndex: Int, levelMillibel: Int) =
        equalizerController.setBandLevel(bandIndex, levelMillibel)

    fun setBassBoostStrength(strength: Int) = equalizerController.setBassBoostStrength(strength)
}
