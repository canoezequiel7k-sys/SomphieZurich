package com.somphie.staff.feature.stations.ui

import com.somphie.staff.TestDispatcherRule
import com.somphie.staff.core.network.ActionResult
import com.somphie.staff.core.network.ApiFailure
import com.somphie.staff.core.network.QueueOption
import com.somphie.staff.core.network.Station
import com.somphie.staff.core.network.StationQueueItem
import com.somphie.staff.core.network.StationQueueResponse
import com.somphie.staff.feature.stations.data.PendingPreparationAction
import com.somphie.staff.feature.stations.data.PreparationAction
import com.somphie.staff.feature.stations.data.PreparationDataSource
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StationQueueViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val station = Station(
        id = "station-bar",
        code = "BAR",
        name = "Barra",
    )

    @Test
    fun `load stations and queue renders authorized station contents`() = runTest(dispatcherRule.dispatcher) {
        val source = FakePreparationSource()
        source.queueResponses += page(item(status = "QUEUED"))
        val viewModel = StationQueueViewModel(source)

        viewModel.loadStations()
        advanceUntilIdle()

        assertEquals(listOf(station), viewModel.state.value.stations)
        assertEquals(station, viewModel.state.value.selectedStation)
        assertEquals("Flat white", viewModel.state.value.items.single().productName)
        assertEquals(listOf("QUEUED", "IN_PREPARATION"), source.statusRequests.single())
    }

    @Test
    fun `queue read failure preserves the last server snapshot`() = runTest(dispatcherRule.dispatcher) {
        val source = FakePreparationSource()
        val item = item(status = "IN_PREPARATION")
        source.queueResponses += page(item)
        source.queueFailures += null
        source.queueFailures += IOException("connection reset")
        val viewModel = StationQueueViewModel(source)

        viewModel.loadStations()
        advanceUntilIdle()
        viewModel.refreshQueue()
        advanceUntilIdle()

        assertEquals(listOf(item), viewModel.state.value.items)
        assertEquals("NETWORK_ERROR", viewModel.state.value.queueErrorCode)
    }

    @Test
    fun `ambiguous action retry reuses its idempotency key and confirms from server queue`() =
        runTest(dispatcherRule.dispatcher) {
            val source = FakePreparationSource()
            val queued = item(status = "QUEUED")
            val started = queued.copy(status = "IN_PREPARATION")
            source.queueResponses += page(queued)
            source.queueResponses += page(queued)
            source.queueResponses += page(started)
            source.actionFailures += IOException("read timed out")
            val viewModel = StationQueueViewModel(source)

            viewModel.loadStations()
            advanceUntilIdle()
            viewModel.performAction(queued, PreparationAction.START)
            advanceUntilIdle()

            val pendingKey = viewModel.state.value.pendingActions.single().idempotencyKey
            assertEquals("ACTION_RESULT_UNKNOWN", viewModel.state.value.noticeCode)

            viewModel.retryAction(pendingKey)
            advanceUntilIdle()

            assertEquals(listOf(pendingKey, pendingKey), source.actionRequests.map { it.idempotencyKey })
            assertTrue(viewModel.state.value.pendingActions.isEmpty())
            assertEquals("IN_PREPARATION", viewModel.state.value.items.single().status)
            assertEquals("ACTION_SUCCEEDED", viewModel.state.value.noticeCode)
        }

    @Test
    fun `conflict clears retry and reloads the server state`() = runTest(dispatcherRule.dispatcher) {
        val source = FakePreparationSource()
        val queued = item(status = "QUEUED")
        val ready = queued.copy(status = "READY")
        source.queueResponses += page(queued)
        source.queueResponses += page(ready)
        source.actionFailures += ApiFailure(409, "ITEM_INVALID_TRANSITION", "Item is already ready")
        val viewModel = StationQueueViewModel(source)

        viewModel.loadStations()
        advanceUntilIdle()
        viewModel.performAction(queued, PreparationAction.START)
        advanceUntilIdle()

        assertEquals("READY", viewModel.state.value.items.single().status)
        assertEquals("ITEM_INVALID_TRANSITION", viewModel.state.value.noticeCode)
        assertTrue(viewModel.state.value.pendingActions.isEmpty())
    }

    @Test
    fun `visible actions follow item status and the server permission set`() {
        val queued = item(status = "QUEUED")
        val ready = queued.copy(status = "READY")
        val unknown = queued.copy(status = "UNRECOGNIZED")

        assertEquals(
            listOf(PreparationAction.START, PreparationAction.READY),
            availableActions(queued, setOf("item.prepare")),
        )
        assertEquals(
            listOf(PreparationAction.DELIVER, PreparationAction.UNDO, PreparationAction.CANCEL),
            availableActions(ready, setOf("item.deliver", "item.undo", "order.cancel")),
        )
        assertEquals(emptyList<PreparationAction>(), availableActions(unknown, setOf("item.prepare", "order.cancel")))
        assertFalse(PreparationAction.DELIVER in availableActions(ready, setOf("item.prepare")))
        assertTrue(PreparationAction.UNDO in availableActions(ready, setOf("item.undo")))
    }

    private fun page(vararg items: StationQueueItem) = StationQueueResponse(
        items = items.toList(),
        nextCursor = null,
    )

    private fun item(status: String) = StationQueueItem(
        itemId = "item-1",
        orderId = "order-1",
        orderCode = "#042",
        tableLabel = "7",
        productName = "Flat white",
        quantity = 2,
        note = "Sin azúcar",
        status = status,
        queuedAt = null,
        delayed = false,
        optionsSummary = "Grande · Avena",
        options = listOf(
            QueueOption(
                groupId = "group-1",
                optionId = "option-1",
                groupName = "Tamaño",
                optionName = "Grande",
            ),
        ),
    )

    private class FakePreparationSource : PreparationDataSource {
        val queueResponses = ArrayDeque<StationQueueResponse>()
        val queueFailures = ArrayDeque<IOException?>()
        val actionFailures = ArrayDeque<Exception>()
        val statusRequests = mutableListOf<List<String>>()
        val actionRequests = mutableListOf<PendingPreparationAction>()

        override suspend fun stations() = listOf(
            Station(id = "station-bar", code = "BAR", name = "Barra"),
        )

        override suspend fun queue(
            stationId: String,
            statuses: List<String>,
            cursor: String?,
        ): StationQueueResponse {
            statusRequests += statuses
            val failure = queueFailures.removeFirstOrNull()
            if (failure != null) throw failure
            return queueResponses.removeFirst()
        }

        override suspend fun perform(action: PendingPreparationAction): ActionResult {
            actionRequests += action
            val failure = actionFailures.removeFirstOrNull()
            if (failure != null) throw failure
            return ActionResult(
                id = action.orderCode,
                status = "IN_PREPARATION",
                version = 2,
                items = emptyList(),
            )
        }
    }
}
