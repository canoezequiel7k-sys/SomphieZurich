package com.somphie.staff.feature.availability.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somphie.staff.core.network.StaffProductAvailability
import com.somphie.staff.feature.availability.data.AvailabilityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado inmutable de la pantalla de Gestión de Disponibilidad
data class AvailabilityUiState(
    val isLoading: Boolean = false,
    val products: List<StaffProductAvailability> = emptyList(),
    val searchQuery: String = "",
    val isUpdating: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AvailabilityViewModel @Inject constructor(
    private val repository: AvailabilityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvailabilityUiState())
    val uiState: StateFlow<AvailabilityUiState> = _uiState.asStateFlow()

    // Carta demo de productos para alternar disponibilidad en vivo
    private val initialProducts = listOf(
        StaffProductAvailability("p-latte", "Latte", "Cafetería", true, 320),
        StaffProductAvailability("p-espresso", "Espresso", "Cafetería", true, 250),
        StaffProductAvailability("p-capuchino", "Capuchino", "Cafetería", true, 350),
        StaffProductAvailability("p-tostado", "Tostado mixto", "Comidas", true, 450),
        StaffProductAvailability("p-medialuna", "Medialuna de manteca", "Comidas", false, 180),
        StaffProductAvailability("p-agua", "Agua mineral", "Bebidas", true, 150)
    )

    init {
        loadProducts()
    }

    fun loadProducts() {
        _uiState.value = _uiState.value.copy(products = initialProducts)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    // Alternar la disponibilidad operativa (Habilitar / Agotar)
    fun toggleProductAvailability(productId: String) {
        val currentList = _uiState.value.products
        val product = currentList.find { it.productId == productId } ?: return
        val newAvailableState = !product.available

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, errorMessage = null)

            repository.updateProductAvailability(
                productId = productId,
                available = newAvailableState,
                reasonCode = if (newAvailableState) "RESTOCKED" else "OUT_OF_STOCK"
            ).onSuccess {
                // Actualizar la lista local con el nuevo estado
                val updatedList = currentList.map { item ->
                    if (item.productId == productId) item.copy(available = newAvailableState) else item
                }
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    products = updatedList,
                    successMessage = if (newAvailableState) "Producto marcado como disponible" else "Producto marcado como agotado"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    errorMessage = error.message ?: "Error al actualizar la disponibilidad"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}
