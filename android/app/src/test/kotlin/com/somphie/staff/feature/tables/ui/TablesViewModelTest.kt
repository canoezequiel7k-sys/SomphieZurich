package com.somphie.staff.feature.tables.ui

import com.somphie.staff.TestDispatcherRule
import com.somphie.staff.core.network.StaffTable
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
class TablesViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    @Test
    fun `loadTables success populates tables list`() = runTest(dispatcherRule.dispatcher) {
        val fakeTables = listOf(
            StaffTable(tableId = "t1", label = "1", displayStatus = "FREE"),
            StaffTable(tableId = "t2", label = "2", displayStatus = "OCCUPIED")
        )
        val fakeApi = FakeStaffApi().apply {
            tablesList = fakeTables
        }
        val repository = TablesRepository(fakeApi)
        val viewModel = TablesViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TablesUiState.Success)
        assertEquals(2, (state as TablesUiState.Success).tables.size)
    }
}
