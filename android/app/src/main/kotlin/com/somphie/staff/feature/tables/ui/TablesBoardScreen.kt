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
import com.somphie.staff.core.network.StaffTable

// Pantalla de Tablero de Mesas
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesBoardScreen(
    viewModel: TablesViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        // Barra superior con titulo, boton de volver y boton de refrescar
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tables_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Boton de refrescar tablero
                    IconButton(onClick = { viewModel.loadTables() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            when (val current = state) {
                // Indicador de carga
                is TablesUiState.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                // Mensaje de error
                is TablesUiState.Error -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(current.message, color = MaterialTheme.colorScheme.error)
                }
                is TablesUiState.Success -> {
                    // Cuadrícula 3xN con la lista de mesas
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(current.tables, key = { it.tableId }) { table ->
                            TableCard(table = table)
                        }
                    }
                }
            }

            // Leyenda visual al pie (Colores por estado: Libre, Ocupada, En preparación)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = com.somphie.staff.core.design.StaffColors.Sage, label = stringResource(R.string.status_free))
                LegendItem(color = com.somphie.staff.core.design.StaffColors.StatusInPrepBg, label = stringResource(R.string.status_occupied))
                LegendItem(color = com.somphie.staff.core.design.StaffColors.BadgeBrown, label = stringResource(R.string.status_in_preparation))
            }
        }
    }
}

// Tarjeta individual de mesa
@Composable
private fun TableCard(table: StaffTable) {
    // Mapeo de colores y textos segun el estado contractual de la mesa
    val (bgColor, textColor, statusText) = when (table.displayStatus) {
        "FREE" -> Triple(com.somphie.staff.core.design.StaffColors.Cream, com.somphie.staff.core.design.StaffColors.Coffee, stringResource(R.string.status_free))
        "OCCUPIED" -> Triple(com.somphie.staff.core.design.StaffColors.StatusPendingBg, com.somphie.staff.core.design.StaffColors.StatusPendingText, stringResource(R.string.status_occupied))
        "PENDING_ORDERS" -> Triple(com.somphie.staff.core.design.StaffColors.StatusInPrepBg, com.somphie.staff.core.design.StaffColors.StatusInPrepText, stringResource(R.string.status_pending_orders))
        "IN_SERVICE" -> Triple(com.somphie.staff.core.design.StaffColors.Sage, Color.White, stringResource(R.string.status_in_preparation))
        "AWAITING_PAYMENT" -> Triple(com.somphie.staff.core.design.StaffColors.Caramel, Color.White, stringResource(R.string.status_awaiting_payment))
        "READY_TO_CLOSE" -> Triple(com.somphie.staff.core.design.StaffColors.BlueGray, Color.White, stringResource(R.string.status_ready_to_close))
        else -> Triple(Color.LightGray, Color.Black, table.displayStatus)
    }

    Card(
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
            // Numero o etiqueta de la mesa
            Text(
                text = table.label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Texto del estado de la mesa
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

// Item de leyenda con punto de color y texto
@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
