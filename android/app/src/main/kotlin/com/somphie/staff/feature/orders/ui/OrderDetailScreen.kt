package com.somphie.staff.feature.orders.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: OrderDetailViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val showRejectDialog = remember { mutableStateOf(false) }
}