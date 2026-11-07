package com.somphie.staff.feature.availability.data

import com.somphie.staff.core.network.ActionResult
import com.somphie.staff.core.network.OptionAvailabilityRequest
import com.somphie.staff.core.network.ProductAvailabilityRequest
import com.somphie.staff.core.network.StaffApi
import com.somphie.staff.core.network.StaffProductAvailability
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Repositorio para la gestión de disponibilidad de productos u opciones (SC-S-11)
@Singleton
class AvailabilityRepository @Inject constructor(
    private val api: StaffApi
) {
    // Actualizar la disponibilidad de un producto (habilitar/deshabilitar)
    suspend fun updateProductAvailability(
        productId: String,
        available: Boolean,
        reasonCode: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<ActionResult> = runCatching {
        val response = api.updateProductAvailability(
            productId = productId,
            idempotencyKey = idempotencyKey,
            request = ProductAvailabilityRequest(
                available = available,
                reasonCode = reasonCode
            )
        )
        if (response.isSuccessful && response.body() != null) {
            response.body()!!
        } else {
            throw Exception("Error al actualizar disponibilidad del producto: ${response.code()}")
        }
    }

    // Actualizar la disponibilidad de una opción (ej: Leche de almendra)
    suspend fun updateOptionAvailability(
        optionId: String,
        available: Boolean,
        reasonCode: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<ActionResult> = runCatching {
        val response = api.updateOptionAvailability(
            optionId = optionId,
            idempotencyKey = idempotencyKey,
            request = OptionAvailabilityRequest(
                available = available,
                reasonCode = reasonCode
            )
        )
        if (response.isSuccessful && response.body() != null) {
            response.body()!!
        } else {
            throw Exception("Error al actualizar disponibilidad de la opción: ${response.code()}")
        }
    }
}
