package com.somphie.staff.feature.orders.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somphie.staff.R
import com.somphie.staff.core.network.StaffOrder

// Pantalla de Bandeja de Pedidos
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onNavigateBack: () -> Unit,
    onOrderClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        // Barra superior (TopAppBar) de Pedidos
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.orders_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                // Boton de volver atras
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when (val current = state) {
                // Indicador de carga centrado
                is InboxUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                // Mensaje de error
                is InboxUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = current.message, color = MaterialTheme.colorScheme.error)
                }
                is InboxUiState.Success -> {
                    // Barra de filtros de vista (En curso, Listos, Todos)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Filtro "En curso"
                        FilterChip(
                            selected = current.filter == OrderFilter.IN_PROGRESS,
                            onClick = { viewModel.loadOrders(OrderFilter.IN_PROGRESS) },
                            label = { Text(stringResource(R.string.filter_in_progress, current.inProgressCount)) }
                        )
                        // Filtro "Listos"
                        FilterChip(
                            selected = current.filter == OrderFilter.READY,
                            onClick = { viewModel.loadOrders(OrderFilter.READY) },
                            label = { Text(stringResource(R.string.filter_ready, current.readyCount)) }
                        )
                        // Filtro "Todos"
                        FilterChip(
                            selected = current.filter == OrderFilter.ALL,
                            onClick = { viewModel.loadOrders(OrderFilter.ALL) },
                            label = { Text(stringResource(R.string.filter_all)) }
                        )
                    }
                    // Lista scrolleable de pedidos por mesa
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(current.orders, key = { it.id }) { order ->
                            OrderCard(order = order, onClick = { onOrderClick(order.id) })
                        }
                    }
                }
            }
        }
    }
}

// Tarjeta individual de pedido
@Composable
private fun OrderCard(
    order: StaffOrder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Numero de mesa
                Text(
                    text = stringResource(R.string.table_item_label, order.tableLabel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Badge / Insignia de estado del pedido
                OrderStatusBadge(status = order.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Lista de items / productos del pedido
            order.items.forEach { item ->
                Text(
                    text = "${item.quantity} x ${item.productName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Badge de estado con color segun el estado
@Composable
fun OrderStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "PENDING_REVIEW" -> Triple(com.somphie.staff.core.design.StaffColors.StatusPendingBg, com.somphie.staff.core.design.StaffColors.StatusPendingText, "En espera")
        "ACCEPTED" -> Triple(com.somphie.staff.core.design.StaffColors.StatusInPrepBg, com.somphie.staff.core.design.StaffColors.StatusInPrepText, "En preparación")
        "READY" -> Triple(com.somphie.staff.core.design.StaffColors.StatusReadyBg, com.somphie.staff.core.design.StaffColors.StatusReadyText, "Listo")
        else -> Triple(Color.Gray, Color.White, status)
    }

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
