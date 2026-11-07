package com.somphie.staff.core.sse

import kotlinx.serialization.Serializable

// Modelo de evento recibido por Server-Sent Events (SSE)
@Serializable
data class ServerEvent(
    val event: String = "message",
    val data: String,
    val id: String? = null
)
