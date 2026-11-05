package com.somphie.staff.feature.orders.ui

import com.somphie.staff.TestDispatcherRule
import com.somphie.staff.core.network.StaffOrder
import com.somphie.staff.feature.orders.data.OrdersRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val sampleOrder = StaffOrder(
        id = "order-123",
        code = "#004",
        sessionId = "session-1",
        tableLabel = "4",
        status = "PENDING_REVIEW",
        createdAt = "2026-10-18T12:34:00Z",
        items = emptyList(),
        totalMinor = 1090
    )

    @Test
    fun `loadOrderDetail success updates state`() = runTest(dispatcherRule.dispatcher) {
        val fakeApi = FakeStaffApi().apply {
            orderDetail = sampleOrder
        }
        val repository = OrdersRepository(fakeApi)
        val viewModel = OrderDetailViewModel(repository)
        viewModel.loadOrderDetail("order-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is OrderDetailUiState.Success)
        assertEquals("order-123", (state as OrderDetailUiState.Success).order.id)
    }

    @Test
    fun `acceptOrder triggers success callback`() = runTest(dispatcherRule.dispatcher) {
        val fakeApi = FakeStaffApi().apply {
            orderDetail = sampleOrder
        }
        val repository = OrdersRepository(fakeApi)
        val viewModel = OrderDetailViewModel(repository)
        var callbackCalled = false

        viewModel.acceptOrder("order-123") {
            callbackCalled = true
        }
        advanceUntilIdle()

        assertTrue(callbackCalled)
        assertEquals("order-123", fakeApi.lastAcceptedOrderId)
    }
}
