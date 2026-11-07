package com.somphie.staff.feature.tables.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.somphie.staff.core.network.StaffTable

// Pantalla de Tablero de Mesas
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesBoardScreen(
    viewModel: TablesViewModel,
    onNavigateBack: () -> Unit,
    onTableClick: (sessionId: String) -> Unit = {},
    showTopBar: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()

    TablesBoardContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onRefresh = { viewModel.loadTables() },
        onTableClick = onTableClick,
        showTopBar = showTopBar
    )
}

// Contenido desacoplado del tablero de Mesas para soportar Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesBoardContent(
    state: TablesUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onTableClick: (sessionId: String) -> Unit = {},
    showTopBar: Boolean = true
) {
    Scaffold(
        // Barra superior opcional (solo si showTopBar es true)
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(stringResource(R.string.tables_title), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        // Boton de refrescar tablero
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            when (state) {
                // Indicador de carga centrado
                is TablesUiState.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                // Mensaje de error
                is TablesUiState.Error -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                is TablesUiState.Success -> {
                    // Cuadrícula 3xN con las tarjetas de cada mesa
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.tables, key = { it.tableId }) { table ->
                            TableCard(table = table, onClick = { table.sessionId?.let(onTableClick) })
                        }
                    }
                }
            }

            // Leyenda visual al pie con circulos de color por estado (Libre, Ocupada, En preparación)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = StaffColors.Sage, label = stringResource(R.string.status_free))
                LegendItem(color = StaffColors.StatusPendingText, label = stringResource(R.string.status_occupied))
                LegendItem(color = StaffColors.StatusReadyBg, label = stringResource(R.string.status_in_preparation))
            }
        }
    }
}

// Tarjeta individual de mesa
@Composable
private fun TableCard(table: StaffTable, onClick: () -> Unit = {}) {
    // Mapeo de colores y textos segun el estado contractual de la mesa
    val (bgColor, textColor, statusText) = when (table.displayStatus) {
        "FREE" -> Triple(Color.White, StaffColors.Coffee, stringResource(R.string.status_free))
        "OCCUPIED" -> Triple(StaffColors.StatusPendingBg, StaffColors.StatusPendingText, stringResource(R.string.status_occupied))
        "PENDING_ORDERS" -> Triple(StaffColors.StatusInPrepBg, StaffColors.StatusInPrepText, stringResource(R.string.status_pending_orders))
        "IN_SERVICE" -> Triple(StaffColors.Sage, Color.White, stringResource(R.string.status_in_preparation))
        "AWAITING_PAYMENT" -> Triple(StaffColors.Caramel, Color.White, stringResource(R.string.status_awaiting_payment))
        "READY_TO_CLOSE" -> Triple(StaffColors.BlueGray, Color.White, stringResource(R.string.status_ready_to_close))
        else -> Triple(Color.LightGray, Color.Black, table.displayStatus)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(0.85f)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Numero o etiqueta de la mesa (ej: 1, 2, 4)
            Text(
                text = table.label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Texto descriptivo del estado de la mesa (ej: Libre, Ocupada, En preparación)
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.9f)
            )
        }
    }
}

// Item de leyenda con circulo de color y texto
@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Circulo indicador de color
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        // Etiqueta del estado
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = StaffColors.Coffee)
    }
}

// Preview para Android Studio (Split / Design tab)
@Preview(showBackground = true)
@Composable
fun TablesBoardScreenPreview() {
    SomphieTheme {
        TablesBoardContent(
            state = TablesUiState.Success(
                tables = listOf(
                    StaffTable("t1", "1", "FREE"),
                    StaffTable("t2", "2", "OCCUPIED"),
                    StaffTable("t3", "3", "OCCUPIED"),
                    StaffTable("t4", "4", "IN_SERVICE"),
                    StaffTable("t5", "5", "FREE"),
                    StaffTable("t6", "6", "OCCUPIED"),
                    StaffTable("t7", "7", "OCCUPIED"),
                    StaffTable("t8", "8", "FREE"),
                    StaffTable("t9", "9", "IN_SERVICE"),
                    StaffTable("t10", "10", "FREE"),
                    StaffTable("t11", "11", "OCCUPIED"),
                    StaffTable("t12", "12", "FREE")
                )
            ),
            onNavigateBack = {},
            onRefresh = {},
            showTopBar = true
        )
    }
}
