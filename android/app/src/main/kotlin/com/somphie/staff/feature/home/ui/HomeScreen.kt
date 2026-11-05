package com.somphie.staff.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.somphie.staff.core.design.StaffColors
import com.somphie.staff.core.network.StaffProfile
import com.somphie.staff.feature.orders.ui.InboxScreen
import com.somphie.staff.feature.orders.ui.InboxViewModel
import com.somphie.staff.feature.orders.ui.OrderDetailScreen
import com.somphie.staff.feature.orders.ui.OrderDetailViewModel
import com.somphie.staff.feature.tables.ui.TablesBoardScreen
import com.somphie.staff.feature.tables.ui.TablesViewModel

// Pantalla principal / Panel del turno
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: StaffProfile,
    onNavigateToKitchen: () -> Unit,
    onNavigateToBar: () -> Unit,
    onNavigateToProfile: () -> Unit,
    pendingOrdersCount: Int = 5, // Contador configurable de pedidos pendientes
    activeTablesCount: Int = 3  // Contador configurable de mesas activas
) {
    // Estado local de pestaña activa: 0 = Inicio, 1 = Pedidos, 2 = Mesas, 3 = Perfil
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Sub-estado para abrir el detalle de un pedido
    var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        // Barra superior (TopAppBar) con el menu
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Somphie Zúrich",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                // Boton de menu hamburguesa
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        // Barra de navegacion inferior (4 pestañas: Inicio, Pedidos, Mesas, Perfil)
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Pestaña 0: Inicio
                NavigationBarItem(
                    selected = selectedTab == 0 && selectedOrderId == null,
                    onClick = {
                        selectedTab = 0
                        selectedOrderId = null
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") }
                )

                // Pestaña 1: Pedidos
                NavigationBarItem(
                    selected = (selectedTab == 1 || selectedOrderId != null),
                    onClick = {
                        selectedTab = 1
                        selectedOrderId = null
                    },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Pedidos") },
                    label = { Text("Pedidos") }
                )

                // Pestaña 2: Mesas
                NavigationBarItem(
                    selected = selectedTab == 2 && selectedOrderId == null,
                    onClick = {
                        selectedTab = 2
                        selectedOrderId = null
                    },
                    icon = { Icon(Icons.Default.TableBar, contentDescription = "Mesas") },
                    label = { Text("Mesas") }
                )

                // Pestaña 3: Perfil
                NavigationBarItem(
                    selected = selectedTab == 3 && selectedOrderId == null,
                    onClick = {
                        selectedTab = 3
                        selectedOrderId = null
                        onNavigateToProfile()
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") }
                )
            }
        }
    ) { innerPadding ->
        // Contenido principal que cambia segun la pestaña o sub-pantalla
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedOrderId != null) {
                // Vista de detalle del pedido seleccionado
                val orderDetailViewModel: OrderDetailViewModel = hiltViewModel()
                OrderDetailScreen(
                    orderId = selectedOrderId!!,
                    viewModel = orderDetailViewModel,
                    onNavigateBack = { selectedOrderId = null }
                )
            } else {
                when (selectedTab) {
                    0 -> DashboardContent(
                        profile = profile,
                        pendingOrdersCount = pendingOrdersCount,
                        activeTablesCount = activeTablesCount,
                        onNavigateToOrders = { selectedTab = 1 },
                        onNavigateToTables = { selectedTab = 2 },
                        onNavigateToKitchen = onNavigateToKitchen,
                        onNavigateToBar = onNavigateToBar
                    )
                    1 -> {
                        // Vista de la lista de pedidos
                        val inboxViewModel: InboxViewModel = hiltViewModel()
                        InboxScreen(
                            viewModel = inboxViewModel,
                            onNavigateBack = { selectedTab = 0 },
                            onOrderClick = { orderId -> selectedOrderId = orderId }
                        )
                    }
                    2 -> {
                        // Vista del tablero de mesas
                        val tablesViewModel: TablesViewModel = hiltViewModel()
                        TablesBoardScreen(
                            viewModel = tablesViewModel,
                            onNavigateBack = { selectedTab = 0 }
                        )
                    }
                }
            }
        }
    }
}

// Contenido principal del Panel de Inicio
@Composable
private fun DashboardContent(
    profile: StaffProfile,
    pendingOrdersCount: Int,
    activeTablesCount: Int,
    onNavigateToOrders: () -> Unit,
    onNavigateToTables: () -> Unit,
    onNavigateToKitchen: () -> Unit,
    onNavigateToBar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fila de encabezado: Saludo a la izquierda + Chip de estacion a la derecha
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Saludo con nombre del usuario
            Column {
                Text(
                    text = "Hola, ${profile.displayName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "¡Buen turno!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chip de estacion activa (ej. Cocina)
            AssistChip(
                onClick = {},
                label = { Text("Cocina", fontSize = 12.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = StaffColors.Sage,
                    labelColor = StaffColors.Cream
                )
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Grilla de accesos rapidos 2x2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Fila 1: Pedidos en curso + Mesas activas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Pedidos (con badge contador)
                HomeDashboardCard(
                    title = "Pedidos",
                    subtitle = "(en curso)",
                    badgeCount = pendingOrdersCount,
                    icon = Icons.Default.ReceiptLong,
                    onClick = onNavigateToOrders,
                    modifier = Modifier.weight(1f)
                )

                // Card 2: Mesas (con badge contador)
                HomeDashboardCard(
                    title = "Mesas",
                    subtitle = "(activas)",
                    badgeCount = activeTablesCount,
                    icon = Icons.Default.TableBar,
                    onClick = onNavigateToTables,
                    modifier = Modifier.weight(1f)
                )
            }

            // Fila 2: Cocina + Barra
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 3: Cocina
                HomeDashboardCard(
                    title = "Cocina",
                    subtitle = "(preparación)",
                    badgeCount = null,
                    icon = Icons.Default.SoupKitchen,
                    onClick = onNavigateToKitchen,
                    modifier = Modifier.weight(1f)
                )

                // Card 4: Barra
                HomeDashboardCard(
                    title = "Barra",
                    subtitle = "(preparación)",
                    badgeCount = null,
                    icon = Icons.Default.LocalCafe,
                    onClick = onNavigateToBar,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Card del turno actual
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Horario del turno
                Column {
                    Text("Turno actual", style = MaterialTheme.typography.labelMedium)
                    Text("10:00 - 18:00", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                // Badge con el estado del turno
                SuggestionChip(
                    onClick = {},
                    label = { Text("En curso", color = StaffColors.Sage) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = StaffColors.SoftSageBg
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Tarjeta reusable para los accesos rapidos del dashboard
@Composable
private fun HomeDashboardCard(
    title: String,
    subtitle: String,
    badgeCount: Int?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(152.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Badge / contador flotante (notificacion si count > 0)
            if (badgeCount != null && badgeCount > 0) {
                Badge(
                    containerColor = StaffColors.BadgeBrown,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text("$badgeCount", fontWeight = FontWeight.Bold)
                }
            }
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Icono del acceso rapido
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                // Titulo y subtitulo
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// Preview del contenido principal para Android Studio (Vista Previa Visual)
@Composable
fun DashboardContentPreview() {
    com.somphie.staff.core.design.SomphieTheme {
        DashboardContent(
            profile = StaffProfile(
                id = "demo-id",
                email = "lucas@somphie.example",
                displayName = "Franquito",
                roles = listOf("WAITER"),
                permissions = emptyList(),
                stations = emptyList(),
                mustChangePassword = false
            ),
            pendingOrdersCount = 5,
            activeTablesCount = 3,
            onNavigateToOrders = {},
            onNavigateToTables = {},
            onNavigateToKitchen = {},
            onNavigateToBar = {}
        )
    }
}