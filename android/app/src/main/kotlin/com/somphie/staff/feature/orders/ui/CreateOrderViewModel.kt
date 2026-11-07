package com.somphie.staff.feature.orders.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somphie.staff.core.network.CreateOrderItemRequest
import com.somphie.staff.core.network.StaffOrder
import com.somphie.staff.feature.orders.data.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Item de producto de la carta para selección
data class CatalogProductItem(
    val id: String,
    val name: String,
    val priceMinor: Long,
    val category: String
)

// Estado inmutable de la pantalla de Crear Pedido
data class CreateOrderUiState(
    val isSubmitting: Boolean = false,
    val quantities: Map<String, Int> = emptyMap(),
    val notes: String = "",
    val createdOrder: StaffOrder? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    private val repository: OrdersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    // Carta de productos demo para selección rápida
    val demoCatalog = listOf(
        CatalogProductItem("p-latte", "Latte", 320, "Cafetería"),
        CatalogProductItem("p-espresso", "Espresso", 250, "Cafetería"),
        CatalogProductItem("p-capuchino", "Capuchino", 350, "Cafetería"),
        CatalogProductItem("p-tostado", "Tostado mixto", 450, "Comidas"),
        CatalogProductItem("p-medialuna", "Medialuna de manteca", 180, "Comidas"),
        CatalogProductItem("p-agua", "Agua mineral", 150, "Bebidas")
    )

    fun incrementProduct(productId: String) {
        val currentMap = _uiState.value.quantities.toMutableMap()
        val currentCount = currentMap[productId] ?: 0
        currentMap[productId] = currentCount + 1
        _uiState.value = _uiState.value.copy(quantities = currentMap)
    }

    fun decrementProduct(productId: String) {
        val currentMap = _uiState.value.quantities.toMutableMap()
        val currentCount = currentMap[productId] ?: 0
        if (currentCount > 1) {
            currentMap[productId] = currentCount - 1
        } else {
            currentMap.remove(productId)
        }
        _uiState.value = _uiState.value.copy(quantities = currentMap)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    // Enviar el pedido a la sesión de mesa
    fun submitOrder(sessionId: String, onSuccess: () -> Unit = {}) {
        val selectedItems = _uiState.value.quantities.map { (productId, qty) ->
            CreateOrderItemRequest(productId = productId, quantity = qty)
        }

        if (selectedItems.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Selecciona al menos un producto")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            repository.createStaffOrder(
                sessionId = sessionId,
                items = selectedItems,
                notes = _uiState.value.notes.ifBlank { null }
            ).onSuccess { order ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    createdOrder = order
                )
                onSuccess()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = error.message ?: "Error al enviar el pedido"
                )
            }
        }
    }
}
