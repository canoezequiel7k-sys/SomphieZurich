package com.somphie.staff.feature.stations.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.somphie.staff.R
import com.somphie.staff.core.network.StationQueueItem
import com.somphie.staff.core.network.StaffProfile
import com.somphie.staff.feature.stations.data.PendingPreparationAction
import com.somphie.staff.feature.stations.data.PreparationAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val QUEUE_REFRESH_INTERVAL_MS = 5_000L

/** Pantalla de cocina/barra; todos los datos y resultados de acciones provienen del backend. */
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
    // En Fase 5 la guía fija polling REST de 5 s; SSE se integra en la Fase 6.
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

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.stations_title), style = MaterialTheme.typography.titleLarge)
                    Text(profile.displayName, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onSignOut) { Text(stringResource(R.string.logout_action)) }
            }

            if ("item.prepare" !in permissionSet) {
                Text(
                    stringResource(R.string.preparation_permission_required),
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                return@Column
            }

            state.stationErrorCode?.let { code ->
                ErrorPanel(code = code, onRetry = viewModel::retryStations)
            }

            when {
                state.isLoadingStations -> CenteredProgress()
                state.stationErrorCode != null -> Unit
                state.stations.isEmpty() -> Text(
                    stringResource(R.string.stations_empty),
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                else -> {
                    Text(
                        stringResource(R.string.station_picker_label),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.stations.forEach { station ->
                            FilterChip(
                                selected = station.id == state.selectedStation?.id,
                                onClick = { viewModel.selectStation(station) },
                                label = { Text(station.name) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QueueFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = state.filter == filter,
                                onClick = { viewModel.selectFilter(filter) },
                                label = { Text(stringResource(filterLabel(filter))) },
                            )
                        }
                        TextButton(
                            onClick = viewModel::refreshQueue,
                            modifier = Modifier.semantics {
                                contentDescription = context.getString(R.string.accessibility_refresh)
                            },
                        ) {
                            Text(stringResource(R.string.refresh_action))
                        }
                    }

                    state.noticeCode?.let { code -> NoticePanel(code) }
                    state.pendingActions.forEach { pending ->
                        PendingActionPanel(pending, onRetry = { viewModel.retryAction(pending.idempotencyKey) })
                    }

                    state.queueErrorCode?.let { code ->
                        ErrorPanel(code = code, onRetry = viewModel::refreshQueue)
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
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth().testTag("station-queue"),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.items, key = { it.itemId }) { item ->
                                    QueueItemCard(
                                        item = item,
                                        permissions = permissionSet,
                                        isBusy = item.itemId in state.activeItemIds,
                                        retryPending = state.pendingActions.any { it.itemId == item.itemId },
                                        onAction = { action ->
                                            if (action == PreparationAction.CANCEL) itemToCancel = item
                                            else viewModel.performAction(item, action)
                                        },
                                        onRetry = {
                                            state.pendingActions
                                                .firstOrNull { it.itemId == item.itemId }
                                                ?.let { viewModel.retryAction(it.idempotencyKey) }
                                        },
                                    )
                                }
                                if (state.nextCursor != null) {
                                    item(key = "load-more") {
                                        OutlinedButton(
                                            onClick = viewModel::loadMore,
                                            enabled = !state.isLoadingMore,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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

@Composable
private fun QueueItemCard(
    item: StationQueueItem,
    permissions: Set<String>,
    isBusy: Boolean,
    retryPending: Boolean,
    onAction: (PreparationAction) -> Unit,
    onRetry: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.order_label, item.orderCode),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(item.tableLabel)
            Text(
                stringResource(R.string.product_quantity, item.quantity, item.productName),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                if (item.status in KNOWN_QUEUE_STATUSES) stringResource(statusLabel(item.status))
                else stringResource(R.string.unknown_status, item.status),
            )
            if (item.delayed == true) {
                Text(
                    stringResource(R.string.delayed_label),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (item.optionsSummary.isNotBlank()) {
                Text(stringResource(R.string.options_label, item.optionsSummary))
            }
            item.note?.takeIf(String::isNotBlank)?.let {
                Text(stringResource(R.string.note_label, it))
            }
            val actions = availableActions(item, permissions)
            if (actions.isNotEmpty()) {
                val quickActions = actions.filterNot { it == PreparationAction.CANCEL }
                if (quickActions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        quickActions.forEach { action ->
                            Button(
                                onClick = { onAction(action) },
                                enabled = !isBusy,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(actionLabel(action)))
                            }
                        }
                    }
                }
                if (PreparationAction.CANCEL in actions) {
                    // La cancelación tiene motivo obligatorio; el ancho completo evita truncar su etiqueta.
                        Button(
                            onClick = { onAction(PreparationAction.CANCEL) },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(actionLabel(PreparationAction.CANCEL)))
                        }
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
        colors = androidx.compose.material3.CardDefaults.cardColors(
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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

private fun statusLabel(status: String): Int = when (status) {
    "QUEUED" -> R.string.status_queued
    "IN_PREPARATION" -> R.string.status_in_preparation
    "READY" -> R.string.status_ready
    else -> R.string.status_unknown
}

private fun actionLabel(action: PreparationAction): Int = when (action) {
    PreparationAction.START -> R.string.start_action
    PreparationAction.READY -> R.string.ready_action
    PreparationAction.DELIVER -> R.string.deliver_action
    PreparationAction.CANCEL -> R.string.cancel_action
    PreparationAction.UNDO -> R.string.undo_action
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
private val KNOWN_QUEUE_STATUSES = setOf("QUEUED", "IN_PREPARATION", "READY")
