package com.somphie.staff.feature.orders.data

import com.somphie.staff.core.network.AcceptOrderRequest
import com.somphie.staff.core.network.ApiFailure
import com.somphie.staff.core.network.RejectOrderRequest
import com.somphie.staff.core.network.StaffApi
import com.somphie.staff.core.network.StaffOrder
import com.somphie.staff.core.network.StaffOrderPage
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdersRepository @Inject constructor(
    private val api: StaffApi
) {
    suspend fun getOrders(status: String? = null, cursor: String? = null): Result<StaffOrderPage> =
        runCatching { api.getOrders(status = status, cursor = cursor) }

    suspend fun getOrder(orderId: String): Result<StaffOrder> =
        runCatching { api.getOrder(orderId) }

    suspend fun acceptOrder(orderId: String, idempotencyKey: String = UUID.randomUUID().toString()): Result<StaffOrder> =
        runCatching { api.acceptOrder(orderId, idempotencyKey, AcceptOrderRequest()) }

    suspend fun rejectOrder(
        orderId: String,
        reasonCode: String,
        reasonText: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<StaffOrder> =
        runCatching {
            api.rejectOrder(
                orderId,
                idempotencyKey,
                RejectOrderRequest(reasonCode = reasonCode, reasonText = reasonText)
            )
        }
}