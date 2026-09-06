package com.wavelength.music.activation

import com.squareup.moshi.JsonClass

enum class ActivationStatus {
    NOT_ACTIVATED,
    PENDING,
    ACTIVE,
    EXPIRED,
    REJECTED,
    REVOKED
}

@JsonClass(generateAdapter = true)
data class ActivationRecord(
    val email: String = "",
    val status: ActivationStatus = ActivationStatus.NOT_ACTIVATED,
    val requestedAt: Long? = null,
    val approvedAt: Long? = null,
    val activationStartDate: Long? = null,
    val expirationDate: Long? = null,
    val durationDays: Int? = null
)

@JsonClass(generateAdapter = true)
data class ActivationRequestBody(val email: String)

@JsonClass(generateAdapter = true)
data class ActivationResponse(
    val activation: ActivationRecord,
    val serverTimestamp: Long
)
