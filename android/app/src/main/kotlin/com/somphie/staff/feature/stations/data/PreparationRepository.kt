package com.somphie.staff.feature.stations.data

import com.somphie.staff.core.network.ActionResult
import com.somphie.staff.core.network.CancelItemRequest
import com.somphie.staff.core.network.Station
import com.somphie.staff.core.network.StationQueueResponse
import com.somphie.staff.core.network.StaffApi
import com.somphie.staff.core.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

enum class PreparationAction {
    START,
    READY,
    DELIVER,
    CANCEL,
    UNDO,
}

interface PreparationDataSource {
    suspend fun stations(): List<Station>

    suspend fun queue(
        stationId: String,
        statuses: List<String>,
        cursor: String? = null,
    ): StationQueueResponse

    suspend fun perform(action: PendingPreparationAction): ActionResult
}

data class PendingPreparationAction(
    val itemId: String,
    val productName: String,
    val orderCode: String,
    val action: PreparationAction,
    val idempotencyKey: String,
    val reason: String? = null,
)

/** Encapsula las rutas de preparación y conserva juntas la operación y su clave idempotente. */
@Singleton
class PreparationRepository @Inject constructor(
    private val api: StaffApi,
    private val json: Json,
) : PreparationDataSource {
    override suspend fun stations(): List<Station> =
        apiCall(json) { api.stations().items }

    override suspend fun queue(
        stationId: String,
        statuses: List<String>,
        cursor: String?,
    ): StationQueueResponse = apiCall(json) {
        api.queue(
            stationId = stationId,
            statuses = statuses.joinToString(","),
            limit = PAGE_SIZE,
            cursor = cursor,
        )
    }

    override suspend fun perform(action: PendingPreparationAction): ActionResult = apiCall(json) {
        when (action.action) {
            PreparationAction.START -> api.startItem(action.itemId, action.idempotencyKey)
            PreparationAction.READY -> api.markReady(action.itemId, action.idempotencyKey)
            PreparationAction.DELIVER -> api.deliver(action.itemId, action.idempotencyKey)
            PreparationAction.CANCEL -> api.cancel(
                action.itemId,
                action.idempotencyKey,
                CancelItemRequest(requireNotNull(action.reason)),
            )
            PreparationAction.UNDO -> api.undo(action.itemId, action.idempotencyKey)
        }
    }

    private companion object {
        const val PAGE_SIZE = 100
    }
}
