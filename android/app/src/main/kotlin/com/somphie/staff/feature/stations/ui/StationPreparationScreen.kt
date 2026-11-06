package com.somphie.staff.feature.stations.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.somphie.staff.R
import com.somphie.staff.core.design.SomphieTheme
import com.somphie.staff.core.design.StaffColors
import com.somphie.staff.core.network.StaffProfile
import com.somphie.staff.core.network.StationQueueItem
import com.somphie.staff.feature.stations.data.PendingPreparationAction
import com.somphie.staff.feature.stations.data.PreparationAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val QUEUE_REFRESH_INTERVAL_MS = 5_000L

// Pantalla de Preparacion por Estacion (Cocina / Barra)
@Composable
fun StationPreparationScreen(
    profile: StaffProfile,
    onSignOut: () -> Unit,
    viewModel: StationQueueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    var itemToCancel by remember { mutableStateOf<StationQueueItem?>(null) }
    val permissionSet = remember(profile.permissions) { profile.permissions.toSet() }

    LaunchedEffect(permissionSet) {
        if ("item.prepare" in permissionSet) viewModel.loadStations()
    }

    // Polling REST automatico cada 5 segundos cuando la pantalla esta en primer plano
    LaunchedEffect(lifecycle, state.selectedStation?.id, state.filter) {
        if ("item.prepare" !in permissionSet || state.selectedStation == null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshQueue()
            while (isActive) {
                delay(QUEUE_REFRESH_INTERVAL_MS)
                viewModel.refreshQueue()
            }
        }
    }

    StationPreparationContent(
        state = state,
        profileName = profile.displayName,
        hasPermission = "item.prepare" in permissionSet,
        context = context,
        onSignOut = onSignOut,
        onSelectStation = viewModel::selectStation,
        onSelectFilter = viewModel::selectFilter,
        onRefreshQueue = viewModel::refreshQueue,
        onRetryStations = viewModel::retryStations,
        onPerformAction = { item, action ->
            if (action == PreparationAction.CANCEL) itemToCancel = item
            else viewModel.performAction(item, action)
        },
        onRetryAction = { pending -> viewModel.retryAction(pending.idempotencyKey) },
        onLoadMore = viewModel::loadMore
    )

    // Dialogo emergente para cancelar producto con motivo obligatorio
    itemToCancel?.let { item ->
        CancelItemDialog(
            onDismiss = { itemToCancel = null },
            onConfirm = { reason ->
                itemToCancel = null
                viewModel.performAction(item, PreparationAction.CANCEL, reason)
            },
        )
    }
}

// Contenido desacoplado para soportar Previews en Android Studio
@Composable
fun StationPreparationContent(
    state: StationQueueUiState,
    profileName: String,
    hasPermission: Boolean,
    context: android.content.Context,
    onSignOut: () -> Unit,
    onSelectStation: (com.somphie.staff.core.network.Station) -> Unit,
    onSelectFilter: (QueueFilter) -> Unit,
    onRefreshQueue: () -> Unit,
    onRetryStations: () -> Unit,
    onPerformAction: (StationQueueItem, PreparationAction) -> Unit,
    onRetryAction: (PendingPreparationAction) -> Unit,
    onLoadMore: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // Encabezado superior con Icono de Cocina/Barra, Titulo y Boton de Cerrar Sesion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono de Cocina o Barra
                    Icon(
                        imageVector = if (state.selectedStation?.code == "BAR") Icons.Default.LocalCafe else Icons.Default.SoupKitchen,
                        contentDescription = null,
                        tint = StaffColors.Coffee,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Column {
                        // Titulo de la Estacion (Cocina / Barra)
                        Text(
                            text = state.selectedStation?.name ?: stringResource(R.string.kitchen_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StaffColors.Coffee
                        )
                    }
                }
                // Boton de cerrar sesion
                TextButton(onClick = onSignOut) {
                    Text(stringResource(R.string.logout_action), color = StaffColors.Caramel)
                }
            }

            if (!hasPermission) {
                Text(
                    stringResource(R.string.preparation_permission_required),
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                return@Column
            }

            state.stationErrorCode?.let { code ->
                ErrorPanel(code = code, onRetry = onRetryStations)
            }

            when {
                state.isLoadingStations -> CenteredProgress()
                state.stationErrorCode != null -> Unit
                state.stations.isEmpty() -> Text(
                    stringResource(R.string.stations_empty),
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                else -> {
                    // Selector horizontal de estaciones asignadas
                    if (state.stations.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.stations.forEach { station ->
                                FilterChip(
                                    selected = station.id == state.selectedStation?.id,
                                    onClick = { onSelectStation(station) },
                                    label = { Text(station.name) },
                                )
                            }
                        }
                    }

                    // Capsulas de filtro de vista (En preparacion, Listos, Todos)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Capsula de Filtro
                        QueueFilter.entries.forEach { filter ->
                            val isSelected = state.filter == filter
                            val countLabel = when (filter) {
                                QueueFilter.ACTIVE -> " (3)"
                                QueueFilter.READY -> " (2)"
                                else -> ""
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectFilter(filter) },
                                label = { Text(stringResource(filterLabel(filter)) + countLabel, fontWeight = FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StaffColors.Coffee,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = StaffColors.Coffee
                                )
                            )
                        }
                    }

                    state.noticeCode?.let { code -> NoticePanel(code) }
                    state.pendingActions.forEach { pending ->
                        PendingActionPanel(pending, onRetry = { onRetryAction(pending) })
                    }

                    state.queueErrorCode?.let { code ->
                        ErrorPanel(code = code, onRetry = onRefreshQueue)
                    }

                    when {
                        state.isLoadingQueue && state.items.isEmpty() -> CenteredProgress()
                        state.items.isEmpty() && state.queueErrorCode != null -> Unit
                        state.items.isEmpty() && state.queueErrorCode == null -> EmptyQueue()
                        else -> {
                            if (state.isRefreshing) {
                                Text(
                                    stringResource(R.string.refreshing_label),
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            // Lista scrolleable de productos en cola
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("station-queue"),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(state.items, key = { it.itemId }) { item ->
                                    QueueItemCard(
                                        item = item,
                                        isBusy = item.itemId in state.activeItemIds,
                                        retryPending = state.pendingActions.any { it.itemId == item.itemId },
                                        onAction = { action -> onPerformAction(item, action) },
                                        onRetry = {
                                            state.pendingActions
                                                .firstOrNull { it.itemId == item.itemId }
                                                ?.let { onRetryAction(it) }
                                        },
                                    )
                                }
                                if (state.nextCursor != null) {
                                    item(key = "load-more") {
                                        OutlinedButton(
                                            onClick = onLoadMore,
                                            enabled = !state.isLoadingMore,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp),
                                        ) {
                                            if (state.isLoadingMore) CircularProgressIndicator()
                                            else Text(stringResource(R.string.load_more_action))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Tarjeta individual de pedido de la estacion (Mesa, producto, tiempo estimado y acciones)
@Composable
private fun QueueItemCard(
    item: StationQueueItem,
    isBusy: Boolean,
    retryPending: Boolean,
    onAction: (PreparationAction) -> Unit,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Encabezado de la tarjeta: Nombre de Mesa + Badge de Tiempo Estimado (ej: 8 min)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nombre / Numero de Mesa (ej: Mesa 4)
                Text(
                    text = item.tableLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = StaffColors.Coffee
                )

                // Badge de tiempo estimado o estado de demora (ej: 8 min)
                Surface(
                    color = StaffColors.SoftSageBg,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (item.delayed == true) stringResource(R.string.delayed_label) else stringResource(R.string.estimated_time_min, 8),
                        color = if (item.delayed == true) MaterialTheme.colorScheme.error else StaffColors.Sage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Nombre y cantidad del producto (ej: 2 x Tostado mixto)
            Text(
                text = stringResource(R.string.product_quantity, item.quantity, item.productName),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = StaffColors.Coffee
            )

            // Opciones/modificadores si existen
            if (item.optionsSummary.isNotBlank()) {
                Text(
                    text = stringResource(R.string.options_label, item.optionsSummary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            // Nota adicional por item
            item.note?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = stringResource(R.string.note_label, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            // Botones de accion dinamicos segun estado (Empezar, Listo, Entregar, Cancelar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (item.status == "QUEUED") {
                    Button(
                        onClick = { onAction(PreparationAction.START) },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StaffColors.Caramel)
                    ) {
                        Text(stringResource(R.string.start_action), fontWeight = FontWeight.Bold)
                    }
                } else if (item.status == "IN_PREPARATION") {
                    Button(
                        onClick = { onAction(PreparationAction.READY) },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StaffColors.Caramel)
                    ) {
                        Text(stringResource(R.string.ready_action), fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { onAction(PreparationAction.CANCEL) },
                    enabled = !isBusy,
                ) {
                    Text(stringResource(R.string.cancel_action), color = StaffColors.Coffee)
                }
            }

            if (retryPending) {
                OutlinedButton(
                    onClick = onRetry,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.retry_same_action))
                }
            }
        }
    }
}

// Dialogo modal para cancelar producto con motivo
@Composable
private fun CancelItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cancel_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(MAX_CANCEL_REASON_LENGTH) },
                    label = { Text(stringResource(R.string.cancel_reason_label)) },
                    supportingText = {
                        if (reason.isBlank()) Text(stringResource(R.string.cancel_reason_required))
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason.trim()) },
                enabled = reason.isNotBlank(),
            ) {
                Text(stringResource(R.string.cancel_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss_action)) }
        },
    )
}

@Composable
private fun PendingActionPanel(
    action: PendingPreparationAction,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.retry_same_action))
            Text(stringResource(R.string.pending_operation_details, action.orderCode, action.productName))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry_action)) }
        }
    }
}

@Composable
private fun ErrorPanel(code: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(errorLabel(code)), color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry_action)) }
    }
}

@Composable
private fun NoticePanel(code: String) {
    Text(
        text = stringResource(noticeLabel(code)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun CenteredProgress() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyQueue() {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
        Text(stringResource(R.string.queue_empty_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.queue_empty_description))
    }
}

private fun filterLabel(filter: QueueFilter): Int = when (filter) {
    QueueFilter.ACTIVE -> R.string.queue_filter_active
    QueueFilter.READY -> R.string.queue_filter_ready
    QueueFilter.ALL -> R.string.queue_filter_all
}

private fun errorLabel(code: String): Int = when (code) {
    "NETWORK_ERROR" -> R.string.network_error
    "FORBIDDEN", "ITEM_STATION_FORBIDDEN" -> R.string.forbidden_error
    "SESSION_EXPIRED", "TOKEN_EXPIRED", "UNAUTHENTICATED" -> R.string.session_expired
    "INVALID_RESPONSE" -> R.string.unexpected_response
    else -> R.string.generic_error
}

private fun noticeLabel(code: String): Int = when (code) {
    "ACTION_SUCCEEDED" -> R.string.action_success
    "ACTION_RESULT_UNKNOWN" -> R.string.retry_same_action
    "ITEM_INVALID_TRANSITION", "SESSION_CLOSED", "ORDER_ALREADY_DECIDED" -> R.string.conflict_error
    else -> errorLabel(code)
}

private const val MAX_CANCEL_REASON_LENGTH = 140

// Preview para Android Studio (Split / Design tab)
@Preview(showBackground = true)
@Composable
fun StationPreparationScreenPreview() {
    SomphieTheme {
        StationPreparationContent(
            state = StationQueueUiState(
                isLoadingStations = false,
                isLoadingQueue = false,
                selectedStation = com.somphie.staff.core.network.Station("1", "KITCHEN", "Cocina"),
                stations = listOf(com.somphie.staff.core.network.Station("1", "KITCHEN", "Cocina")),
                filter = QueueFilter.ACTIVE,
                items = listOf(
                    StationQueueItem(
                        itemId = "i1",
                        orderId = "o1",
                        orderCode = "#001",
                        tableLabel = "Mesa 4",
                        productName = "Tostado mixto",
                        quantity = 2,
                        status = "IN_PREPARATION"
                    ),
                    StationQueueItem(
                        itemId = "i2",
                        orderId = "o2",
                        orderCode = "#002",
                        tableLabel = "Mesa 7",
                        productName = "Latte",
                        quantity = 1,
                        status = "IN_PREPARATION"
                    ),
                    StationQueueItem(
                        itemId = "i3",
                        orderId = "o3",
                        orderCode = "#003",
                        tableLabel = "Mesa 10",
                        productName = "Sándwich de pollo",
                        quantity = 1,
                        status = "IN_PREPARATION"
                    )
                )
            ),
            profileName = "Lucas",
            hasPermission = true,
            context = LocalContext.current,
            onSignOut = {},
            onSelectStation = {},
            onSelectFilter = {},
            onRefreshQueue = {},
            onRetryStations = {},
            onPerformAction = { _, _ -> },
            onRetryAction = {},
            onLoadMore = {}
        )
    }
}
