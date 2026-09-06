package com.wavelength.music.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import org.json.JSONObject
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

    fun loginExisting(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.loginExisting(email)
                .onSuccess { record ->
                    _message.value = when (record.status) {
                        ActivationStatus.ACTIVE -> "Login successful. Your activation is active."
                        ActivationStatus.PENDING -> "Login successful. Your activation is still pending approval."
                        ActivationStatus.EXPIRED -> "Login successful. Your activation has expired."
                        ActivationStatus.REJECTED -> "Login successful. Your activation request was rejected."
                        ActivationStatus.REVOKED -> "Login successful. Your activation was revoked."
                        ActivationStatus.NOT_ACTIVATED -> null
                    }
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

    fun logout() {
        repository.logout()
        _message.value = "Signed out."
    }

    fun clearMessage() {
        _message.value = null
    }

    fun isAccessActive(): Boolean = repository.isAccessActive()
    fun getRemainingDays(): Long = repository.getRemainingDays()

    private fun activationError(error: Throwable): String = when (error) {
        is IllegalArgumentException -> error.message ?: "Enter a valid email address."
        is HttpException -> {
            val backendMessage = runCatching {
                val raw = error.response()?.errorBody()?.string().orEmpty()
                JSONObject(raw).optString("error").takeIf { it.isNotBlank() }
            }.getOrNull()
            when (error.code()) {
                404 -> backendMessage ?: "Activation service was not found. The Cloudflare activation backend has not been deployed at this app's backend URL."
                409 -> backendMessage ?: "This email is already activated on another device. Ask the admin to reset the linked device before logging in here."
                429 -> backendMessage ?: "Please wait a moment before submitting the same activation request again."
                500, 502, 503, 504 -> backendMessage ?: "Activation server is unavailable or not configured yet."
                else -> backendMessage ?: "Activation server returned HTTP ${error.code()}."
            }
        }
        else -> "Could not reach the activation server. Check that the Cloudflare Worker is deployed and that you have an internet connection."
    }
}
