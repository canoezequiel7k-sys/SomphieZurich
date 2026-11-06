package com.somphie.staff.feature.orders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somphie.staff.R
import com.somphie.staff.core.design.SomphieTheme
import com.somphie.staff.core.design.StaffColors
import com.somphie.staff.core.network.StaffOrder
import com.somphie.staff.core.network.StaffOrderItem

// Pantalla de Detalle del Pedido
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: OrderDetailViewModel,
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.loadOrderDetail(orderId)
    }

    OrderDetailContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onAcceptOrder = { viewModel.acceptOrder(orderId, onNavigateBack) },
        onRejectOrder = { reason -> viewModel.rejectOrder(orderId, reason, onNavigateBack) },
        showTopBar = showTopBar
    )
}

// Contenido desacoplado de la pantalla de Detalle para soportar Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailContent(
    state: OrderDetailUiState,
    onNavigateBack: () -> Unit,
    onAcceptOrder: () -> Unit,
    onRejectOrder: (String) -> Unit,
    showTopBar: Boolean = true
) {
    var showRejectDialog by remember { mutableStateOf(false) }

    Scaffold(
        // Barra superior opcional
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(stringResource(R.string.order_detail_title), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (state) {
                // Indicador de carga
                is OrderDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                // Mensaje de error
                is OrderDetailUiState.Error -> Text(text = state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                is OrderDetailUiState.Success -> {
                    val order = state.order
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Encabezado con numero de mesa, subtexto (hora y productos) y badge de estado
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    // Nombre/Numero de Mesa (ej: Mesa 4)
                                    Text(
                                        text = stringResource(R.string.table_item_label, order.tableLabel),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )
                                    // Horario y cantidad de productos (ej: 12:34 · 2 productos)
                                    val timeText = if (order.createdAt.length >= 16) order.createdAt.substring(11, 16) else "12:34"
                                    Text(
                                        text = "$timeText · ${stringResource(R.string.order_items_count, order.items.size)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                                // Badge / Pastilla de estado
                                OrderStatusBadge(status = order.status)
                            }

                            // Card con la lista de productos y sus precios
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    order.items.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                // Punto viñeta indicador
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 6.dp)
                                                        .size(8.dp)
                                                        .background(StaffColors.BadgeBrown, shape = CircleShape)
                                                )
                                                Column {
                                                    // Cantidad y nombre del producto (ej: 2 x Latte)
                                                    Text(
                                                        text = "${item.quantity} x ${item.productName}",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = StaffColors.Coffee,
                                                        fontSize = 16.sp
                                                    )
                                                    // Opciones/modificadores (ej: 1 x Leche de almendra)
                                                    if (item.optionsSummary.isNotBlank()) {
                                                        Text(
                                                            text = item.optionsSummary,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                }
                                            }
                                            // Precio formateado (ej: $6.40)
                                            Text(
                                                text = formatMoney(item.totalMinor),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = StaffColors.Coffee,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Fila del importe Total
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.order_total),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = formatMoney(order.totalMinor),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = StaffColors.Caramel,
                                    fontSize = 22.sp
                                )
                            }

                            // Card con las notas adicionales del cliente
                            if (!order.notes.isNullOrBlank()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.customer_notes_title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = order.notes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        // Acciones principales segun el estado del pedido
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (order.status == "PENDING_REVIEW") {
                                // Boton principal de Aceptar pedido
                                Button(
                                    onClick = onAcceptOrder,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StaffColors.Caramel)
                                ) {
                                    Text(stringResource(R.string.action_accept_order), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            } else if (order.status == "ACCEPTED") {
                                // Boton principal de Marcar como listo
                                Button(
                                    onClick = { /* Transicion a listo */ },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StaffColors.Caramel)
                                ) {
                                    Text(stringResource(R.string.action_mark_ready), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }

                            // Boton secundario de Rechazar pedido
                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(stringResource(R.string.action_reject_order), color = StaffColors.Coffee, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogo modal para rechazar pedido con motivo
    if (showRejectDialog) {
        RejectOrderDialog(
            onDismiss = { showRejectDialog = false },
            onConfirm = { reason ->
                onRejectOrder(reason)
                showRejectDialog = false
            }
        )
    }
}

// Dialogo emergente para elegir el motivo de rechazo
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
            // Boton de confirmacion del rechazo
            Button(onClick = { onConfirm(selectedReason) }) {
                Text(stringResource(R.string.dialog_reject_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// Formateo de precios en centavos (Long)
fun formatMoney(minor: Long): String {
    val sign = if (minor < 0) "-" else ""
    val absMinor = kotlin.math.abs(minor)
    val dollars = absMinor / 100
    val cents = absMinor % 100
    return "$sign$$dollars.${cents.toString().padStart(2, '0')}"
}

// Preview para Android Studio (Split / Design tab)
@Preview(showBackground = true)
@Composable
fun OrderDetailScreenPreview() {
    SomphieTheme {
        OrderDetailContent(
            state = OrderDetailUiState.Success(
                order = StaffOrder(
                    id = "1",
                    code = "#004",
                    sessionId = "s1",
                    tableLabel = "4",
                    status = "ACCEPTED",
                    createdAt = "2026-10-18T12:34:00Z",
                    items = listOf(
                        StaffOrderItem("i1", "p1", "Latte", 2, 320, 640, "ACCEPTED", optionsSummary = "1 x Leche de almendra"),
                        StaffOrderItem("i2", "p2", "Tostado mixto", 1, 450, 450, "ACCEPTED")
                    ),
                    totalMinor = 1090,
                    notes = "Sin azúcar en el latte."
                )
            ),
            onNavigateBack = {},
            onAcceptOrder = {},
            onRejectOrder = {},
            showTopBar = true
        )
    }
}
