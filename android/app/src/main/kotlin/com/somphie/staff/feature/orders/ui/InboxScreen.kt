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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

// Pantalla de Bandeja de Pedidos
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onNavigateBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    showTopBar: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()

    InboxContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onFilterChange = { viewModel.loadOrders(it) },
        onOrderClick = onOrderClick,
        showTopBar = showTopBar
    )
}

// Contenido desacoplado de la pantalla de Pedidos para soportar Preview y contenedores con TopBar propia
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxContent(
    state: InboxUiState,
    onNavigateBack: () -> Unit,
    onFilterChange: (OrderFilter) -> Unit,
    onOrderClick: (String) -> Unit,
    showTopBar: Boolean = true
) {
    Scaffold(
        // Barra superior opcional (solo si showTopBar es true)
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.orders_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when (state) {
                // Indicador de carga centrado
                is InboxUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                // Mensaje de error
                is InboxUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
                is InboxUiState.Success -> {
                    // Barra de capsulas de filtro de vista (En curso, Listos, Todos)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Capsula de Filtro "En curso (5)"
                        FilterChip(
                            selected = state.filter == OrderFilter.IN_PROGRESS,
                            onClick = { onFilterChange(OrderFilter.IN_PROGRESS) },
                            label = { Text(stringResource(R.string.filter_in_progress, state.inProgressCount), fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StaffColors.Coffee,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = StaffColors.Coffee
                            )
                        )
                        // Capsula de Filtro "Listos (2)"
                        FilterChip(
                            selected = state.filter == OrderFilter.READY,
                            onClick = { onFilterChange(OrderFilter.READY) },
                            label = { Text(stringResource(R.string.filter_ready, state.readyCount), fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StaffColors.Coffee,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = StaffColors.Coffee
                            )
                        )
                        // Capsula de Filtro "Todos"
                        FilterChip(
                            selected = state.filter == OrderFilter.ALL,
                            onClick = { onFilterChange(OrderFilter.ALL) },
                            label = { Text(stringResource(R.string.filter_all), fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StaffColors.Coffee,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = StaffColors.Coffee
                            )
                        )
                    }

                    // Lista scrolleable de pedidos por mesa
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.orders, key = { it.id }) { order ->
                            OrderCard(order = order, onClick = { onOrderClick(order.id) })
                        }
                    }
                }
            }
        }
    }
}

// Tarjeta individual de pedido de cada mesa
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
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Encabezado de la tarjeta: Nombre de Mesa + Badge de Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Numero o Etiqueta de Mesa (ej: Mesa 4)
                Text(
                    text = stringResource(R.string.table_item_label, order.tableLabel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                // Badge / Pastilla de estado del pedido (ej. "En preparación", "En espera")
                OrderStatusBadge(status = order.status)
            }

            // Horario del pedido (ej: 12:34)
            val timeText = if (order.createdAt.length >= 16) order.createdAt.substring(11, 16) else "12:34"
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de items / productos solicitados en este pedido
            order.items.forEach { item ->
                Text(
                    text = "${item.quantity} x ${item.productName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = StaffColors.Coffee,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Badge de estado con color e indicacion visual
@Composable
fun OrderStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "PENDING_REVIEW" -> Triple(StaffColors.StatusPendingBg, StaffColors.StatusPendingText, "En espera")
        "ACCEPTED" -> Triple(StaffColors.StatusInPrepBg, StaffColors.StatusInPrepText, "En preparación")
        "READY" -> Triple(StaffColors.StatusReadyBg, StaffColors.StatusReadyText, "Listo")
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// Vista previa para Android Studio (Split / Design tab)
@Preview(showBackground = true)
@Composable
fun InboxScreenPreview() {
    SomphieTheme {
        InboxContent(
            state = InboxUiState.Success(
                orders = listOf(
                    StaffOrder(
                        id = "1",
                        code = "#001",
                        sessionId = "s1",
                        tableLabel = "4",
                        status = "ACCEPTED",
                        createdAt = "2026-10-18T12:34:00Z",
                        items = listOf(
                            StaffOrderItem("i1", "p1", "Latte", 2, 320, 640, "ACCEPTED"),
                            StaffOrderItem("i2", "p2", "Tostado mixto", 1, 450, 450, "ACCEPTED")
                        ),
                        totalMinor = 1090
                    ),
                    StaffOrder(
                        id = "2",
                        code = "#002",
                        sessionId = "s2",
                        tableLabel = "7",
                        status = "ACCEPTED",
                        createdAt = "2026-10-18T12:40:00Z",
                        items = listOf(
                            StaffOrderItem("i3", "p3", "Cappuccino", 1, 350, 350, "ACCEPTED"),
                            StaffOrderItem("i4", "p4", "Medialuna", 1, 200, 200, "ACCEPTED")
                        ),
                        totalMinor = 550
                    ),
                    StaffOrder(
                        id = "3",
                        code = "#003",
                        sessionId = "s3",
                        tableLabel = "10",
                        status = "PENDING_REVIEW",
                        createdAt = "2026-10-18T12:52:00Z",
                        items = listOf(
                            StaffOrderItem("i5", "p5", "Café espresso", 3, 250, 750, "PENDING_REVIEW"),
                            StaffOrderItem("i6", "p2", "Tostado mixto", 1, 450, 450, "PENDING_REVIEW")
                        ),
                        totalMinor = 1200
                    ),
                    StaffOrder(
                        id = "4",
                        code = "#004",
                        sessionId = "s5",
                        tableLabel = "12",
                        status = "PENDING_REVIEW",
                        createdAt = "2026-10-18T12:52:00Z",
                        items = listOf(
                            StaffOrderItem("i5", "p5", "Café espresso", 3, 250, 750, "PENDING_REVIEW"),
                            StaffOrderItem("i6", "p2", "Tostado mixto", 1, 450, 450, "PENDING_REVIEW")
                        ),
                        totalMinor = 1200
                    ),
                    StaffOrder(
                        id = "5",
                        code = "#005",
                        sessionId = "s5",
                        tableLabel = "2",
                        status = "EXPIRED",
                        createdAt = "2026-10-18T12:52:00Z",
                        items = listOf(
                            StaffOrderItem("i5", "p5", "Café espresso", 3, 250, 750, "EXPIRED"),
                            StaffOrderItem("i6", "p2", "Tostado mixto", 1, 450, 450, "EXPIRED")
                        ),
                        totalMinor = 1200
                    ),
                    StaffOrder(
                        id = "6",
                        code = "#006",
                        sessionId = "s6",
                        tableLabel = "1",
                        status = "PENDING_REVIEW",
                        createdAt = "2026-10-18T12:52:00Z",
                        items = listOf(
                            StaffOrderItem("i5", "p5", "Café espresso", 3, 250, 750, "PENDING_REVIEW"),
                            StaffOrderItem("i6", "p2", "Tostado mixto", 1, 450, 450, "PENDING_REVIEW")
                        ),
                        totalMinor = 1200
                    )
                ),
                inProgressCount = 5,
                readyCount = 2,
                filter = OrderFilter.IN_PROGRESS
            ),
            onNavigateBack = {},
            onFilterChange = {},
            onOrderClick = {}
        )
    }
}
