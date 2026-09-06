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
class AdminActivationViewModel @Inject constructor(
    private val api: ActivationApiService
) : ViewModel() {
    private var token: String? = null

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _requests = MutableStateFlow<List<ActivationRecord>>(emptyList())
    val requests: StateFlow<List<ActivationRecord>> = _requests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { api.adminLogin(AdminLoginRequest(email.trim(), password)) }
                .onSuccess { response ->
                    token = response.token
                    _isAuthenticated.value = true
                    loadRequests()
                }
                .onFailure {
                    _message.value = "Admin sign-in failed. Check your credentials."
                }
            _isLoading.value = false
        }
    }

    fun loadRequests() {
        val bearer = token?.let { "Bearer $it" } ?: return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { api.getAdminRequests(bearer) }
                .onSuccess { _requests.value = it.requests }
                .onFailure { handleAdminFailure() }
            _isLoading.value = false
        }
    }

    fun approve(id: Long, durationDays: Int) {
        if (durationDays !in setOf(30, 60, 90)) return
        val bearer = token?.let { "Bearer $it" } ?: return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { api.approveRequest(bearer, id, AdminDecisionRequest(durationDays)) }
                .onSuccess {
                    _message.value = "Activation approved for $durationDays days."
                    loadRequests()
                }
                .onFailure { handleAdminFailure() }
            _isLoading.value = false
        }
    }

    fun reject(id: Long) {
        val bearer = token?.let { "Bearer $it" } ?: return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { api.rejectRequest(bearer, id) }
                .onSuccess {
                    _message.value = "Activation request rejected."
                    loadRequests()
                }
                .onFailure { handleAdminFailure() }
            _isLoading.value = false
        }
    }

    fun revoke(id: Long) {
        val bearer = token?.let { "Bearer $it" } ?: return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { api.revokeRequest(bearer, id) }
                .onSuccess {
                    _message.value = "Activation revoked."
                    loadRequests()
                }
                .onFailure { handleAdminFailure() }
            _isLoading.value = false
        }
    }

    fun logout() {
        token = null
        _isAuthenticated.value = false
        _requests.value = emptyList()
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun handleAdminFailure() {
        _message.value = "Admin request failed. Please sign in again if your session expired."
    }
}
