package com.wavelength.music.activation

import android.content.Context
import android.util.Patterns
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface ActivationRepository {
    val activation: StateFlow<ActivationRecord>

    suspend fun requestActivation(email: String): Result<ActivationRecord>
    suspend fun loginExisting(email: String): Result<ActivationRecord>
    suspend fun checkActivationStatus(): Result<ActivationRecord>
    suspend fun refreshStatus(): Result<ActivationRecord>
    fun logout()
    fun isAccessActive(): Boolean
    fun getRemainingDays(): Long
}

@Singleton
class ActivationRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val api: ActivationApiService,
    moshi: Moshi
) : ActivationRepository {

    private val adapter = moshi.adapter(ActivationRecord::class.java)
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

    // The cached record is the user's local activation identity/session. It survives app process
    // death and clearing the app from Recents. It is removed only by explicit logout (or app data
    // removal/uninstall), while access itself still follows server status and expiry rules.
    private val installationId: String = loadOrCreateInstallationId()

    private val _activation = MutableStateFlow(loadCachedRecord())
    override val activation: StateFlow<ActivationRecord> = _activation.asStateFlow()

    override suspend fun requestActivation(email: String): Result<ActivationRecord> {
        val normalized = email.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(normalized).matches()) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }

        return runCatching {
            val response = api.requestActivation(ActivationRequestBody(normalized, installationId))
            persistVerified(response)
            response.activation
        }
    }

    override suspend fun loginExisting(email: String): Result<ActivationRecord> {
        val normalized = email.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(normalized).matches()) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }

        return runCatching {
            val response = api.getActivationStatus(normalized, installationId)
            if (response.activation.status == ActivationStatus.NOT_ACTIVATED) {
                throw IllegalArgumentException("This email is not registered. Use Register to request access.")
            }
            persistVerified(response)
            response.activation
        }
    }

    override suspend fun checkActivationStatus(): Result<ActivationRecord> {
        val email = _activation.value.email
        if (email.isBlank()) return Result.success(_activation.value)
        return refreshStatus()
    }

    override suspend fun refreshStatus(): Result<ActivationRecord> {
        val email = _activation.value.email
        if (email.isBlank()) return Result.success(_activation.value)

        return try {
            val response = api.getActivationStatus(email, installationId)
            persistVerified(response)
            Result.success(response.activation)
        } catch (e: Exception) {
            val cached = effectiveCachedRecord()
            _activation.value = cached
            if (canUseOfflineCache(cached)) Result.success(cached)
            else Result.failure(IOException("Activation status could not be verified.", e))
        }
    }

    override fun logout() {
        // Keep the installation ID. Logging out must not make this phone look like a new device,
        // otherwise one user could bypass the one-device rule simply by signing out and back in.
        prefs.edit()
            .remove(KEY_RECORD)
            .remove(KEY_LAST_VERIFIED_SERVER_TIME)
            .remove(KEY_LAST_VERIFIED_DEVICE_TIME)
            .apply()
        _activation.value = ActivationRecord()
    }

    override fun isAccessActive(): Boolean {
        val record = effectiveCachedRecord()
        return record.status == ActivationStatus.ACTIVE &&
            (record.expirationDate == null || record.expirationDate > System.currentTimeMillis()) &&
            canUseOfflineCache(record)
    }

    override fun getRemainingDays(): Long {
        val expiration = effectiveCachedRecord().expirationDate ?: return Long.MAX_VALUE
        val remaining = expiration - System.currentTimeMillis()
        return if (remaining <= 0L) 0L else remaining / DAY_MS
    }

    private fun persistVerified(response: ActivationResponse) {
        _activation.value = normalizeExpiration(response.activation)
        prefs.edit()
            .putString(KEY_RECORD, adapter.toJson(_activation.value))
            .putLong(KEY_LAST_VERIFIED_SERVER_TIME, response.serverTimestamp)
            .putLong(KEY_LAST_VERIFIED_DEVICE_TIME, System.currentTimeMillis())
            .apply()
    }

    private fun loadOrCreateInstallationId(): String {
        val existing = prefs.getString(KEY_INSTALLATION_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALLATION_ID, created).commit()
        return created
    }

    private fun loadCachedRecord(): ActivationRecord {
        val raw = prefs.getString(KEY_RECORD, null) ?: return ActivationRecord()
        return runCatching { adapter.fromJson(raw) ?: ActivationRecord() }
            .getOrElse { ActivationRecord() }
            .let(::normalizeExpiration)
    }

    private fun effectiveCachedRecord(): ActivationRecord {
        val normalized = normalizeExpiration(_activation.value)
        if (normalized != _activation.value) _activation.value = normalized
        return normalized
    }

    private fun normalizeExpiration(record: ActivationRecord): ActivationRecord {
        val expiration = record.expirationDate
        return if (record.status == ActivationStatus.ACTIVE && expiration != null && expiration <= System.currentTimeMillis()) {
            record.copy(status = ActivationStatus.EXPIRED)
        } else record
    }

    private fun canUseOfflineCache(record: ActivationRecord): Boolean {
        if (record.status != ActivationStatus.ACTIVE) return false
        val lastServer = prefs.getLong(KEY_LAST_VERIFIED_SERVER_TIME, 0L)
        val lastDevice = prefs.getLong(KEY_LAST_VERIFIED_DEVICE_TIME, 0L)
        if (lastServer <= 0L || lastDevice <= 0L) return false

        val now = System.currentTimeMillis()
        if (now + CLOCK_ROLLBACK_TOLERANCE_MS < lastDevice) return false
        return now - lastDevice <= OFFLINE_GRACE_MS &&
            (record.expirationDate == null || record.expirationDate > now)
    }

    private companion object {
        const val PREFS_NAME = "activation_secure_cache"
        const val KEY_RECORD = "activation_record"
        const val KEY_INSTALLATION_ID = "installation_id"
        const val KEY_LAST_VERIFIED_SERVER_TIME = "last_verified_server_time"
        const val KEY_LAST_VERIFIED_DEVICE_TIME = "last_verified_device_time"
        const val DAY_MS = 24L * 60L * 60L * 1000L
        const val OFFLINE_GRACE_MS = 7L * DAY_MS
        const val CLOCK_ROLLBACK_TOLERANCE_MS = 5L * 60L * 1000L
    }
}
