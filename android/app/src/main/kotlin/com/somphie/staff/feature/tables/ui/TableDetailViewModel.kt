package com.somphie.staff.feature.tables.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somphie.staff.core.network.StaffSessionDetail
import com.somphie.staff.feature.tables.data.TablesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estados de UI para el Detalle de Mesa
sealed interface TableDetailUiState {
    data object Loading : TableDetailUiState
    data class Success(
        val sessionDetail: StaffSessionDetail,
        val tableLabel: String
    ) : TableDetailUiState
    data class Error(val message: String) : TableDetailUiState
}

// ViewModel para la pantalla de Detalle de Mesa
@HiltViewModel
class TableDetailViewModel @Inject constructor(
    private val repository: TablesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TableDetailUiState>(TableDetailUiState.Loading)
    val uiState: StateFlow<TableDetailUiState> = _uiState.asStateFlow()

    // Cargar los datos de la sesion / mesa
    fun loadTableDetail(sessionId: String, tableLabel: String = "") {
        viewModelScope.launch {
            _uiState.value = TableDetailUiState.Loading
            repository.getSessionDetail(sessionId).fold(
                onSuccess = { detail ->
                    _uiState.value = TableDetailUiState.Success(
                        sessionDetail = detail,
                        tableLabel = if (tableLabel.isNotBlank()) tableLabel else detail.session.tableLabel
                    )
                },
                onFailure = { error ->
                    _uiState.value = TableDetailUiState.Error(error.message ?: "Error al cargar detalle de mesa")
                }
            )
        }
    }
}
