package com.somphie.staff.feature.orders.ui

import com.somphie.staff.core.network.AcceptOrderRequest
import com.somphie.staff.core.network.CancelItemRequest
import com.somphie.staff.core.network.ChangePasswordRequest
import com.somphie.staff.core.network.LoginRequest
import com.somphie.staff.core.network.RefreshRequest
import com.somphie.staff.core.network.RejectOrderRequest
import com.somphie.staff.core.network.StaffApi
import com.somphie.staff.core.network.StaffOrder
import com.somphie.staff.core.network.StaffOrderPage
import com.somphie.staff.core.network.StaffTable
import com.somphie.staff.core.network.StaffTableBoard
import java.io.IOException

class FakeStaffApi : StaffApi {
    var ordersPage = StaffOrderPage(items = emptyList())
    var orderDetail: StaffOrder? = null
    var tablesList: List<StaffTable> = emptyList()
    var shouldFail = false
    var failureToThrow: Throwable? = null
    var sessionDetail: com.somphie.staff.core.network.StaffSessionDetail? = null
    var lastAcceptedOrderId: String? = null
    var lastRejectedOrderId: String? = null
    var lastRejectReasonCode: String? = null

    override suspend fun getOrders(status: String?, limit: Int, cursor: String?): StaffOrderPage {
        if (shouldFail) throw IOException("Error de red simulado")
        return ordersPage
    }

    override suspend fun getOrder(orderId: String): StaffOrder {
        if (shouldFail) throw IOException("Error al obtener detalle")
        return orderDetail ?: throw IllegalStateException("Order not found")
    }

    override suspend fun acceptOrder(orderId: String, idempotencyKey: String, request: AcceptOrderRequest?): StaffOrder {
        if (shouldFail) throw IOException("Error al aceptar pedido")
        lastAcceptedOrderId = orderId
        return orderDetail ?: throw IllegalStateException("Order not found")
    }

    override suspend fun rejectOrder(orderId: String, idempotencyKey: String, request: RejectOrderRequest): StaffOrder {
        if (shouldFail) throw IOException("Error al rechazar pedido")
        lastRejectedOrderId = orderId
        lastRejectReasonCode = request.reasonCode
        return orderDetail ?: throw IllegalStateException("Order not found")
    }

    override suspend fun getTables(): StaffTableBoard {
        if (shouldFail) throw IOException("Error al obtener mesas")
        return StaffTableBoard(items = tablesList)
    }

    override suspend fun getSessionDetail(sessionId: String): com.somphie.staff.core.network.StaffSessionDetail {
        if (shouldFail) throw IOException("Error al obtener detalle de mesa")
        return sessionDetail ?: throw IllegalStateException("Session not found")
    }

    override suspend fun login(request: LoginRequest) = error("Unused")
    override fun refresh(request: RefreshRequest) = error("Unused")
    override suspend fun logout(request: RefreshRequest) = error("Unused")
    override suspend fun changePassword(request: ChangePasswordRequest) = error("Unused")
    override suspend fun me() = error("Unused")
    override suspend fun stations() = error("Unused")
    override suspend fun queue(stationId: String, statuses: String, limit: Int, cursor: String?) = error("Unused")
    override suspend fun startItem(itemId: String, idempotencyKey: String) = error("Unused")
    override suspend fun markReady(itemId: String, idempotencyKey: String) = error("Unused")
    override suspend fun deliver(itemId: String, idempotencyKey: String) = error("Unused")
    override suspend fun cancel(itemId: String, idempotencyKey: String, request: CancelItemRequest) = error("Unused")
    override suspend fun undo(itemId: String, idempotencyKey: String) = error("Unused")

    override suspend fun createPayment(
        sessionId: String,
        idempotencyKey: String,
        request: com.somphie.staff.core.network.CreateCashPaymentRequest
    ): com.somphie.staff.core.network.PaymentResult {
        failureToThrow?.let { throw it }
        if (shouldFail) throw RuntimeException("Error en pago")
        val currentBill = sessionDetail?.bill ?: com.somphie.staff.core.network.Bill(currency = "ARS", chargesMinor = 0, balanceMinor = 0)
        val payment = com.somphie.staff.core.network.Payment(
            id = "pay-1",
            method = "CASH_COUNTER",
            status = "SUCCEEDED",
            amountMinor = request.amountMinor,
            tipMinor = request.tipMinor,
            payerLabel = request.payerLabel,
            createdAt = "12:30"
        )
        return com.somphie.staff.core.network.PaymentResult(
            payment = payment,
            bill = currentBill
        )
    }

    override suspend fun voidPayment(
        paymentId: String,
        idempotencyKey: String,
        request: com.somphie.staff.core.network.ReasonRequest
    ): com.somphie.staff.core.network.PaymentResult {
        failureToThrow?.let { throw it }
        if (shouldFail) throw RuntimeException("Error en void payment")
        val currentBill = sessionDetail?.bill ?: com.somphie.staff.core.network.Bill(currency = "ARS", chargesMinor = 0, balanceMinor = 0)
        val payment = com.somphie.staff.core.network.Payment(
            id = paymentId,
            method = "CASH_COUNTER",
            status = "VOIDED",
            amountMinor = 1000,
            tipMinor = 0,
            createdAt = "12:35"
        )
        return com.somphie.staff.core.network.PaymentResult(
            payment = payment,
            bill = currentBill
        )
    }

    override suspend fun refundPayment(
        sessionId: String,
        idempotencyKey: String,
        request: com.somphie.staff.core.network.CreateRefundRequest
    ): com.somphie.staff.core.network.PaymentResult {
        failureToThrow?.let { throw it }
        if (shouldFail) throw RuntimeException("Error en refund")
        val currentBill = sessionDetail?.bill ?: com.somphie.staff.core.network.Bill(currency = "ARS", chargesMinor = 0, balanceMinor = 0)
        val payment = com.somphie.staff.core.network.Payment(
            id = "ref-1",
            kind = "REFUND",
            method = "CASH_COUNTER",
            status = "SUCCEEDED",
            amountMinor = request.amountMinor,
            tipMinor = 0,
            createdAt = "12:35"
        )
        return com.somphie.staff.core.network.PaymentResult(
            payment = payment,
            bill = currentBill
        )
    }

    override suspend fun closeSession(
        sessionId: String
    ): com.somphie.staff.core.network.StaffSession {
        if (shouldFail) throw RuntimeException("Error en close")
        return com.somphie.staff.core.network.StaffSession(
            id = sessionId,
            status = "CLOSED",
            tableLabel = "Mesa 4",
            openedAt = "12:00",
            closedAt = "12:40"
        )
    }

    override suspend fun forceCloseSession(
        sessionId: String,
        idempotencyKey: String,
        request: com.somphie.staff.core.network.ReasonRequest
    ): com.somphie.staff.core.network.StaffSession {
        if (shouldFail) throw RuntimeException("Error en force-close")
        return com.somphie.staff.core.network.StaffSession(
            id = sessionId,
            status = "CLOSED",
            tableLabel = "Mesa 4",
            openedAt = "12:00",
            closedAt = "12:40"
        )
    }

    override suspend fun createAdjustment(
        sessionId: String,
        idempotencyKey: String,
        request: com.somphie.staff.core.network.CreateAdjustmentRequest
    ): com.somphie.staff.core.network.StaffAdjustment {
        if (shouldFail) throw RuntimeException("Error en adjustment")
        return com.somphie.staff.core.network.StaffAdjustment(
            id = "adj-1",
            kind = request.kind,
            amountMinor = request.amountMinor ?: 100L,
            percentBp = request.percentBp,
            reasonCode = request.reasonCode,
            reasonText = request.reasonText,
            status = "APPLIED",
            appliedAt = "12:35"
        )
    }

    override suspend fun voidAdjustment(
        adjustmentId: String,
        idempotencyKey: String,
        request: com.somphie.staff.core.network.ReasonRequest
    ): com.somphie.staff.core.network.StaffAdjustment {
        if (shouldFail) throw RuntimeException("Error en void adjustment")
        return com.somphie.staff.core.network.StaffAdjustment(
            id = adjustmentId,
            kind = "DISCOUNT_PERCENT",
            amountMinor = 100L,
            percentBp = 1000,
            reasonCode = "SERVICE_ISSUE",
            status = "VOIDED",
            appliedAt = "12:35",
            voidedAt = "12:40",
            voidReason = request.reason
        )
    }

    override suspend fun createStaffOrder(
        sessionId: String,
        idempotencyKey: String,
        request: com.somphie.staff.core.network.CreateStaffOrderRequest
    ): retrofit2.Response<StaffOrder> {
        if (shouldFail) return retrofit2.Response.error(400, okhttp3.ResponseBody.create(null, "Error"))
        val newOrder = StaffOrder(
            id = "ord-new",
            code = "ORD-999",
            sessionId = sessionId,
            tableLabel = "Mesa 4",
            status = "ACCEPTED",
            createdAt = "12:35",
            totalMinor = 640,
            items = emptyList()
        )
        return retrofit2.Response.success(newOrder)
    }

    override suspend fun updateProductAvailability(
        productId: String,
        idempotencyKey: String,
        request: com.somphie.staff.core.network.ProductAvailabilityRequest
    ): retrofit2.Response<com.somphie.staff.core.network.ActionResult> {
        if (shouldFail) return retrofit2.Response.error(400, okhttp3.ResponseBody.create(null, "Error"))
        return retrofit2.Response.success(
            com.somphie.staff.core.network.ActionResult(
                id = productId,
                status = "UPDATED",
                version = 1L,
                items = emptyList()
            )
        )
    }

    override suspend fun updateOptionAvailability(
        optionId: String,
        idempotencyKey: String,
        request: com.somphie.staff.core.network.OptionAvailabilityRequest
    ): retrofit2.Response<com.somphie.staff.core.network.ActionResult> {
        if (shouldFail) return retrofit2.Response.error(400, okhttp3.ResponseBody.create(null, "Error"))
        return retrofit2.Response.success(
            com.somphie.staff.core.network.ActionResult(
                id = optionId,
                status = "UPDATED",
                version = 1L,
                items = emptyList()
            )
        )
    }
}
