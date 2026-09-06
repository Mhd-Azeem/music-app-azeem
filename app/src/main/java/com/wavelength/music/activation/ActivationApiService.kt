package com.wavelength.music.activation

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ActivationApiService {
    @POST("activation/request")
    suspend fun requestActivation(@Body request: ActivationRequestBody): ActivationRecord

    @GET("activation/status")
    suspend fun getActivationStatus(@Query("email") email: String): ActivationRecord
}
