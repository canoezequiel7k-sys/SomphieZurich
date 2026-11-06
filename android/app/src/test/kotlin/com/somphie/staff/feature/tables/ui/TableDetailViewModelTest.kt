package com.somphie.staff.feature.tables.ui

import com.somphie.staff.TestDispatcherRule
import com.somphie.staff.core.network.Bill
import com.somphie.staff.core.network.StaffSession
import com.somphie.staff.core.network.StaffSessionDetail
import com.somphie.staff.feature.orders.ui.FakeStaffApi
import com.somphie.staff.feature.tables.data.TablesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TableDetailViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    @Test
    fun `loadTableDetail success updates state`() = runTest(dispatcherRule.dispatcher) {
        val sampleDetail = StaffSessionDetail(
            session = StaffSession("s1", "OPEN", "Mesa 4", "2026-10-18T12:00:00Z"),
            orders = emptyList(),
            bill = Bill(balanceMinor = 1090)
        )
        val fakeApi = FakeStaffApi().apply {
            sessionDetail = sampleDetail
        }
        val repository = TablesRepository(fakeApi)
        val viewModel = TableDetailViewModel(repository)

        viewModel.loadTableDetail("s1", "Mesa 4")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TableDetailUiState.Success)
        assertEquals("Mesa 4", (state as TableDetailUiState.Success).tableLabel)
        assertEquals(1090L, (state as TableDetailUiState.Success).sessionDetail.bill.balanceMinor)
    }
}
