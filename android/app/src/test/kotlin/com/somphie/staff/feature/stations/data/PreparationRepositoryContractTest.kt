package com.somphie.staff.feature.stations.data

import com.somphie.staff.core.network.StaffApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class PreparationRepositoryContractTest {
    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private lateinit var repository: PreparationRepository

    @Before
    fun setUp() {
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(StaffApi::class.java)
        repository = PreparationRepository(api, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `queue uses station path requested statuses and tolerates unknown status values`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "items": [{
                    "itemId": "item-1",
                    "orderId": "order-1",
                    "orderCode": "#042",
                    "tableLabel": "Mesa 7",
                    "productName": "Flat white",
                    "quantity": 1,
                    "status": "FUTURE_STATUS",
                    "queuedAt": null,
                    "delayed": null,
                    "optionsSummary": "",
                    "options": []
                  }],
                  "nextCursor": null
                }
                """.trimIndent(),
            ),
        )

        val page = repository.queue("station-bar", listOf("QUEUED", "IN_PREPARATION", "READY"))
        val request = server.takeRequest()

        assertEquals(
            "/api/v1/staff/stations/station-bar/queue",
            request.requestUrl?.encodedPath,
        )
        assertEquals("QUEUED,IN_PREPARATION,READY", request.requestUrl?.queryParameter("status"))
        assertEquals("100", request.requestUrl?.queryParameter("limit"))
        assertEquals("FUTURE_STATUS", page.items.single().status)
    }

    @Test
    fun `start sends the caller idempotency key to the contracted item route`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"id":"order-1","status":"IN_PREPARATION","version":2,"items":[]}""",
            ),
        )
        val action = PendingPreparationAction(
            itemId = "item-1",
            productName = "Flat white",
            orderCode = "#042",
            action = PreparationAction.START,
            idempotencyKey = "67fb8c8a-8904-4b84-9798-bb6aa6e9570a",
        )

        repository.perform(action)
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/v1/staff/order-items/item-1/start", request.requestUrl?.encodedPath)
        assertEquals(action.idempotencyKey, request.getHeader("Idempotency-Key"))
    }

    @Test
    fun `cancel sends the required reason and idempotency key`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"id":"order-1","status":"ACCEPTED","version":3,"items":[]}""",
            ),
        )
        val action = PendingPreparationAction(
            itemId = "item-1",
            productName = "Flat white",
            orderCode = "#042",
            action = PreparationAction.CANCEL,
            idempotencyKey = "fdc44e25-610b-48c1-843c-c99791acb602",
            reason = "Se cayó al piso",
        )

        repository.perform(action)
        val request = server.takeRequest()

        assertEquals("/api/v1/staff/order-items/item-1/cancel", request.requestUrl?.encodedPath)
        assertEquals(action.idempotencyKey, request.getHeader("Idempotency-Key"))
        assertEquals("""{"reason":"Se cayó al piso"}""", request.body.readUtf8())
    }
}
