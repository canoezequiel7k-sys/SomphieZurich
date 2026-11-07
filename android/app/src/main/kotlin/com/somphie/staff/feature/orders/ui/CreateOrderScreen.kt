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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somphie.staff.core.design.SomphieTheme
import com.somphie.staff.core.design.StaffColors

// Pantalla para tomar pedido a nombre del cliente (SC-S-12)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    sessionId: String,
    tableLabel: String = "Mesa",
    viewModel: CreateOrderViewModel,
    onNavigateBack: () -> Unit = {},
    onOrderCreated: () -> Unit = {},
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()

    CreateOrderContent(
        tableLabel = tableLabel,
        catalog = viewModel.demoCatalog,
        uiState = uiState,
        onIncrement = { viewModel.incrementProduct(it) },
        onDecrement = { viewModel.decrementProduct(it) },
        onNotesChange = { viewModel.updateNotes(it) },
        onSubmit = { viewModel.submitOrder(sessionId, onOrderCreated) },
        onNavigateBack = onNavigateBack,
        showTopBar = showTopBar
    )
}

// Contenido desacoplado para vista previa en Android Studio (@Preview)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderContent(
    tableLabel: String,
    catalog: List<CatalogProductItem>,
    uiState: CreateOrderUiState,
    onIncrement: (String) -> Unit = {},
    onDecrement: (String) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    showTopBar: Boolean = true
) {
    // Cálculo del importe total acumulado
    val totalAmountMinor = catalog.sumOf { product ->
        val qty = uiState.quantities[product.id] ?: 0
        product.priceMinor * qty
    }
    val totalItemCount = uiState.quantities.values.sum()

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text("Tomar Pedido - $tableLabel", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Cartel de error si ocurre
            if (uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }

            // Lista de productos de la carta
            Text("Selecciona los productos:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StaffColors.Coffee)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(catalog, key = { it.id }) { product ->
                    val qty = uiState.quantities[product.id] ?: 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StaffColors.Coffee)
                                Text(formatMoney(product.priceMinor), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(
                                modifier = Modifier.width(120.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                // Selector de cantidad (+ / -)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (qty > 0) {
                                        IconButton(
                                            onClick = { onDecrement(product.id) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(StaffColors.SoftSageBg, shape = CircleShape)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Menos", tint = StaffColors.Coffee, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onIncrement(product.id) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(StaffColors.Caramel, shape = CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Más", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Cantidad:")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("$qty", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Aclaración / Nota del pedido
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                label = { Text("Aclaración / Notas (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Botón principal Enviar Pedido
            Button(
                onClick = onSubmit,
                enabled = totalItemCount > 0 && !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StaffColors.Coffee,
                    contentColor = Color.White
                )
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enviar Pedido ($totalItemCount p. · ${formatMoney(totalAmountMinor)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// Preview para Android Studio (Split / Design tab)
@Preview(showBackground = true)
@Composable
fun CreateOrderScreenPreview() {
    SomphieTheme {
        CreateOrderContent(
            tableLabel = "Mesa 4",
            catalog = listOf(
                CatalogProductItem("p-latte", "Latte", 320, "Cafetería"),
                CatalogProductItem("p-tostado", "Tostado mixto", 450, "Comidas")
            ),
            uiState = CreateOrderUiState(
                quantities = mapOf("p-latte" to 2, "p-tostado" to 1)
            )
        )
    }
}
