package com.somphie.staff.feature.availability.ui

import com.somphie.staff.TestDispatcherRule
import com.somphie.staff.feature.availability.data.AvailabilityRepository
import com.somphie.staff.feature.orders.ui.FakeStaffApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AvailabilityViewModelTest {

    @get:Rule
    val testDispatcherRule = TestDispatcherRule()

    private lateinit var fakeApi: FakeStaffApi
    private lateinit var repository: AvailabilityRepository
    private lateinit var viewModel: AvailabilityViewModel

    @Before
    fun setUp() {
        fakeApi = FakeStaffApi()
        repository = AvailabilityRepository(fakeApi)
        viewModel = AvailabilityViewModel(repository)
    }

    @Test
    fun `loadProducts loads initial products list`() {
        val state = viewModel.uiState.value
        assertTrue(state.products.isNotEmpty())
        assertEquals("Latte", state.products.first().name)
    }

    @Test
    fun `updateSearchQuery filters product list search`() {
        viewModel.updateSearchQuery("Tostado")
        val state = viewModel.uiState.value
        assertEquals("Tostado", state.searchQuery)
    }

    @Test
    fun `toggleProductAvailability toggles available state successfully`() = runTest {
        viewModel.toggleProductAvailability("p-latte")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isUpdating)
        val latte = state.products.find { it.productId == "p-latte" }
        assertNotNull(latte)
        assertFalse(latte!!.available)
        assertEquals("Producto marcado como agotado", state.successMessage)
    }
}
