package com.somphie.staff.feature.billing.ui

import com.somphie.staff.TestDispatcherRule
import com.somphie.staff.core.network.AdjustmentReasonCode
import com.somphie.staff.core.network.ApiFailure
import com.somphie.staff.core.network.Bill
import com.somphie.staff.core.network.StaffSession
import com.somphie.staff.core.network.StaffSessionDetail
import com.somphie.staff.feature.billing.data.BillingRepository
import com.somphie.staff.feature.orders.ui.FakeStaffApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `processPayment success sets success message and clears idempotency key`() = runTest {
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
        assertNull(viewModel.idempotencyTracker.currentKey())
    }

    @Test
    fun `processPayment failure preserves idempotency key for identical retry`() = runTest {
        fakeApi.shouldFail = true

        viewModel.processPayment("sess-1", amountMinor = 1090L, tipMinor = 100L)
        testScheduler.advanceUntilIdle()

        val firstKey = viewModel.idempotencyTracker.currentKey()
        assertNotNull("La clave debe haberse generado en el primer intento", firstKey)

        // Reintento con el mismo cuerpo exacto
        viewModel.processPayment("sess-1", amountMinor = 1090L, tipMinor = 100L)
        testScheduler.advanceUntilIdle()
        val secondKey = viewModel.idempotencyTracker.currentKey()

        assertEquals("Reintentar el mismo payload debe conservar la misma clave de idempotencia", firstKey, secondKey)

        // Modificar cualquier campo (ej: monto) debe generar una clave nueva
        viewModel.processPayment("sess-1", amountMinor = 1500L, tipMinor = 100L)
        testScheduler.advanceUntilIdle()
        val thirdKey = viewModel.idempotencyTracker.currentKey()

        assertNotEquals("Cambiar cualquier campo del cuerpo debe producir una clave de idempotencia nueva", firstKey, thirdKey)
    }

    @Test
    fun `idempotency tracker generates new key on any field change`() {
        val tracker = IdempotencyTracker()

        val payload1 = CashPaymentPayload("sess-1", 1000L, 100L, "Mesa 1")
        val key1 = tracker.getOrCreateKey(payload1)
        val key1Repeat = tracker.getOrCreateKey(payload1)
        assertEquals("Mismo payload debe retornar la misma clave", key1, key1Repeat)

        // Cambiar propina
        val payloadChangedTip = CashPaymentPayload("sess-1", 1000L, 200L, "Mesa 1")
        val keyChangedTip = tracker.getOrCreateKey(payloadChangedTip)
        assertNotEquals("Cambio en tipMinor genera clave nueva", key1, keyChangedTip)

        // Cambiar monto
        val payloadChangedAmount = CashPaymentPayload("sess-1", 1200L, 200L, "Mesa 1")
        val keyChangedAmount = tracker.getOrCreateKey(payloadChangedAmount)
        assertNotEquals("Cambio en amountMinor genera clave nueva", keyChangedTip, keyChangedAmount)

        // Cambiar etiqueta
        val payloadChangedLabel = CashPaymentPayload("sess-1", 1200L, 200L, "Mesa 2")
        val keyChangedLabel = tracker.getOrCreateKey(payloadChangedLabel)
        assertNotEquals("Cambio en payerLabel genera clave nueva", keyChangedAmount, keyChangedLabel)
    }

    @Test
    fun `anti-reentry guard prevents concurrent execution when already processing`() = runTest {
        val mockSession = StaffSessionDetail(
            session = StaffSession(id = "sess-1", status = "OPEN", tableLabel = "Mesa 4", openedAt = "12:00"),
            bill = Bill(currency = "ARS", chargesMinor = 1090, balanceMinor = 1090)
        )
        fakeApi.sessionDetail = mockSession

        // Disparo inicial
        viewModel.processPayment("sess-1", amountMinor = 1090L)

        // Intento concurrente mientras está en proceso
        viewModel.processPayment("sess-1", amountMinor = 2000L)

        // El payload retenido no debe haber cambiado al segundo porque fue ignorado por el guard
        val payload = viewModel.idempotencyTracker.currentPayload() as? CashPaymentPayload
        assertEquals(1090L, payload?.amountMinor)

        testScheduler.advanceUntilIdle()
    }

    @Test
    fun `applyDiscountPercent sends adjustment to backend without 15 percent client limitation`() = runTest {
        val mockSession = StaffSessionDetail(
            session = StaffSession(id = "sess-1", status = "OPEN", tableLabel = "Mesa 4", openedAt = "12:00"),
            bill = Bill(currency = "ARS", chargesMinor = 1090, balanceMinor = 1090)
        )
        fakeApi.sessionDetail = mockSession

        // Un mozo o cualquier usuario puede enviar 25% y es el backend quien decide
        viewModel.applyDiscountPercent("sess-1", percent = 25, reasonCode = AdjustmentReasonCode.PROMOTION.code)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Descuento del 25% aplicado correctamente", state.paymentSuccessMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun `problem json mapping extracts contractual error codes correctly`() = runTest {
        fakeApi.failureToThrow = ApiFailure(403, "ADJUSTMENT_LIMIT_EXCEEDED", "Límite excedido")

        viewModel.processPayment("sess-1", 1000L)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("ADJUSTMENT_LIMIT_EXCEEDED", state.lastErrorCode)
        assertTrue(state.errorMessage!!.contains("ADJUSTMENT_LIMIT_EXCEEDED"))
    }

    @Test
    fun `uncertain result is mapped properly for unreadable 2xx response`() = runTest {
        fakeApi.failureToThrow = ApiFailure(200, "UNCERTAIN_RESULT", "Respuesta corrupta")

        viewModel.processPayment("sess-1", 1000L)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("UNCERTAIN_RESULT", state.lastErrorCode)
        assertTrue(state.errorMessage!!.contains("Resultado incierto"))
    }

    @Test
    fun `formatMoney formats minor units correctly`() {
        val formatted = formatMoney(1090L, "ARS")
        assertEquals("$10.90", formatted)
    }
}
