package com.somphie.staff.feature.tables.data

import com.somphie.staff.core.network.StaffApi
import com.somphie.staff.core.network.StaffTable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TablesRepository @Inject constructor(
    private val api: StaffApi
) {
    suspend fun getTables(): Result<List<StaffTable>> =
        runCatching { api.getTables().items }
}