package com.wavelength.music.ui.components

import androidx.lifecycle.ViewModel
import com.wavelength.music.data.network.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
}
