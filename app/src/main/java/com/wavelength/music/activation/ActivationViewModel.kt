package com.wavelength.music.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val repository: ActivationRepository
) : ViewModel() {
    val activation: StateFlow<ActivationRecord> = repository.activation

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun requestActivation(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.requestActivation(email)
                .onSuccess {
                    _message.value = "Activation request submitted. Your account is waiting for approval."
                }
                .onFailure { error ->
                    _message.value = error.message ?: "Could not submit activation request. Please try again."
                }
            _isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.checkActivationStatus()
                .onFailure {
                    _message.value = "Could not verify activation right now. Cached access is used only within the offline grace period."
                }
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun isAccessActive(): Boolean = repository.isAccessActive()
    fun getRemainingDays(): Long = repository.getRemainingDays()
}
