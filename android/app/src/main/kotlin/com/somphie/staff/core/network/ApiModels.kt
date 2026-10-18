package com.somphie.staff.core.network

import kotlinx.serialization.Serializable

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSec: Long,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSec: Long,
    val user: StaffProfile,
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class StaffProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val roles: List<String>,
    val permissions: List<String>,
    val stations: List<Station>,
    val mustChangePassword: Boolean,
)

@Serializable
data class Station(val id: String, val code: String, val name: String)

@Serializable
data class StationListResponse(val items: List<Station>)

@Serializable
data class QueueOption(
    val groupId: String,
    val optionId: String,
    val groupName: String,
    val optionName: String,
    val priceDeltaMinor: Long? = null,
)

@Serializable
data class StationQueueItem(
    val itemId: String,
    val orderId: String,
    val orderCode: String,
    val tableLabel: String,
    val productName: String,
    val quantity: Int,
    val note: String? = null,
    val status: String,
    val queuedAt: String? = null,
    val delayed: Boolean? = null,
    val optionsSummary: String = "",
    val options: List<QueueOption> = emptyList(),
)

@Serializable
data class StationQueueResponse(
    val items: List<StationQueueItem>,
    val nextCursor: String? = null,
)

@Serializable
data class CancelItemRequest(val reason: String)

@Serializable
data class ActionResult(
    val id: String,
    val status: String,
    val version: Long,
    val items: List<ActionItem>,
)

@Serializable
data class ActionItem(val id: String, val status: String)

@Serializable
data class ApiProblem(
    val type: String = "",
    val title: String = "",
    val status: Int = 0,
    val code: String = "UNKNOWN_ERROR",
    val detail: String? = null,
)
