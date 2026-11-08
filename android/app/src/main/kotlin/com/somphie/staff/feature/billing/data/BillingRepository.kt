package com.somphie.staff.feature.billing.data

import com.somphie.staff.core.network.CreateAdjustmentRequest
import com.somphie.staff.core.network.CreateCashPaymentRequest
import com.somphie.staff.core.network.CreateRefundRequest
import com.somphie.staff.core.network.PaymentResult
import com.somphie.staff.core.network.ReasonRequest
import com.somphie.staff.core.network.StaffAdjustment
import com.somphie.staff.core.network.StaffApi
import com.somphie.staff.core.network.StaffSession
import com.somphie.staff.core.network.StaffSessionDetail
import com.somphie.staff.core.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

// Repositorio de Caja, Cobro, Ajustes y Cierres (Alineado con OpenAPI)
@Singleton
class BillingRepository @Inject constructor(
    private val api: StaffApi,
    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false }
) {
    // Obtener la cuenta y el detalle de la sesión
    suspend fun getSessionDetail(sessionId: String): Result<StaffSessionDetail> {
        return runCatching {
            apiCall(json) { api.getSessionDetail(sessionId) }
        }
    }

    // Registrar un pago en caja (Efectivo)
    suspend fun createPayment(
        sessionId: String,
        idempotencyKey: String,
        amountMinor: Long,
        tipMinor: Long = 0L,
        payerLabel: String? = null
    ): Result<PaymentResult> {
        return runCatching {
            apiCall(json) {
                api.createPayment(
                    sessionId = sessionId,
                    idempotencyKey = idempotencyKey,
                    request = CreateCashPaymentRequest(
                        amountMinor = amountMinor,
                        tipMinor = tipMinor,
                        payerLabel = payerLabel
                    )
                )
            }
        }
    }

    // Anular un pago registrado en caja
    suspend fun voidPayment(
        paymentId: String,
        idempotencyKey: String,
        reason: String
    ): Result<PaymentResult> {
        return runCatching {
            apiCall(json) {
                api.voidPayment(
                    paymentId = paymentId,
                    idempotencyKey = idempotencyKey,
                    request = ReasonRequest(reason = reason)
                )
            }
        }
    }

    // Reembolsar saldo de una sesión
    suspend fun refundPayment(
        sessionId: String,
        idempotencyKey: String,
        amountMinor: Long,
        reason: String
    ): Result<PaymentResult> {
        return runCatching {
            apiCall(json) {
                api.refundPayment(
                    sessionId = sessionId,
                    idempotencyKey = idempotencyKey,
                    request = CreateRefundRequest(
                        amountMinor = amountMinor,
                        reason = reason
                    )
                )
            }
        }
    }

    // Cerrar normalmente la sesión de la mesa (sin cuerpo ni Idempotency-Key)
    suspend fun closeSession(
        sessionId: String
    ): Result<StaffSession> {
        return runCatching {
            apiCall(json) {
                api.closeSession(sessionId = sessionId)
            }
        }
    }

    // Cerrar de forma forzada la sesión de la mesa con motivo
    suspend fun forceCloseSession(
        sessionId: String,
        idempotencyKey: String,
        reason: String
    ): Result<StaffSession> {
        return runCatching {
            apiCall(json) {
                api.forceCloseSession(
                    sessionId = sessionId,
                    idempotencyKey = idempotencyKey,
                    request = ReasonRequest(reason = reason)
                )
            }
        }
    }

    // Aplicar un descuento, cortesía o ajuste
    suspend fun createAdjustment(
        sessionId: String,
        idempotencyKey: String,
        kind: String,
        percentBp: Int? = null,
        amountMinor: Long? = null,
        orderItemId: String? = null,
        reasonCode: String,
        reasonText: String? = null
    ): Result<StaffAdjustment> {
        return runCatching {
            apiCall(json) {
                api.createAdjustment(
                    sessionId = sessionId,
                    idempotencyKey = idempotencyKey,
                    request = CreateAdjustmentRequest(
                        kind = kind,
                        percentBp = percentBp,
                        amountMinor = amountMinor,
                        orderItemId = orderItemId,
                        reasonCode = reasonCode,
                        reasonText = reasonText
                    )
                )
            }
        }
    }

    // Anular un descuento o cortesía previamente aplicado
    suspend fun voidAdjustment(
        adjustmentId: String,
        idempotencyKey: String,
        reason: String
    ): Result<StaffAdjustment> {
        return runCatching {
            apiCall(json) {
                api.voidAdjustment(
                    adjustmentId = adjustmentId,
                    idempotencyKey = idempotencyKey,
                    request = ReasonRequest(reason = reason)
                )
            }
        }
    }
}
