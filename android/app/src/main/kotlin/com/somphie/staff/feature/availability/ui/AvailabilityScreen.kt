package com.somphie.staff.feature.availability.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.somphie.staff.core.network.StaffProductAvailability
import com.somphie.staff.feature.orders.ui.formatMoney

// Pantalla para activar/desactivar la disponibilidad de productos (SC-S-11)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityScreen(
    viewModel: AvailabilityViewModel,
    onNavigateBack: () -> Unit = {},
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()

    AvailabilityContent(
        uiState = uiState,
        onSearchChange = { viewModel.updateSearchQuery(it) },
        onToggleAvailability = { viewModel.toggleProductAvailability(it) },
        onNavigateBack = onNavigateBack,
        showTopBar = showTopBar
    )
}

// Contenido desacoplado para vista previa en Android Studio (@Preview)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityContent(
    uiState: AvailabilityUiState,
    onSearchChange: (String) -> Unit = {},
    onToggleAvailability: (productId: String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    showTopBar: Boolean = true
) {
    val filteredProducts = uiState.products.filter {
        it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                it.category.contains(uiState.searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text("Disponibilidad de Carta", fontWeight = FontWeight.Bold, fontSize = 20.sp)
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

            // Buscador por nombre o categoría
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Buscar producto...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Indicador de éxito / confirmación
            if (uiState.successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StaffColors.SoftSageBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.successMessage,
                        color = StaffColors.Sage,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StaffColors.Coffee)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.productId }) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = StaffColors.Coffee
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = product.category,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = formatMoney(product.priceMinor),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = StaffColors.Caramel
                                        )
                                    }

                                    // Badge de estado (Disponible vs Agotado)
                                    Surface(
                                        color = if (product.available) StaffColors.SoftSageBg else Color.LightGray.copy(alpha = 0.5f),
                                        shape = MaterialTheme.shapes.extraSmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = if (product.available) "Disponible" else "Agotado",
                                            color = if (product.available) StaffColors.Sage else Color.DarkGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Switch para alternar estado (Disponible / Agotado)
                                Switch(
                                    checked = product.available,
                                    onCheckedChange = { onToggleAvailability(product.productId) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = StaffColors.Coffee,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color.LightGray
                                    )
                                )
                            }
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
fun AvailabilityScreenPreview() {
    SomphieTheme {
        AvailabilityContent(
            uiState = AvailabilityUiState(
                products = listOf(
                    StaffProductAvailability("p-latte", "Latte", "Cafetería", true, 320),
                    StaffProductAvailability("p-medialuna", "Medialuna de manteca", "Comidas", false, 180)
                )
            )
        )
    }
}
