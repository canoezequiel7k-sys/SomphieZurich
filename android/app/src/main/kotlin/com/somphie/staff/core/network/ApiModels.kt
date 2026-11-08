package com.somphie.staff.core.network

import kotlinx.serialization.Serializable

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSec: Long,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSec: Long,
    val user: StaffProfile,
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class StaffProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val roles: List<String>,
    val permissions: List<String>,
    val stations: List<Station>,
    val mustChangePassword: Boolean,
)

@Serializable
data class Station(val id: String, val code: String, val name: String)

@Serializable
data class StationListResponse(val items: List<Station>)

@Serializable
data class QueueOption(
    val groupId: String,
    val optionId: String,
    val groupName: String,
    val optionName: String,
    val priceDeltaMinor: Long? = null,
)

@Serializable
data class StationQueueItem(
    val itemId: String,
    val orderId: String,
    val orderCode: String,
    val tableLabel: String,
    val productName: String,
    val quantity: Int,
    val note: String? = null,
    val status: String,
    val queuedAt: String? = null,
    val delayed: Boolean? = null,
    val optionsSummary: String = "",
    val options: List<QueueOption> = emptyList(),
)

@Serializable
data class StationQueueResponse(
    val items: List<StationQueueItem>,
    val nextCursor: String? = null,
)

@Serializable
data class CancelItemRequest(val reason: String)

@Serializable
data class ActionResult(
    val id: String,
    val status: String,
    val version: Long,
    val items: List<ActionItem>,
)

@Serializable
data class ActionItem(val id: String, val status: String)

@Serializable
data class ApiProblem(
    val type: String = "",
    val title: String = "",
    val status: Int = 0,
    val code: String = "UNKNOWN_ERROR",
    val detail: String? = null,
)

// --- Pedidos Staff ---
@Serializable
data class StaffOrderPage(
    val items: List<StaffOrder>,
    val nextCursor: String? = null
)

@Serializable
data class StaffOrder(
    val id: String,
    val code: String,
    val sessionId: String,
    val tableLabel: String,
    val status: String, // PENDING_REVIEW, ACCEPTED, REJECTED, CANCELLED, EXPIRED
    val createdAt: String,
    val items: List<StaffOrderItem>,
    val totalMinor: Long,
    val notes: String? = null,
    val decidedBy: OrderDecidedBy? = null,
    val decidedAt: String? = null
)

@Serializable
data class StaffOrderItem(
    val id: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPriceMinor: Long,
    val totalMinor: Long,
    val status: String,
    val note: String? = null,
    val optionsSummary: String = "",
    val productCurrentlyAvailable: Boolean = true
)

@Serializable
data class OrderDecidedBy(
    val userId: String? = null,
    val displayName: String
)

@Serializable
data class AcceptOrderRequest(
    val excludedItems: List<ExcludedItem>? = null
)

@Serializable
data class ExcludedItem(
    val itemId: String,
    val reason: String
)

@Serializable
data class RejectOrderRequest(
    val reasonCode: String,
    val reasonText: String? = null
)

// --- Mesas Staff ---
@Serializable
data class StaffTableBoard(
    val items: List<StaffTable>
)

@Serializable
data class StaffTable(
    val tableId: String,
    val label: String,
    val displayStatus: String, // FREE, OCCUPIED, PENDING_ORDERS, IN_SERVICE, AWAITING_PAYMENT, READY_TO_CLOSE
    val sessionId: String? = null,
    val openedAt: String? = null,
    val pendingOrdersCount: Int = 0,
    val balanceMinor: Long? = null,
    val waiterCallActive: Boolean = false
)

// --- Detalle de Sesión / Mesa ---
@Serializable
data class StaffSession(
    val id: String,
    val status: String,
    val tableLabel: String,
    val openedAt: String,
    val closedAt: String? = null,
    val readOnly: Boolean = false,
    val tableId: String? = null,
    val closeReason: String? = null
)

@Serializable
data class Participant(
    val id: String,
    val displayName: String,
    val role: String, // HOST, MEMBER
    val revoked: Boolean? = false
)

@Serializable
data class BillLine(
    val orderCode: String,
    val productName: String,
    val quantity: Int,
    val unitPriceMinor: Long,
    val lineTotalMinor: Long,
)

@Serializable
data class BillAdjustment(
    val id: String,
    val kind: String,
    val label: String,
    val amountMinor: Long,
)

@Serializable
data class PendingOrderSummary(
    val orderCode: String,
    val totalMinor: Long,
)

@Serializable
data class Payment(
    val id: String,
    val kind: String = "PAYMENT", // PAYMENT, REFUND
    val method: String,           // CASH_COUNTER, SIMULATED_ONLINE
    val status: String,           // SUCCEEDED, FAILED, VOIDED
    val amountMinor: Long,
    val tipMinor: Long = 0L,
    val payerName: String? = null,
    val payerLabel: String? = null,
    val createdAt: String,
)

@Serializable
data class Bill(
    val currency: String = "ARS",
    val chargesMinor: Long = 0,
    val adjustmentsMinor: Long = 0,
    val paidMinor: Long = 0,
    val balanceMinor: Long = 0,
    val tipsMinor: Long = 0,
    val lines: List<BillLine> = emptyList(),
    val adjustments: List<BillAdjustment> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val pendingOrdersNotIncluded: List<PendingOrderSummary> = emptyList(),
)

@Serializable
data class StaffSessionDetail(
    val session: StaffSession,
    val participants: List<Participant> = emptyList(),
    val orders: List<StaffOrder> = emptyList(),
    val bill: Bill
)

// --- Pagos, Caja y Cierre Staff (Alineados con OpenAPI) ---
@Serializable
data class CreateCashPaymentRequest(
    val amountMinor: Long,
    val tipMinor: Long = 0L,
    val payerLabel: String? = null,
)

@Serializable
data class PaymentResult(
    val payment: Payment,
    val bill: Bill,
)

@Serializable
data class ReasonRequest(
    val reason: String,
)

@Serializable
data class CreateRefundRequest(
    val amountMinor: Long,
    val reason: String,
)

// --- Ajustes, Descuentos y Cortesías Staff ---
object AdjustmentKind {
    const val DISCOUNT_PERCENT = "DISCOUNT_PERCENT"
    const val DISCOUNT_AMOUNT = "DISCOUNT_AMOUNT"
    const val COURTESY_ITEM = "COURTESY_ITEM"
    const val MANUAL_ADJUSTMENT = "MANUAL_ADJUSTMENT"
}

enum class AdjustmentReasonCode(val code: String, val label: String) {
    COURTESY_HOUSE("COURTESY_HOUSE", "Cortesía de la casa"),
    SERVICE_ISSUE("SERVICE_ISSUE", "Problema de servicio"),
    PROMOTION("PROMOTION", "Promoción comercial"),
    STAFF("STAFF", "Consumo de personal"),
    ERROR_CORRECTION("ERROR_CORRECTION", "Corrección de error"),
    OTHER("OTHER", "Otro motivo");

    companion object {
        fun fromCode(code: String): AdjustmentReasonCode =
            entries.firstOrNull { it.code == code } ?: OTHER
    }
}

@Serializable
data class CreateAdjustmentRequest(
    val kind: String,
    val percentBp: Int? = null,
    val amountMinor: Long? = null,
    val orderItemId: String? = null,
    val reasonCode: String,
    val reasonText: String? = null,
)

@Serializable
data class StaffAdjustment(
    val id: String,
    val kind: String,
    val status: String, // APPLIED, VOIDED
    val amountMinor: Long,
    val percentBp: Int? = null,
    val orderItemId: String? = null,
    val reasonCode: String,
    val reasonText: String? = null,
    val appliedBy: String? = null,
    val appliedAt: String,
    val voidedAt: String? = null,
    val voidReason: String? = null,
)

// --- Crear Pedido Staff ---
@Serializable
data class CreateStaffOrderRequest(
    val items: List<CreateOrderItemRequest>,
    val notes: String? = null
)

@Serializable
data class CreateOrderItemRequest(
    val productId: String,
    val quantity: Int,
    val note: String? = null
)

// --- Disponibilidad de Productos y Opciones Staff ---
@Serializable
data class ProductAvailabilityRequest(
    val available: Boolean,
    val reasonCode: String? = null // OUT_OF_STOCK, TEMPORARILY_UNAVAILABLE, RESTOCKED
)

@Serializable
data class OptionAvailabilityRequest(
    val available: Boolean,
    val reasonCode: String? = null
)

@Serializable
data class StaffProductAvailability(
    val productId: String,
    val name: String,
    val category: String,
    val available: Boolean,
    val priceMinor: Long
)