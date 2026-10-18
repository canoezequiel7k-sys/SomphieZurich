package com.somphie.staff.feature.stations.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somphie.staff.core.network.ApiFailure
import com.somphie.staff.core.network.Station
import com.somphie.staff.core.network.StationQueueItem
import com.somphie.staff.feature.stations.data.PendingPreparationAction
import com.somphie.staff.feature.stations.data.PreparationAction
import com.somphie.staff.feature.stations.data.PreparationDataSource
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

enum class QueueFilter(val statuses: List<String>) {
    ACTIVE(listOf("QUEUED", "IN_PREPARATION")),
    READY(listOf("READY")),
    ALL(listOf("QUEUED", "IN_PREPARATION", "READY")),
}

/**
 * Conserva la última lectura confirmada por el servidor y el estado de cada solicitud.
 * Las acciones no cambian ítems localmente: después de cada respuesta se vuelve a leer la cola.
 */
data class StationQueueUiState(
    val stations: List<Station> = emptyList(),
    val selectedStation: Station? = null,
    val filter: QueueFilter = QueueFilter.ACTIVE,
    val items: List<StationQueueItem> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingStations: Boolean = true,
    val isLoadingQueue: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val stationErrorCode: String? = null,
    val queueErrorCode: String? = null,
    val noticeCode: String? = null,
    val loadingActionKeys: Set<String> = emptySet(),
    val activeItemIds: Set<String> = emptySet(),
    val pendingActions: List<PendingPreparationAction> = emptyList(),
    val refreshAfterCurrent: Boolean = false,
)

@HiltViewModel
class StationQueueViewModel @Inject constructor(
    private val repository: PreparationDataSource,
) : ViewModel() {
    private val _state = MutableStateFlow(StationQueueUiState())
    val state: StateFlow<StationQueueUiState> = _state.asStateFlow()
    private var stationsLoaded = false
    private var queueLoaded = false

    fun loadStations() {
        if (stationsLoaded || !_state.value.isLoadingStations) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingStations = true, stationErrorCode = null) }
            try {
                val stations = repository.stations()
                stationsLoaded = true
                val selected = _state.value.selectedStation
                    ?.let { current -> stations.firstOrNull { it.id == current.id } }
                    ?: stations.firstOrNull()
                _state.update {
                    it.copy(
                        stations = stations,
                        selectedStation = selected,
                        isLoadingStations = false,
                        stationErrorCode = null,
                    )
                }
                if (selected != null) refreshQueue()
            } catch (failure: ApiFailure) {
                stationLoadFailed(failure)
            } catch (failure: IOException) {
                stationLoadFailed(failure)
            } catch (failure: SerializationException) {
                stationLoadFailed(failure)
            } catch (failure: HttpException) {
                stationLoadFailed(failure)
            }
        }
    }

    fun retryStations() {
        stationsLoaded = false
        _state.update { it.copy(isLoadingStations = true) }
        loadStations()
    }

    fun selectStation(station: Station) {
        if (_state.value.selectedStation?.id == station.id) return
        _state.update {
            it.copy(
                selectedStation = station,
                items = emptyList(),
                nextCursor = null,
                queueErrorCode = null,
                isLoadingQueue = false,
                isRefreshing = false,
                isLoadingMore = false,
                refreshAfterCurrent = false,
            )
        }
        queueGeneration++
        queueLoaded = false
        refreshQueue()
    }

    fun selectFilter(filter: QueueFilter) {
        if (_state.value.filter == filter) return
        _state.update {
            it.copy(
                filter = filter,
                items = emptyList(),
                nextCursor = null,
                queueErrorCode = null,
                isLoadingQueue = false,
                isRefreshing = false,
                isLoadingMore = false,
                refreshAfterCurrent = false,
            )
        }
        queueGeneration++
        queueLoaded = false
        refreshQueue()
    }

    fun refreshQueue() {
        val current = _state.value
        val station = current.selectedStation ?: return
        if (current.isLoadingQueue || current.isRefreshing) {
            if (current.isRefreshing) _state.update { it.copy(refreshAfterCurrent = true) }
            return
        }

        val generation = ++queueGeneration
        _state.update {
            it.copy(
                isLoadingQueue = !queueLoaded,
                isRefreshing = queueLoaded,
                isLoadingMore = false,
                queueErrorCode = null,
                refreshAfterCurrent = false,
            )
        }
        viewModelScope.launch {
            try {
                val page = repository.queue(station.id, current.filter.statuses)
                if (generation != queueGeneration ||
                    _state.value.selectedStation?.id != station.id ||
                    _state.value.filter != current.filter
                ) return@launch
                queueLoaded = true
                _state.update {
                    it.copy(
                        items = page.items,
                        nextCursor = page.nextCursor,
                        isLoadingQueue = false,
                        isRefreshing = false,
                        queueErrorCode = null,
                    )
                }
                // Si una acción terminó mientras esta lectura estaba en vuelo, repetimos la consulta
                // para que una respuesta iniciada antes de la acción no deje la pantalla atrasada.
                if (_state.value.refreshAfterCurrent) {
                    _state.update { it.copy(refreshAfterCurrent = false) }
                    refreshQueue()
                }
            } catch (failure: ApiFailure) {
                queueLoadFailed(failure, generation)
            } catch (failure: IOException) {
                queueLoadFailed(failure, generation)
            } catch (failure: SerializationException) {
                queueLoadFailed(failure, generation)
            } catch (failure: HttpException) {
                queueLoadFailed(failure, generation)
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        val station = current.selectedStation ?: return
        val cursor = current.nextCursor ?: return
        if (current.isLoadingMore) return
        val generation = queueGeneration
        _state.update { it.copy(isLoadingMore = true, queueErrorCode = null) }
        viewModelScope.launch {
            try {
                val page = repository.queue(station.id, current.filter.statuses, cursor)
                if (generation != queueGeneration ||
                    _state.value.selectedStation?.id != station.id ||
                    _state.value.filter != current.filter
                ) return@launch
                _state.update { latest ->
                    val knownItems = latest.items.associateBy { it.itemId }
                    val appended = page.items.filterNot { it.itemId in knownItems }
                    latest.copy(
                        items = latest.items + appended,
                        nextCursor = page.nextCursor,
                        isLoadingMore = false,
                        queueErrorCode = null,
                    )
                }
            } catch (failure: ApiFailure) {
                moreLoadFailed(failure, generation)
            } catch (failure: IOException) {
                moreLoadFailed(failure, generation)
            } catch (failure: SerializationException) {
                moreLoadFailed(failure, generation)
            } catch (failure: HttpException) {
                moreLoadFailed(failure, generation)
            }
        }
    }

    fun performAction(
        item: StationQueueItem,
        action: PreparationAction,
        reason: String? = null,
    ) {
        val pending = PendingPreparationAction(
            itemId = item.itemId,
            productName = item.productName,
            orderCode = item.orderCode,
            action = action,
            idempotencyKey = UUID.randomUUID().toString(),
            reason = reason,
        )
        submitAction(pending)
    }

    fun retryAction(idempotencyKey: String) {
        val pending = _state.value.pendingActions.firstOrNull { it.idempotencyKey == idempotencyKey } ?: return
        submitAction(pending)
    }

    private fun submitAction(action: PendingPreparationAction) {
        if (action.idempotencyKey in _state.value.loadingActionKeys) return
        _state.update {
            it.copy(
                loadingActionKeys = it.loadingActionKeys + action.idempotencyKey,
                activeItemIds = it.activeItemIds + action.itemId,
                pendingActions = it.pendingActions
                    .filterNot { pending -> pending.idempotencyKey == action.idempotencyKey },
                noticeCode = null,
            )
        }
        viewModelScope.launch {
            try {
                repository.perform(action)
                _state.update {
                    it.copy(
                        loadingActionKeys = it.loadingActionKeys - action.idempotencyKey,
                        activeItemIds = it.activeItemIds - action.itemId,
                        noticeCode = "ACTION_SUCCEEDED",
                    )
                }
                refreshQueue()
            } catch (failure: ApiFailure) {
                actionFailed(action, failure, failure.status >= SERVER_ERROR)
            } catch (failure: IOException) {
                actionFailed(action, failure, uncertain = true)
            } catch (failure: SerializationException) {
                actionFailed(action, failure, uncertain = true)
            } catch (failure: HttpException) {
                actionFailed(action, failure, failure.code() >= SERVER_ERROR)
            }
        }
    }

    private fun stationLoadFailed(failure: Throwable) {
        _state.update {
            it.copy(isLoadingStations = false, stationErrorCode = errorCode(failure))
        }
    }

    private fun queueLoadFailed(failure: Throwable, generation: Int) {
        if (generation != queueGeneration) return
        val refreshAgain = _state.value.refreshAfterCurrent
        _state.update {
            it.copy(
                isLoadingQueue = false,
                isRefreshing = false,
                queueErrorCode = errorCode(failure),
                refreshAfterCurrent = false,
            )
        }
        if (refreshAgain) refreshQueue()
    }

    private fun moreLoadFailed(failure: Throwable, generation: Int) {
        if (generation != queueGeneration) return
        _state.update {
            it.copy(isLoadingMore = false, queueErrorCode = errorCode(failure))
        }
    }

    private fun actionFailed(
        action: PendingPreparationAction,
        failure: Throwable,
        uncertain: Boolean,
    ) {
        _state.update {
            it.copy(
                loadingActionKeys = it.loadingActionKeys - action.idempotencyKey,
                activeItemIds = it.activeItemIds - action.itemId,
                pendingActions = if (uncertain) {
                    it.pendingActions + action
                } else {
                    it.pendingActions.filterNot { pending ->
                        pending.idempotencyKey == action.idempotencyKey
                    }
                },
                noticeCode = if (uncertain) "ACTION_RESULT_UNKNOWN" else errorCode(failure),
            )
        }
        if (failure is ApiFailure && failure.status == CONFLICT || uncertain) refreshQueue()
    }

    private fun errorCode(failure: Throwable): String = when (failure) {
        is ApiFailure -> failure.code
        is HttpException -> "HTTP_ERROR"
        is IOException -> "NETWORK_ERROR"
        is SerializationException -> "INVALID_RESPONSE"
        else -> "UNKNOWN_ERROR"
    }

    private companion object {
        const val SERVER_ERROR = 500
        const val CONFLICT = 409
    }

    private var queueGeneration = 0
}

fun availableActions(item: StationQueueItem, permissions: Set<String>): List<PreparationAction> =
    buildList {
        // Esto evita ofrecer controles inválidos; el backend vuelve a validar permiso y estación.
        when (item.status) {
            "QUEUED" -> {
                if ("item.prepare" in permissions) {
                    add(PreparationAction.START)
                    add(PreparationAction.READY)
                }
            }
            "IN_PREPARATION" -> {
                if ("item.prepare" in permissions) add(PreparationAction.READY)
                if ("item.undo" in permissions) add(PreparationAction.UNDO)
            }
            "READY" -> {
                if ("item.deliver" in permissions) add(PreparationAction.DELIVER)
                if ("item.undo" in permissions) add(PreparationAction.UNDO)
            }
        }
        if (
            item.status in setOf("QUEUED", "IN_PREPARATION", "READY") &&
            "order.cancel" in permissions
        ) {
            add(PreparationAction.CANCEL)
        }
    }
