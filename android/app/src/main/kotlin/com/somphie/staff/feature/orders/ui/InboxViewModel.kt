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

sealed interface InboxUiState{
    data object Loading: InboxUiState
    data class Success(
        val orders: List<StaffOrder>,
        val inProgressCount: Int,
        val readyCount: Int,
        val filter: OrderFilter = OrderFilter.IN_PROGRESS
    ): InboxUiState
    data class Error(val message: String): InboxUiState
}

enum class OrderFilter{ IN_PROGRESS, READY, ALL }

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: OrdersRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    private fun loadOrders(
        filter: OrderFilter = OrderFilter.IN_PROGRESS
    ) {
        viewModelScope.launch {
            _uiState.value = InboxUiState.Loading
            repository.getOrders().fold(
                onSuccess = { page ->
                    val orders = page.items
                    val inProgress = orders.filter { it.status == "PENDING_REVIEW" || it.status == "ACCEPTED" }
                    val ready = orders.filter { it.status == "READY" }

                    val filteredList = when(filter){
                        OrderFilter.IN_PROGRESS -> inProgress
                        OrderFilter.READY -> ready
                        OrderFilter.ALL -> orders
                    }

                    _uiState.value = InboxUiState.Success(
                        orders = filteredList,
                        inProgressCount = inProgress.size,
                        readyCount = ready.size,
                        filter = filter
                    )
                },
                onFailure = { error ->
                    _uiState.value = InboxUiState.Error(error.message ?: "Error al cargar pedidos")
                }
            )
        }
    }
}