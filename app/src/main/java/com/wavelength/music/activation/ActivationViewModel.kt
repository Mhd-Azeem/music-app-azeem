package com.wavelength.music.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
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
                    _message.value = activationError(error)
                }
            _isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.checkActivationStatus()
                .onFailure { error ->
                    _message.value = activationError(error)
                }
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun isAccessActive(): Boolean = repository.isAccessActive()
    fun getRemainingDays(): Long = repository.getRemainingDays()

    private fun activationError(error: Throwable): String = when (error) {
        is IllegalArgumentException -> error.message ?: "Enter a valid email address."
        is HttpException -> when (error.code()) {
            404 -> "Activation service was not found. The Cloudflare activation backend has not been deployed at this app's backend URL."
            429 -> "Please wait a moment before submitting the same activation request again."
            500, 502, 503, 504 -> "Activation server is unavailable or not configured yet."
            else -> "Activation server returned HTTP ${error.code()}."
        }
        else -> "Could not reach the activation server. Check that the Cloudflare Worker is deployed and that you have an internet connection."
    }
}
