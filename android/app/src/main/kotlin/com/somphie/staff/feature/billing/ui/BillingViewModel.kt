package com.somphie.staff.feature.billing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somphie.staff.core.network.AdjustmentKind
import com.somphie.staff.core.network.AdjustmentReasonCode
import com.somphie.staff.core.network.ApiFailure
import com.somphie.staff.core.network.StaffSessionDetail
import com.somphie.staff.feature.billing.data.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// Estado inmutable de la pantalla de Cobro en Caja
data class BillingUiState(
    val isLoading: Boolean = false,
    val sessionDetail: StaffSessionDetail? = null,
    val errorMessage: String? = null,
    val lastErrorCode: String? = null,
    val paymentSuccessMessage: String? = null,
    val isProcessingPayment: Boolean = false,
    val showPayDialog: Boolean = false,
    val showRefundDialog: Boolean = false,
    val showAdjustmentDialog: Boolean = false,
    val showForceCloseDialog: Boolean = false,
    val selectedPaymentId: String? = null,
    val activeIdempotencyKey: String? = null,
)

// Gestor de clave de idempotencia estable por intento lógico
class IdempotencyTracker {
    private var lastPayload: Any? = null
    private var lastKey: String? = null

    @Synchronized
    fun <T : Any> getOrCreateKey(payload: T): String {
        return if (payload == lastPayload && lastKey != null) {
            lastKey!!
        } else {
            val newKey = UUID.randomUUID().toString()
            lastPayload = payload
            lastKey = newKey
            newKey
        }
    }

    @Synchronized
    fun clear() {
        lastPayload = null
        lastKey = null
    }

    @Synchronized
    fun currentKey(): String? = lastKey

    @Synchronized
    fun currentPayload(): Any? = lastPayload
}

// Payloads tipados para validar estabilidad de clave e idempotencia
data class CashPaymentPayload(val sessionId: String, val amountMinor: Long, val tipMinor: Long, val payerLabel: String?)
data class VoidPaymentPayload(val paymentId: String, val reason: String)
data class RefundPayload(val sessionId: String, val amountMinor: Long, val reason: String)
data class ForceClosePayload(val sessionId: String, val reason: String)
data class CreateAdjustmentPayload(
    val sessionId: String,
    val kind: String,
    val percentBp: Int?,
    val amountMinor: Long?,
    val orderItemId: String?,
    val reasonCode: String,
    val reasonText: String?
)
data class VoidAdjustmentPayload(val adjustmentId: String, val reason: String)

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val repository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    // Tracker para preservar la misma clave mientras el intento lógico sea idéntico
    val idempotencyTracker = IdempotencyTracker()

    // Cargar la cuenta de la sesión
    fun loadBill(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, lastErrorCode = null)
            repository.getSessionDetail(sessionId)
                .onSuccess { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        sessionDetail = detail
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(error),
                        lastErrorCode = (error as? ApiFailure)?.code
                    )
                }
        }
    }

    // Registrar pago en efectivo en caja
    fun processPayment(
        sessionId: String,
        amountMinor: Long,
        tipMinor: Long = 0L,
        payerLabel: String? = null
    ) {
        // Guard anti-reentrada sincrónico
        if (_uiState.value.isProcessingPayment) return

        val payload = CashPaymentPayload(sessionId, amountMinor, tipMinor, payerLabel)
        val idempotencyKey = idempotencyTracker.getOrCreateKey(payload)

        _uiState.value = _uiState.value.copy(
            isProcessingPayment = true,
            errorMessage = null,
            lastErrorCode = null,
            activeIdempotencyKey = idempotencyKey
        )

        viewModelScope.launch {
            repository.createPayment(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                amountMinor = amountMinor,
                tipMinor = tipMinor,
                payerLabel = payerLabel
            )
                .onSuccess { result ->
                    idempotencyTracker.clear()
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        showPayDialog = false,
                        paymentSuccessMessage = "Pago registrado correctamente",
                        activeIdempotencyKey = null
                    )
                    loadBill(sessionId)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = formatErrorMessage(error),
                        lastErrorCode = (error as? ApiFailure)?.code
                    )
                }
        }
    }

    // Aplicar descuento porcentual (reglas de límite validadas por el backend, RN-ADJ / D-142)
    fun applyDiscountPercent(
        sessionId: String,
        percent: Int,
        reasonCode: String = AdjustmentReasonCode.SERVICE_ISSUE.code,
        reasonText: String? = null
    ) {
        // Guard anti-reentrada sincrónico
        if (_uiState.value.isProcessingPayment) return

        val percentBp = percent * 100 // 10% = 1000 bp
        val payload = CreateAdjustmentPayload(
            sessionId = sessionId,
            kind = AdjustmentKind.DISCOUNT_PERCENT,
            percentBp = percentBp,
            amountMinor = null,
            orderItemId = null,
            reasonCode = reasonCode,
            reasonText = reasonText
        )
        val idempotencyKey = idempotencyTracker.getOrCreateKey(payload)

        _uiState.value = _uiState.value.copy(
            isProcessingPayment = true,
            errorMessage = null,
            lastErrorCode = null,
            activeIdempotencyKey = idempotencyKey
        )

        viewModelScope.launch {
            repository.createAdjustment(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                kind = AdjustmentKind.DISCOUNT_PERCENT,
                percentBp = percentBp,
                amountMinor = null,
                orderItemId = null,
                reasonCode = reasonCode,
                reasonText = reasonText
            )
                .onSuccess {
                    idempotencyTracker.clear()
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        showAdjustmentDialog = false,
                        paymentSuccessMessage = "Descuento del $percent% aplicado correctamente",
                        activeIdempotencyKey = null
                    )
                    loadBill(sessionId)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = formatErrorMessage(error),
                        lastErrorCode = (error as? ApiFailure)?.code
                    )
                }
        }
    }

    // Aplicar descuento por monto fijo o cortesía
    fun applyDiscountAmount(
        sessionId: String,
        amountMinor: Long,
        reasonCode: String = AdjustmentReasonCode.COURTESY_HOUSE.code,
        reasonText: String? = null
    ) {
        // Guard anti-reentrada sincrónico
        if (_uiState.value.isProcessingPayment) return

        val payload = CreateAdjustmentPayload(
            sessionId = sessionId,
            kind = AdjustmentKind.DISCOUNT_AMOUNT,
            percentBp = null,
            amountMinor = amountMinor,
            orderItemId = null,
            reasonCode = reasonCode,
            reasonText = reasonText
        )
        val idempotencyKey = idempotencyTracker.getOrCreateKey(payload)

        _uiState.value = _uiState.value.copy(
            isProcessingPayment = true,
            errorMessage = null,
            lastErrorCode = null,
            activeIdempotencyKey = idempotencyKey
        )

        viewModelScope.launch {
            repository.createAdjustment(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                kind = AdjustmentKind.DISCOUNT_AMOUNT,
                percentBp = null,
                amountMinor = amountMinor,
                orderItemId = null,
                reasonCode = reasonCode,
                reasonText = reasonText
            )
                .onSuccess {
                    idempotencyTracker.clear()
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        showAdjustmentDialog = false,
                        paymentSuccessMessage = "Descuento de ${formatMoney(amountMinor)} aplicado",
                        activeIdempotencyKey = null
                    )
                    loadBill(sessionId)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = formatErrorMessage(error),
                        lastErrorCode = (error as? ApiFailure)?.code
                    )
                }
        }
    }

    // Anular un pago registrado
    fun voidPayment(
        paymentId: String,
        sessionId: String,
        reason: String = "Anulación por error en caja"
    ) {
        // Guard anti-reentrada sincrónico
        if (_uiState.value.isProcessingPayment) return

        val payload = VoidPaymentPayload(paymentId, reason)
        val idempotencyKey = idempotencyTracker.getOrCreateKey(payload)

        _uiState.value = _uiState.value.copy(
            isProcessingPayment = true,
            errorMessage = null,
            lastErrorCode = null,
            activeIdempotencyKey = idempotencyKey
        )

        viewModelScope.launch {
            repository.voidPayment(
                paymentId = paymentId,
                idempotencyKey = idempotencyKey,
                reason = reason
            )
                .onSuccess {
                    idempotencyTracker.clear()
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        showRefundDialog = false,
                        paymentSuccessMessage = "Pago anulado correctamente",
                        activeIdempotencyKey = null
                    )
                    loadBill(sessionId)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = formatErrorMessage(error),
                        lastErrorCode = (error as? ApiFailure)?.code
                    )
                }
        }
    }

    // Procesar reembolso de una sesión
    fun processRefund(
        sessionId: String,
        amountMinor: Long,
        reason: String = "Reembolso acordado con el cliente"
    ) {
        // Guard anti-reentrada sincrónico
        if (_uiState.value.isProcessingPayment) return

        val payload = RefundPayload(sessionId, amountMinor, reason)
        val idempotencyKey = idempotencyTracker.getOrCreateKey(payload)

        _uiState.value = _uiState.value.copy(
            isProcessingPayment = true,
            errorMessage = null,
            lastErrorCode = null,
            activeIdempotencyKey = idempotencyKey
        )

        viewModelScope.launch {
            repository.refundPayment(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                amountMinor = amountMinor,
                reason = reason
            )
                .onSuccess {
                    idempotencyTracker.clear()
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        showRefundDialog = false,
                        paymentSuccessMessage = "Reembolso procesado correctamente",
                        activeIdempotencyKey = null
                    )
                    loadBill(sessionId)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = formatErrorMessage(error),
                        lastErrorCode = (error as? ApiFailure)?.code
                    )
                }
        }
    }

    // Cerrar sesión de mesa normalmente (sin clave ni cuerpo)
    fun closeSession(
        sessionId: String,
        onClosed: () -> Unit = {}
    ) {
        // Guard anti-reentrada sincrónico
        if (_uiState.value.isProcessingPayment) return

        _uiState.value = _uiState.value.copy(isProcessingPayment = true, errorMessage = null, lastErrorCode = null)

        viewModelScope.launch {
            repository.closeSession(sessionId = sessionId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isProcessingPayment = false)
                    onClosed()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = formatErrorMessage(error),
                        lastErrorCode = (error as? ApiFailure)?.code
                    )
                }
        }
    }

    // Cierre forzado de mesa con motivo
    fun forceCloseSession(
        sessionId: String,
        reason: String,
        onClosed: () -> Unit = {}
    ) {
        // Guard anti-reentrada sincrónico
        if (_uiState.value.isProcessingPayment) return

        val payload = ForceClosePayload(sessionId, reason)
        val idempotencyKey = idempotencyTracker.getOrCreateKey(payload)

        _uiState.value = _uiState.value.copy(
            isProcessingPayment = true,
            errorMessage = null,
            lastErrorCode = null,
            activeIdempotencyKey = idempotencyKey
        )

        viewModelScope.launch {
            repository.forceCloseSession(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                reason = reason
            )
                .onSuccess {
                    idempotencyTracker.clear()
                    _uiState.value = _uiState.value.copy(isProcessingPayment = false, showForceCloseDialog = false)
                    onClosed()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = formatErrorMessage(error),
                        lastErrorCode = (error as? ApiFailure)?.code
                    )
                }
        }
    }

    fun openPayDialog() {
        _uiState.value = _uiState.value.copy(showPayDialog = true)
    }

    fun closePayDialog() {
        idempotencyTracker.clear()
        _uiState.value = _uiState.value.copy(showPayDialog = false, activeIdempotencyKey = null)
    }

    fun openAdjustmentDialog() {
        _uiState.value = _uiState.value.copy(showAdjustmentDialog = true)
    }

    fun closeAdjustmentDialog() {
        idempotencyTracker.clear()
        _uiState.value = _uiState.value.copy(showAdjustmentDialog = false, activeIdempotencyKey = null)
    }

    fun openRefundDialog(paymentId: String) {
        _uiState.value = _uiState.value.copy(showRefundDialog = true, selectedPaymentId = paymentId)
    }

    fun closeRefundDialog() {
        idempotencyTracker.clear()
        _uiState.value = _uiState.value.copy(showRefundDialog = false, selectedPaymentId = null, activeIdempotencyKey = null)
    }

    fun openForceCloseDialog() {
        _uiState.value = _uiState.value.copy(showForceCloseDialog = true)
    }

    fun closeForceCloseDialog() {
        idempotencyTracker.clear()
        _uiState.value = _uiState.value.copy(showForceCloseDialog = false, activeIdempotencyKey = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(paymentSuccessMessage = null)
    }

    // Mapeo detallado de Problem+JSON / Códigos de error del contrato
    private fun formatErrorMessage(error: Throwable): String {
        return if (error is ApiFailure) {
            when (error.code) {
                "ADJUSTMENT_LIMIT_EXCEEDED" -> "El ajuste supera el límite permitido para su rol (ADJUSTMENT_LIMIT_EXCEEDED)."
                "UNCERTAIN_RESULT" -> "Resultado incierto: la respuesta del servidor no pudo leerse. Reintentar con la misma clave."
                "STATE_CONFLICT" -> "Conflicto de estado: la sesión o el pago no admiten esta transición (409)."
                "HTTP_403" -> "No tiene permisos suficientes para ejecutar esta acción en caja (403)."
                "HTTP_409" -> "Conflicto con el estado actual del recurso (409)."
                else -> error.detail ?: "Error en la operación (${error.code})"
            }
        } else {
            error.message ?: "Error al procesar la operación"
        }
    }
}

// Formatear centavos a moneda e.g. 1090 -> "$10.90" (Pendiente de modularizar con formatMoney/strings.xml)
fun formatMoney(minor: Long, currency: String = "ARS"): String {
    val symbol = if (currency == "CHF") "CHF " else "$"
    val major = minor / 100.0
    return String.format(java.util.Locale.US, "%s%.2f", symbol, major)
}
