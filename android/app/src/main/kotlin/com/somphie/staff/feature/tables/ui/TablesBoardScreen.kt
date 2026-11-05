package com.somphie.staff.feature.tables.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somphie.staff.R
import com.somphie.staff.core.network.StaffTable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesBoardScreen(
    viewModel: TablesViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tables_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
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
                is TablesUiState.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is TablesUiState.Error -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(current.message, color = MaterialTheme.colorScheme.error)
                }
                is TablesUiState.Success -> {
                    // Cuadrícula 3xN de Mesas
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

            // Leyenda Visual al Pie
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFF61715B), label = stringResource(R.string.status_free))
                LegendItem(color = Color(0xFFD4A373), label = stringResource(R.string.status_occupied))
                LegendItem(color = Color(0xFF3A5A40), label = stringResource(R.string.status_in_preparation))
            }
        }
    }
}

@Composable
private fun TableCard(table: StaffTable) {
    val (bgColor, textColor, statusText) = when (table.displayStatus) {
        "FREE" -> Triple(Color(0xFFF0EBE1), Color(0xFF3A2B22), stringResource(R.string.status_free))
        "OCCUPIED" -> Triple(Color(0xFFE6BEA5), Color(0xFF5C3317), stringResource(R.string.status_occupied))
        "PENDING_ORDERS" -> Triple(Color(0xFFD4A373), Color(0xFFFFFFFF), stringResource(R.string.status_pending_orders))
        "IN_SERVICE" -> Triple(Color(0xFF61715B), Color(0xFFFFFFFF), stringResource(R.string.status_in_preparation))
        "AWAITING_PAYMENT" -> Triple(Color(0xFF945C34), Color(0xFFFFFFFF), stringResource(R.string.status_awaiting_payment))
        "READY_TO_CLOSE" -> Triple(Color(0xFF59696E), Color(0xFFFFFFFF), stringResource(R.string.status_ready_to_close))
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
            Text(
                text = table.label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

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