package com.somphie.staff.core.network

import retrofit2.Response
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StaffApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    /** Devuelve Call porque el Authenticator de OkHttp renueva tokens en una llamada síncrona. */
    @POST("auth/refresh")
    fun refresh(@Body request: RefreshRequest): Call<TokenPair>

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshRequest): Response<Unit>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    @GET("me")
    suspend fun me(): StaffProfile

    @GET("staff/stations")
    suspend fun stations(): StationListResponse

    @GET("staff/stations/{stationId}/queue")
    suspend fun queue(
        @Path("stationId") stationId: String,
        @Query("status") statuses: String,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
    ): StationQueueResponse

    @POST("staff/order-items/{itemId}/start")
    suspend fun startItem(
        @Path("itemId") itemId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): ActionResult

    @POST("staff/order-items/{itemId}/ready")
    suspend fun markReady(
        @Path("itemId") itemId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): ActionResult

    @POST("staff/order-items/{itemId}/deliver")
    suspend fun deliver(
        @Path("itemId") itemId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): ActionResult

    @POST("staff/order-items/{itemId}/cancel")
    suspend fun cancel(
        @Path("itemId") itemId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CancelItemRequest,
    ): ActionResult

    @POST("staff/order-items/{itemId}/undo")
    suspend fun undo(
        @Path("itemId") itemId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): ActionResult
}
