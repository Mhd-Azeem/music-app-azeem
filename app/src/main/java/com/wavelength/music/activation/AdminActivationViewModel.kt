package com.wavelength.music.activation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class AdminActivationViewModel @Inject constructor(
    private val api: ActivationApiService,
    @ApplicationContext context: Context
) : ViewModel() {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Persist only the signed admin session token — never the admin password.
    private var token: String? = prefs.getString(KEY_TOKEN, null)

    private val _isAuthenticated = MutableStateFlow(!token.isNullOrBlank())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _requests = MutableStateFlow<List<ActivationRecord>>(emptyList())
    val requests: StateFlow<List<ActivationRecord>> = _requests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        if (_isAuthenticated.value) loadRequests()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { api.adminLogin(AdminLoginRequest(email.trim(), password)) }
                .onSuccess { response ->
                    token = response.token
                    prefs.edit().putString(KEY_TOKEN, response.token).apply()
                    _isAuthenticated.value = true
                    _message.value = null
                    loadRequests()
                }
                .onFailure { error ->
                    _message.value = adminLoginError(error)
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
                .onFailure { error -> handleAdminFailure(error) }
            _isLoading.value = false
        }
    }

    fun approve(id: Long, durationDays: Int?) {
        if (durationDays != null && durationDays !in setOf(30, 60, 90)) return
        val bearer = token?.let { "Bearer $it" } ?: return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                    api.approveRequest(
                        bearer,
                        id,
                        AdminDecisionRequest(
                            durationDays = durationDays,
                            lifetime = durationDays == null
                        )
                    )
                }
                .onSuccess {
                    _message.value = if (durationDays == null) {
                        "Lifetime activation approved."
                    } else {
                        "Activation approved for $durationDays days."
                    }
                    loadRequests()
                }
                .onFailure { error -> handleAdminFailure(error) }
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
                .onFailure { error -> handleAdminFailure(error) }
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
                .onFailure { error -> handleAdminFailure(error) }
            _isLoading.value = false
        }
    }

    fun resetDevice(id: Long) {
        val bearer = token?.let { "Bearer $it" } ?: return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { api.resetDevice(bearer, id) }
                .onSuccess {
                    _message.value = "Linked device reset. The next phone that logs in with this email will be linked."
                    loadRequests()
                }
                .onFailure { error -> handleAdminFailure(error) }
            _isLoading.value = false
        }
    }

    fun deleteRevoked(id: Long) {
        val bearer = token?.let { "Bearer $it" } ?: return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { api.deleteRevokedRequest(bearer, id) }
                .onSuccess {
                    _message.value = "Revoked email removed from activation history."
                    _requests.value = _requests.value.filterNot { it.id == id }
                }
                .onFailure { error -> handleAdminFailure(error) }
            _isLoading.value = false
        }
    }

    fun logout() {
        token = null
        prefs.edit().remove(KEY_TOKEN).apply()
        _isAuthenticated.value = false
        _requests.value = emptyList()
        _message.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun adminLoginError(error: Throwable): String = when (error) {
        is HttpException -> when (error.code()) {
            401 -> "Admin email or activation-server password is incorrect. Do not use your Gmail password."
            404 -> "Admin login endpoint was not found. The activation backend has not been deployed at this app's backend URL."
            503 -> "Activation backend admin authentication is not configured. Set ADMIN_EMAIL, ADMIN_PASSWORD and SESSION_SECRET on Cloudflare."
            else -> "Activation backend returned HTTP ${error.code()}."
        }
        else -> "Could not reach the activation backend. Check that the Cloudflare Worker is deployed and ACTIVATION_BASE_URL is correct."
    }

    private fun handleAdminFailure(error: Throwable) {
        _message.value = when (error) {
            is HttpException -> when (error.code()) {
                401 -> "The saved admin session is no longer accepted by the server. Tap Sign out, then sign in again."
                404 -> "Activation backend endpoint was not found."
                else -> "Admin request failed with HTTP ${error.code()}."
            }
            else -> "Could not reach the activation backend."
        }
    }

    private companion object {
        const val PREFS_NAME = "activation_admin_session"
        const val KEY_TOKEN = "admin_session_token"
    }
}
