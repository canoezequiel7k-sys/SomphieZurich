package com.somphie.staff.feature.orders.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.somphie.staff.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: OrderDetailViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val showRejectDialog = remember { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        viewModel.loadOrderDetail(orderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.order_detail_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ){
            when(val current = state){
                is OrderDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is OrderDetailUiState.Error -> Text(text = current.message, color = MaterialTheme.colorScheme.error ,modifier = Modifier.align(Alignment.Center))
                is OrderDetailUiState.Success -> {
                    val order = current.order
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            //Encabezado de mesa y estado
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column{
                                    Text(
                                        text = stringResource(R.string.table_item_label, order.tableLabel),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.order_items_count, order.items.size),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OrderStatusBadge(status = order.status)
                            }

                            Divider()

                            //Lista de productos y precios
                            order.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column{
                                        Text(
                                            text = "${item.quantity} x ${item.productName}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (item.optionsSummary.isNotBlank()){
                                            Text(
                                                text = item.optionsSummary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = formatMoney(item.totalMinor),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Divider()

                            //Total
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.order_total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(formatMoney(order.totalMinor), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }

                            //Notas del cliente
                            if (!order.notes.isNullOrBlank()){
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.customer_notes_title), fontWeight = FontWeight.Bold)
                                        Text(order.notes, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        // Acciones según estado
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (order.status == "PENDING_REVIEW") {
                                Button(
                                    onClick = { viewModel.acceptOrder(order.id, onNavigateBack) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_accept_order))
                                }
                            } else if (order.status == "ACCEPTED") {
                                Button(
                                    onClick = { /* Transición a listo */ },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_mark_ready))
                                }
                            }

                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.action_reject_order))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRejectDialog) {
        RejectOrderDialog(
            onDismiss = { showRejectDialog = false },
            onConfirm = { reason ->
                viewModel.rejectOrder(orderId, reason, onNavigateBack)
                showRejectDialog = false
            }
        )
    }
}

@Composable
private fun RejectOrderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selectedReason by remember { mutableStateOf("OUT_OF_STOCK") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_reject_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.dialog_reject_reason_label))
                listOf(
                    "OUT_OF_STOCK" to stringResource(R.string.reject_reason_out_of_stock),
                    "KITCHEN_OVERWHELMED" to stringResource(R.string.reject_reason_kitchen_overwhelmed),
                    "OTHER" to stringResource(R.string.reject_reason_other)
                ).forEach { (code, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedReason == code, onClick = { selectedReason = code })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedReason) }) {
                Text(stringResource(R.string.dialog_reject_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun formatMoney(minor: Long): String {
    return String.format(Locale.US, "$%.2f", minor / 100.0)
}