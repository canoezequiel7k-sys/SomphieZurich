package com.somphie.staff.feature.tables.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somphie.staff.core.network.StaffTable
import com.somphie.staff.feature.tables.data.TablesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TablesUiState{
    data object Loading : TablesUiState
    data class Success(val tables: List<StaffTable>) : TablesUiState
    data class Error(val message: String) : TablesUiState
}

@HiltViewModel
class TablesViewModel @Inject constructor(
    private val repository: TablesRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<TablesUiState>(TablesUiState.Loading)
    val uiState: StateFlow<TablesUiState> = _uiState.asStateFlow()

    init {
        loadTables()
    }

    fun loadTables() {
        viewModelScope.launch {
            _uiState.value = TablesUiState.Loading
            repository.getTables().fold(
                onSuccess = { tables ->
                    _uiState.value = TablesUiState.Success(tables)
                },
                onFailure = { error ->
                    _uiState.value = TablesUiState.Error(error.message ?: "Error al cargar mesas")
                }
            )
        }
    }
}

