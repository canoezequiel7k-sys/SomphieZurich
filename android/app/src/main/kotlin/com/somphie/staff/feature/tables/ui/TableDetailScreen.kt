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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.somphie.staff.core.network.Bill
import com.somphie.staff.core.network.StaffOrder
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
                    // Boton de volver atras
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    },
                    // Menu de tres puntos a la derecha
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
                .padding(16.dp)
        ) {
            when (state) {
                // Indicador de carga
                is TableDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                // Mensaje de error
                is TableDetailUiState.Error -> Text(text = state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                is TableDetailUiState.Success -> {
                    val detail = state.sessionDetail
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Pastilla / Badge de estado de la mesa (ej: En preparación)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    color = StaffColors.Sage,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = "En preparación",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Card contenedora con los "Pedidos de la mesa"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Titulo de la sección de pedidos
                                    Text(
                                        text = "Pedidos de la mesa",
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

                        // Boton principal al pie "Ver detalles"
                        Button(
                            onClick = onViewFullDetails,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StaffColors.Caramel)
                        ) {
                            Text(
                                text = "Ver detalles",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Preview para Android Studio (Split / Design tab)
@Preview(showBackground = true)
@Composable
fun TableDetailScreenPreview() {
    SomphieTheme {
        TableDetailContent(
            state = TableDetailUiState.Success(
                sessionDetail = StaffSessionDetail(
                    session = StaffSession(
                        id = "s1",
                        status = "OPEN",
                        tableLabel = "Mesa 4",
                        openedAt = "2026-10-18T12:00:00Z"
                    ),
                    orders = listOf(
                        StaffOrder(
                            id = "o1",
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
                        )
                    ),
                    bill = Bill(balanceMinor = 1090)
                ),
                tableLabel = "Mesa 4"
            ),
            tableLabel = "Mesa 4",
            onNavigateBack = {},
            onViewFullDetails = {},
            showTopBar = true
        )
    }
}
