package com.somphie.staff.core.sse

import com.somphie.staff.core.storage.EncryptedTokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Administrador de conexión en tiempo real por Server-Sent Events (SSE - WP-AND-60)
@Singleton
class StaffSseManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenStore: EncryptedTokenStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ServerEvent> = _events.asSharedFlow()

    private var isConnected = false

    // Iniciar la escucha del stream SSE
    fun startStreaming(baseUrl: String) {
        if (isConnected) return
        isConnected = true

        scope.launch {
            while (isActive) {
                try {
                    val tokens = tokenStore.current()
                    val token = tokens?.accessToken
                    val streamUrl = if (baseUrl.endsWith("/")) "${baseUrl}events/stream" else "$baseUrl/events/stream"

                    val requestBuilder = Request.Builder()
                        .url(streamUrl)
                        .header("Accept", "text/event-stream")

                    if (!token.isNullOrBlank()) {
                        requestBuilder.header("Authorization", "Bearer $token")
                    }

                    val request = requestBuilder.build()
                    val response = okHttpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        val source = response.body?.source()
                        if (source != null) {
                            var currentEvent = "message"
                            var currentData = StringBuilder()

                            while (!source.exhausted() && isActive) {
                                val line = source.readUtf8Line() ?: break
                                when {
                                    line.startsWith("event:") -> {
                                        currentEvent = line.substring(6).trim()
                                    }
                                    line.startsWith("data:") -> {
                                        currentData.append(line.substring(5).trim())
                                    }
                                    line.isEmpty() -> {
                                        // Línea en blanco marca el fin de un evento SSE
                                        if (currentData.isNotEmpty()) {
                                            _events.emit(ServerEvent(event = currentEvent, data = currentData.toString()))
                                            currentData = StringBuilder()
                                            currentEvent = "message"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    response.close()
                } catch (e: IOException) {
                    // Error de red o desconexión temporal; reintentar tras un intervalo con backoff
                } catch (e: Exception) {
                    // Excepción general
                }

                // Esperar 4 segundos antes de reintentar la conexión SSE
                delay(4000)
            }
        }
    }

    fun stopStreaming() {
        isConnected = false
    }
}
