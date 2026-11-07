package com.somphie.staff.feature.billing.ui

import com.somphie.staff.TestDispatcherRule
import com.somphie.staff.core.network.Bill
import com.somphie.staff.core.network.StaffSession
import com.somphie.staff.core.network.StaffSessionDetail
import com.somphie.staff.feature.billing.data.BillingRepository
import com.somphie.staff.feature.orders.ui.FakeStaffApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingViewModelTest {

    @get:Rule
    val testDispatcherRule = TestDispatcherRule()

    private lateinit var fakeApi: FakeStaffApi
    private lateinit var repository: BillingRepository
    private lateinit var viewModel: BillingViewModel

    @Before
    fun setUp() {
        fakeApi = FakeStaffApi()
        repository = BillingRepository(fakeApi)
        viewModel = BillingViewModel(repository)
    }

    @Test
    fun `loadBill success updates sessionDetail in uiState`() = runTest {
        val mockSession = StaffSessionDetail(
            session = StaffSession(id = "sess-1", status = "OPEN", tableLabel = "Mesa 4", openedAt = "12:00"),
            bill = Bill(currency = "ARS", chargesMinor = 1090, balanceMinor = 1090)
        )
        fakeApi.sessionDetail = mockSession

        viewModel.loadBill("sess-1")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.sessionDetail)
        assertEquals("Mesa 4", state.sessionDetail?.session?.tableLabel)
        assertEquals(1090L, state.sessionDetail?.bill?.balanceMinor)
        assertNull(state.errorMessage)
    }

    @Test
    fun `processPayment success sets success message`() = runTest {
        val mockSession = StaffSessionDetail(
            session = StaffSession(id = "sess-1", status = "OPEN", tableLabel = "Mesa 4", openedAt = "12:00"),
            bill = Bill(currency = "ARS", chargesMinor = 1090, balanceMinor = 1090)
        )
        fakeApi.sessionDetail = mockSession

        viewModel.processPayment("sess-1", amountMinor = 1090L, tipMinor = 100L)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isProcessingPayment)
        assertEquals("Pago registrado correctamente", state.paymentSuccessMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun `processPayment failure sets errorMessage`() = runTest {
        fakeApi.shouldFail = true

        viewModel.processPayment("sess-1", amountMinor = 1090L)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isProcessingPayment)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `formatMoney formats minor units correctly`() {
        val formatted = formatMoney(1090L, "ARS")
        assertEquals("$10.90", formatted)
    }

    @Test
    fun `applyDiscountPercent success for owner`() = runTest {
        val mockSession = StaffSessionDetail(
            session = StaffSession(id = "sess-1", status = "OPEN", tableLabel = "Mesa 4", openedAt = "12:00"),
            bill = Bill(currency = "ARS", chargesMinor = 1090, balanceMinor = 1090)
        )
        fakeApi.sessionDetail = mockSession

        viewModel.applyDiscountPercent("sess-1", percent = 20, isWaiter = false)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Descuento del 20% aplicado correctamente", state.paymentSuccessMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun `applyDiscountPercent exceeds 15 percent limit for waiter sets errorMessage`() = runTest {
        viewModel.applyDiscountPercent("sess-1", percent = 20, isWaiter = true)

        val state = viewModel.uiState.value
        assertEquals("Los mozos solo pueden aplicar hasta un 15% de descuento (Regla D-142)", state.errorMessage)
    }
}
