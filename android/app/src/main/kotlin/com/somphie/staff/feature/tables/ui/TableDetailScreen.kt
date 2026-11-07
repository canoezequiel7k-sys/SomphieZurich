package com.somphie.staff.feature.tables.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.somphie.staff.core.network.StaffOrderItem
import com.somphie.staff.core.network.StaffSession
import com.somphie.staff.core.network.StaffSessionDetail
import com.somphie.staff.feature.orders.ui.OrderStatusBadge
import com.somphie.staff.feature.orders.ui.formatMoney

// Pantalla de Detalle de Mesa
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableDetailScreen(
    sessionId: String,
    tableLabel: String = "Mesa",
    viewModel: TableDetailViewModel,
    onNavigateBack: () -> Unit = {},
    onViewFullDetails: () -> Unit = {},
    onCreateOrderForTable: () -> Unit = {},
    showTopBar: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadTableDetail(sessionId, tableLabel)
    }

    TableDetailContent(
        state = state,
        tableLabel = tableLabel,
        onNavigateBack = onNavigateBack,
        onViewFullDetails = onViewFullDetails,
        onCreateOrderForTable = onCreateOrderForTable,
        showTopBar = showTopBar
    )
}

// Contenido desacoplado de la pantalla de Detalle de Mesa para soportar Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableDetailContent(
    state: TableDetailUiState,
    tableLabel: String,
    onNavigateBack: () -> Unit,
    onViewFullDetails: () -> Unit,
    onCreateOrderForTable: () -> Unit = {},
    showTopBar: Boolean = true
) {
    Scaffold(
        // Barra superior opcional con titulo de Mesa y boton de opciones
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        val titleText = if (tableLabel.startsWith("Mesa")) tableLabel else stringResource(R.string.table_item_label, tableLabel)
                        Text(
                            text = titleText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones"
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
        ) {
            when (state) {
                // Indicador de carga centrado
                is TableDetailUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = StaffColors.Coffee)
                }

                // Mensaje de error
                is TableDetailUiState.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }

                // Contenido principal cuando se carga con exito
                is TableDetailUiState.Success -> {
                    val detail = state.sessionDetail
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Badge con el estado de la mesa (ej: En preparación, Ocupada)
                            TableStatusBadge(status = detail.session.status)

                            // Card contenedora con los pedidos activos de la mesa
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Titulo de la lista de pedidos
                                    Text(
                                        text = stringResource(R.string.orders_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = StaffColors.Coffee
                                    )

                                    HorizontalDivider()

                                    // Iteración sobre los productos de los pedidos activos
                                    val allItems = detail.orders.flatMap { it.items }
                                    if (allItems.isEmpty()) {
                                        Text(
                                            text = "No hay productos pedidos en esta mesa.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        allItems.forEach { item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Punto viñeta marron
                                                    Box(
                                                        modifier = Modifier
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
                                                            fontSize = 15.sp
                                                        )
                                                        // Opciones/modificadores si existen
                                                        if (item.optionsSummary.isNotBlank()) {
                                                            Text(
                                                                text = item.optionsSummary,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Badge chico de estado del item
                                                    OrderStatusBadge(status = item.status)

                                                    // Precio formateado (ej: $6.40)
                                                    Text(
                                                        text = formatMoney(item.totalMinor),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = StaffColors.Coffee,
                                                        fontSize = 15.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Fila del Total parcial / Saldo de la mesa
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total parcial",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = StaffColors.Coffee
                                )
                                Text(
                                    text = formatMoney(detail.bill.balanceMinor.takeIf { it > 0 } ?: detail.orders.sumOf { it.totalMinor }),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = StaffColors.Caramel,
                                    fontSize = 22.sp
                                )
                            }
                        }

                        // Botones al pie: "Ver detalles" y "Tomar Pedido"
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onViewFullDetails,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StaffColors.Caramel)
                            ) {
                                Text(
                                    text = "Ver detalles / Cobrar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            OutlinedButton(
                                onClick = onCreateOrderForTable,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = StaffColors.Coffee)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tomar Pedido",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = StaffColors.Coffee
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Badge de estado de la mesa (ej: En preparación, Libre)
@Composable
private fun TableStatusBadge(status: String) {
    val (bgColor, textColor, text) = when (status) {
        "OPEN" -> Triple(StaffColors.SoftSageBg, StaffColors.Sage, stringResource(R.string.status_in_preparation))
        "CLOSED" -> Triple(Color.LightGray, Color.DarkGray, "Cerrada")
        else -> Triple(StaffColors.SoftSageBg, StaffColors.Sage, status)
    }

    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

// Preview para Android Studio (Split / Design tab)
@Preview(showBackground = true)
@Composable
fun TableDetailScreenPreview() {
    SomphieTheme {
        TableDetailContent(
            state = TableDetailUiState.Success(
                tableLabel = "Mesa 4",
                sessionDetail = StaffSessionDetail(
                    session = StaffSession(
                        id = "s1",
                        status = "OPEN",
                        tableLabel = "Mesa 4",
                        openedAt = "12:34"
                    ),
                    orders = listOf(
                        com.somphie.staff.core.network.StaffOrder(
                            id = "o1",
                            code = "ORD-001",
                            sessionId = "s1",
                            tableLabel = "Mesa 4",
                            status = "ACCEPTED",
                            createdAt = "12:34",
                            totalMinor = 1090,
                            items = listOf(
                                StaffOrderItem(
                                    id = "i1",
                                    productId = "p1",
                                    productName = "Latte",
                                    quantity = 2,
                                    unitPriceMinor = 320,
                                    totalMinor = 640,
                                    status = "IN_PREPARATION",
                                    optionsSummary = "Leche de almendra"
                                ),
                                StaffOrderItem(
                                    id = "i2",
                                    productId = "p2",
                                    productName = "Tostado mixto",
                                    quantity = 1,
                                    unitPriceMinor = 450,
                                    totalMinor = 450,
                                    status = "IN_PREPARATION"
                                )
                            )
                        )
                    ),
                    bill = com.somphie.staff.core.network.Bill(
                        currency = "ARS",
                        chargesMinor = 1090,
                        balanceMinor = 1090
                    )
                )
            ),
            tableLabel = "Mesa 4",
            onNavigateBack = {},
            onViewFullDetails = {}
        )
    }
}
