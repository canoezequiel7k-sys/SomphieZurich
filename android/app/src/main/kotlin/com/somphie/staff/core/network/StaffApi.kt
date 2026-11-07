package com.somphie.staff.core.network

import retrofit2.Response
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
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

    // Registra un pago en caja (Efectivo / Tarjeta simulada)
    @POST("staff/sessions/{sessionId}/payments")
    suspend fun createPayment(
        @Path("sessionId") sessionId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: StaffPaymentRequest
    ): Response<StaffPaymentResponse>

    // Reembolsa / Anula un pago registrado
    @POST("staff/payments/{paymentId}/refund")
    suspend fun refundPayment(
        @Path("paymentId") paymentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: StaffRefundRequest
    ): Response<StaffPaymentResponse>

    // Cierra manualmente o de forma forzada una sesión de mesa
    @POST("staff/sessions/{sessionId}/close")
    suspend fun closeSession(
        @Path("sessionId") sessionId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CloseSessionRequest
    ): Response<StaffSession>

    // Aplica un ajuste o descuento a la sesión de mesa
    @POST("staff/sessions/{sessionId}/adjustments")
    suspend fun createAdjustment(
        @Path("sessionId") sessionId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: StaffAdjustmentRequest
    ): Response<StaffAdjustmentResponse>

    // Anula un ajuste o descuento previamente aplicado
    @POST("staff/adjustments/{adjustmentId}/void")
    suspend fun voidAdjustment(
        @Path("adjustmentId") adjustmentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: StaffVoidAdjustmentRequest
    ): Response<StaffAdjustmentResponse>

    // Crear un pedido a nombre del cliente para una sesión de mesa
    @POST("staff/sessions/{sessionId}/orders")
    suspend fun createStaffOrder(
        @Path("sessionId") sessionId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateStaffOrderRequest
    ): Response<StaffOrder>

    // Cambia la disponibilidad operativa de un producto
    @PATCH("staff/products/{productId}/availability")
    suspend fun updateProductAvailability(
        @Path("productId") productId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: ProductAvailabilityRequest
    ): Response<ActionResult>

    // Cambia la disponibilidad operativa de una opción
    @PATCH("staff/options/{optionId}/availability")
    suspend fun updateOptionAvailability(
        @Path("optionId") optionId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: OptionAvailabilityRequest
    ): Response<ActionResult>

    // --- Endpoints de Pedidos ---
    @GET("staff/orders")
    suspend fun getOrders(
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): StaffOrderPage

    @GET("staff/orders/{orderId}")
    suspend fun getOrder(
        @Path("orderId") orderId: String
    ): StaffOrder

    @POST("staff/orders/{orderId}/accept")
    suspend fun acceptOrder(
        @Path("orderId") orderId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: AcceptOrderRequest? = null
    ): StaffOrder

    @POST("staff/orders/{orderId}/reject")
    suspend fun rejectOrder(
        @Path("orderId") orderId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: RejectOrderRequest
    ): StaffOrder

    // --- Endpoints de Mesas ---
    @GET("staff/tables")
    suspend fun getTables(): StaffTableBoard

    @GET("staff/sessions/{sessionId}")
    suspend fun getSessionDetail(
        @Path("sessionId") sessionId: String
    ): StaffSessionDetail
}
