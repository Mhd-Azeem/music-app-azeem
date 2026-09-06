package com.wavelength.music.activation

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ActivationApiService {
    @POST("activation/request")
    suspend fun requestActivation(@Body request: ActivationRequestBody): ActivationResponse

    @GET("activation/status")
    suspend fun getActivationStatus(@Query("email") email: String): ActivationResponse

    @POST("admin/login")
    suspend fun adminLogin(@Body request: AdminLoginRequest): AdminLoginResponse

    @GET("admin/requests")
    suspend fun getAdminRequests(
        @Header("Authorization") authorization: String
    ): AdminActivationListResponse

    @POST("admin/requests/{id}/approve")
    suspend fun approveRequest(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body request: AdminDecisionRequest
    ): ActivationResponse

    @POST("admin/requests/{id}/reject")
    suspend fun rejectRequest(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body request: AdminDecisionRequest = AdminDecisionRequest()
    ): ActivationResponse

    @POST("admin/requests/{id}/revoke")
    suspend fun revokeRequest(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body request: AdminDecisionRequest = AdminDecisionRequest()
    ): ActivationResponse
}
