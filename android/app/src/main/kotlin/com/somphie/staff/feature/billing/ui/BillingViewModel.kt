package com.somphie.staff.feature.billing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val paymentSuccessMessage: String? = null,
    val isProcessingPayment: Boolean = false,
    val showPayDialog: Boolean = false,
    val showRefundDialog: Boolean = false,
    val showAdjustmentDialog: Boolean = false,
    val selectedPaymentId: String? = null
)

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val repository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    // Cargar la cuenta de la sesión
    fun loadBill(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
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
                        errorMessage = error.message ?: "Error al cargar la cuenta"
                    )
                }
        }
    }

    // Registrar pago en efectivo o tarjeta simulada
    fun processPayment(
        sessionId: String,
        amountMinor: Long,
        tipMinor: Long = 0L,
        method: String = "CASH"
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingPayment = true, errorMessage = null)
            val idempotencyKey = UUID.randomUUID().toString()
            repository.createPayment(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                amountMinor = amountMinor,
                tipMinor = tipMinor,
                method = method
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        showPayDialog = false,
                        paymentSuccessMessage = "Pago registrado correctamente"
                    )
                    loadBill(sessionId)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = error.message ?: "Error al procesar el pago"
                    )
                }
        }
    }

    // Aplicar descuento porcentual con verificación de límite del 15% para mozos (D-142)
    fun applyDiscountPercent(
        sessionId: String,
        percent: Int,
        reasonCode: String = "WAITER_COURTESY",
        isWaiter: Boolean = false
    ) {
        // Regla D-142: El mozo solo puede aplicar hasta 15% acumulado
        if (isWaiter && percent > 15) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Los mozos solo pueden aplicar hasta un 15% de descuento (Regla D-142)"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingPayment = true, errorMessage = null)
            val idempotencyKey = UUID.randomUUID().toString()
            runCatching {
                repository.createAdjustment(
                    sessionId = sessionId,
                    idempotencyKey = idempotencyKey,
                    type = "DISCOUNT_PERCENT",
                    percent = percent,
                    reasonCode = reasonCode
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isProcessingPayment = false,
                    showAdjustmentDialog = false,
                    paymentSuccessMessage = "Descuento del $percent% aplicado correctamente"
                )
                loadBill(sessionId)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isProcessingPayment = false,
                    errorMessage = error.message ?: "Error al aplicar descuento"
                )
            }
        }
    }

    // Aplicar descuento por monto fijo
    fun applyDiscountAmount(
        sessionId: String,
        amountMinor: Long,
        reasonCode: String = "WAITER_COURTESY"
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingPayment = true, errorMessage = null)
            val idempotencyKey = UUID.randomUUID().toString()
            runCatching {
                repository.createAdjustment(
                    sessionId = sessionId,
                    idempotencyKey = idempotencyKey,
                    type = "DISCOUNT_AMOUNT",
                    valueMinor = amountMinor,
                    reasonCode = reasonCode
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isProcessingPayment = false,
                    showAdjustmentDialog = false,
                    paymentSuccessMessage = "Descuento de ${formatMoney(amountMinor)} aplicado"
                )
                loadBill(sessionId)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isProcessingPayment = false,
                    errorMessage = error.message ?: "Error al aplicar descuento por monto"
                )
            }
        }
    }

    // Anular o reembolsar un pago
    fun processRefund(
        paymentId: String,
        sessionId: String,
        reasonCode: String = "CUSTOMER_CANCELLED"
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingPayment = true, errorMessage = null)
            val idempotencyKey = UUID.randomUUID().toString()
            repository.refundPayment(
                paymentId = paymentId,
                idempotencyKey = idempotencyKey,
                reasonCode = reasonCode
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        showRefundDialog = false,
                        paymentSuccessMessage = "Reembolso procesado correctamente"
                    )
                    loadBill(sessionId)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = error.message ?: "Error al reembolsar el pago"
                    )
                }
        }
    }

    // Cerrar sesión de mesa
    fun closeSession(
        sessionId: String,
        force: Boolean = false,
        reasonCode: String? = null,
        onClosed: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingPayment = true, errorMessage = null)
            val idempotencyKey = UUID.randomUUID().toString()
            repository.closeSession(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                force = force,
                reasonCode = reasonCode
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isProcessingPayment = false)
                    onClosed()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = error.message ?: "Error al cerrar la sesión"
                    )
                }
        }
    }

    fun openPayDialog() {
        _uiState.value = _uiState.value.copy(showPayDialog = true)
    }

    fun closePayDialog() {
        _uiState.value = _uiState.value.copy(showPayDialog = false)
    }

    fun openAdjustmentDialog() {
        _uiState.value = _uiState.value.copy(showAdjustmentDialog = true)
    }

    fun closeAdjustmentDialog() {
        _uiState.value = _uiState.value.copy(showAdjustmentDialog = false)
    }

    fun openRefundDialog(paymentId: String) {
        _uiState.value = _uiState.value.copy(showRefundDialog = true, selectedPaymentId = paymentId)
    }

    fun closeRefundDialog() {
        _uiState.value = _uiState.value.copy(showRefundDialog = false, selectedPaymentId = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(paymentSuccessMessage = null)
    }
}

// Formatear centavos a moneda e.g. 1090 -> "$10.90"
fun formatMoney(minor: Long, currency: String = "ARS"): String {
    val symbol = if (currency == "CHF") "CHF " else "$"
    val major = minor / 100.0
    return String.format(java.util.Locale.US, "%s%.2f", symbol, major)
}
