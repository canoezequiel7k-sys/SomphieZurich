package com.somphie.staff.feature.billing.data

import com.somphie.staff.core.network.CloseSessionRequest
import com.somphie.staff.core.network.StaffApi
import com.somphie.staff.core.network.StaffPaymentRequest
import com.somphie.staff.core.network.StaffPaymentResponse
import com.somphie.staff.core.network.StaffRefundRequest
import com.somphie.staff.core.network.StaffSession
import com.somphie.staff.core.network.StaffSessionDetail
import javax.inject.Inject
import javax.inject.Singleton

// Repositorio de Caja, Cobro y Pagos
@Singleton
class BillingRepository @Inject constructor(
    private val api: StaffApi
) {
    // Obtener la cuenta y el detalle de la sesión
    suspend fun getSessionDetail(sessionId: String): Result<StaffSessionDetail> {
        return runCatching {
            api.getSessionDetail(sessionId)
        }
    }

    // Registrar un pago en caja (Efectivo o Tarjeta simulada)
    suspend fun createPayment(
        sessionId: String,
        idempotencyKey: String,
        amountMinor: Long,
        tipMinor: Long = 0L,
        method: String = "CASH"
    ): Result<StaffPaymentResponse> {
        return runCatching {
            val response = api.createPayment(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                request = StaffPaymentRequest(
                    method = method,
                    amountMinor = amountMinor,
                    tipMinor = tipMinor
                )
            )
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                throw Exception("Error al registrar el pago: ${response.code()}")
            }
        }
    }

    // Reembolsar o anular un pago
    suspend fun refundPayment(
        paymentId: String,
        idempotencyKey: String,
        reasonCode: String,
        note: String? = null
    ): Result<StaffPaymentResponse> {
        return runCatching {
            val response = api.refundPayment(
                paymentId = paymentId,
                idempotencyKey = idempotencyKey,
                request = StaffRefundRequest(
                    reasonCode = reasonCode,
                    note = note
                )
            )
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                throw Exception("Error al procesar el reembolso: ${response.code()}")
            }
        }
    }

    // Cerrar la sesión de la mesa (normal o forzada)
    suspend fun closeSession(
        sessionId: String,
        idempotencyKey: String,
        force: Boolean = false,
        reasonCode: String? = null
    ): Result<StaffSession> {
        return runCatching {
            val response = api.closeSession(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                request = CloseSessionRequest(
                    force = force,
                    reasonCode = reasonCode
                )
            )
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                throw Exception("Error al cerrar la sesión: ${response.code()}")
            }
        }
    }

    // Aplicar un descuento o cortesía
    suspend fun createAdjustment(
        sessionId: String,
        idempotencyKey: String,
        type: String,
        valueMinor: Long? = null,
        percent: Int? = null,
        reasonCode: String,
        note: String? = null
    ): com.somphie.staff.core.network.StaffAdjustmentResponse {
        val response = api.createAdjustment(
            sessionId = sessionId,
            idempotencyKey = idempotencyKey,
            request = com.somphie.staff.core.network.StaffAdjustmentRequest(
                type = type,
                valueMinor = valueMinor,
                percent = percent,
                reasonCode = reasonCode,
                note = note
            )
        )
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            throw Exception("Error al aplicar descuento: ${response.code()}")
        }
    }

    // Anular un descuento o cortesía
    suspend fun voidAdjustment(
        adjustmentId: String,
        idempotencyKey: String,
        reasonCode: String,
        note: String? = null
    ): com.somphie.staff.core.network.StaffAdjustmentResponse {
        val response = api.voidAdjustment(
            adjustmentId = adjustmentId,
            idempotencyKey = idempotencyKey,
            request = com.somphie.staff.core.network.StaffVoidAdjustmentRequest(
                reasonCode = reasonCode,
                note = note
            )
        )
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            throw Exception("Error al anular descuento: ${response.code()}")
        }
    }
}
