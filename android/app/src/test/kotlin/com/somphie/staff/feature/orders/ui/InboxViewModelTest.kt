package com.somphie.staff.feature.orders.ui

import com.somphie.staff.TestDispatcherRule
import com.somphie.staff.core.network.StaffOrder
import com.somphie.staff.core.network.StaffOrderItem
import com.somphie.staff.core.network.StaffOrderPage
import com.somphie.staff.feature.orders.data.OrdersRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun createSampleOrder(id: String, status: String) = StaffOrder(
        id = id,
        code = "#001",
        sessionId = "session-1",
        tableLabel = "4",
        status = status,
        createdAt = "2026-10-18T12:00:00Z",
        items = listOf(
            StaffOrderItem(
                id = "item-1",
                productId = "prod-1",
                productName = "Latte",
                quantity = 2,
                unitPriceMinor = 320,
                totalMinor = 640,
                status = status
            )
        ),
        totalMinor = 640
    )

    @Test
    fun `loadOrders success filters in progress orders by default`() = runTest(dispatcherRule.dispatcher) {
        val fakeApi = FakeStaffApi().apply {
            ordersPage = StaffOrderPage(
                items = listOf(
                    createSampleOrder("1", "PENDING_REVIEW"),
                    createSampleOrder("2", "READY"),
                    createSampleOrder("3", "ACCEPTED")
                )
            )
        }
        val repository = OrdersRepository(fakeApi)
        val viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is InboxUiState.Success)
        val success = state as InboxUiState.Success
        assertEquals(2, success.orders.size) // PENDING_REVIEW + ACCEPTED
        assertEquals(2, success.inProgressCount)
        assertEquals(1, success.readyCount)
    }

    @Test
    fun `loadOrders failure sets error state`() = runTest(dispatcherRule.dispatcher) {
        val fakeApi = FakeStaffApi().apply {
            shouldFail = true
        }
        val repository = OrdersRepository(fakeApi)
        val viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is InboxUiState.Error)
        assertEquals("Error de red simulado", (state as InboxUiState.Error).message)
    }
}
