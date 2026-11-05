package com.somphie.staff.feature.orders.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somphie.staff.core.network.StaffOrder
import com.somphie.staff.feature.orders.data.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OrderDetailUiState{
    data object Loading : OrderDetailUiState
    data class Success( val order: StaffOrder) : OrderDetailUiState
    data class Error(val message: String) : OrderDetailUiState
}

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val repository: OrdersRepository
) : ViewModel(){
    private val _uiState = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Loading)
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    fun loadOrderDetail(orderId: String){
        viewModelScope.launch {
            _uiState.value = OrderDetailUiState.Loading
            repository.getOrder(orderId).fold(
                onSuccess = { order ->
                    _uiState.value = OrderDetailUiState.Success(order)
                },
                onFailure = { error ->
                    _uiState.value = OrderDetailUiState.Error(error.message ?: "Error al cargar el pedido")
                }
            )
        }
    }

    fun acceptOrder(orderId: String, onSuccess: () -> Unit){
        viewModelScope.launch {
            repository.acceptOrder(orderId).fold(
                onSuccess = { onSuccess() },
                onFailure = { error ->
                    _uiState.value = OrderDetailUiState.Error(error.message ?: "Error al aceptar el pedido")
                }
            )
        }
    }

    fun rejectOrder(orderId: String, reasonCode: String, onSuccess: () -> Unit){
        viewModelScope.launch {
            repository.rejectOrder(orderId, reasonCode).fold(
                onSuccess = { onSuccess() },
                onFailure = { error ->
                    _uiState.value = OrderDetailUiState.Error(error.message ?: "Error al rechazar el pedido")
                }
            )
        }
    }

}
