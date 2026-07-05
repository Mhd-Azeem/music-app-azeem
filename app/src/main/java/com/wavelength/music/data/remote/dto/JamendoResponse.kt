package com.wavelength.music.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JamendoResponse<T>(
    @Json(name = "headers") val headers: JamendoHeaders?,
    @Json(name = "results") val results: List<T> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JamendoHeaders(
    @Json(name = "status") val status: String?,
    @Json(name = "code") val code: Int?,
    @Json(name = "error_message") val errorMessage: String?,
    @Json(name = "results_count") val resultsCount: Int?
)
