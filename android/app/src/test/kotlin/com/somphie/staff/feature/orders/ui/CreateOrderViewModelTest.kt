package com.somphie.staff.feature.orders.ui

import com.somphie.staff.TestDispatcherRule
import com.somphie.staff.feature.orders.data.OrdersRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOrderViewModelTest {

    @get:Rule
    val testDispatcherRule = TestDispatcherRule()

    private lateinit var fakeApi: FakeStaffApi
    private lateinit var repository: OrdersRepository
    private lateinit var viewModel: CreateOrderViewModel

    @Before
    fun setUp() {
        fakeApi = FakeStaffApi()
        repository = OrdersRepository(fakeApi)
        viewModel = CreateOrderViewModel(repository)
    }

    @Test
    fun `incrementProduct adds quantity`() {
        viewModel.incrementProduct("p-latte")
        viewModel.incrementProduct("p-latte")

        val state = viewModel.uiState.value
        assertEquals(2, state.quantities["p-latte"])
    }

    @Test
    fun `decrementProduct reduces quantity`() {
        viewModel.incrementProduct("p-latte")
        viewModel.incrementProduct("p-latte")
        viewModel.decrementProduct("p-latte")

        val state = viewModel.uiState.value
        assertEquals(1, state.quantities["p-latte"])
    }

    @Test
    fun `submitOrder without items sets errorMessage`() = runTest {
        viewModel.submitOrder("sess-1")

        val state = viewModel.uiState.value
        assertEquals("Selecciona al menos un producto", state.errorMessage)
    }

    @Test
    fun `submitOrder success calls repository and sets createdOrder`() = runTest {
        viewModel.incrementProduct("p-latte")
        viewModel.submitOrder("sess-1")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isSubmitting)
        assertNull(state.errorMessage)
    }
}
