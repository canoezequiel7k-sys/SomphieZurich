package com.somphie.staff.feature.billing.data

import com.somphie.staff.core.network.AdjustmentReasonCode
import com.somphie.staff.core.network.ApiFailure
import com.somphie.staff.core.network.StaffApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class BillingRepositoryContractTest {

    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private lateinit var repository: BillingRepository

    @Before
    fun setUp() {
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(StaffApi::class.java)
        repository = BillingRepository(api, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `createPayment sends correct path, header and cash body`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "payment": {
                    "id": "pay-1",
                    "kind": "PAYMENT",
                    "method": "CASH_COUNTER",
                    "status": "SUCCEEDED",
                    "amountMinor": 1000,
                    "tipMinor": 100,
                    "payerLabel": "Mesa 4",
                    "createdAt": "2026-11-08T12:00:00Z"
                  },
                  "bill": {
                    "currency": "ARS",
                    "chargesMinor": 1000,
                    "adjustmentsMinor": 0,
                    "paidMinor": 1000,
                    "balanceMinor": 0,
                    "tipsMinor": 100,
                    "lines": [],
                    "adjustments": [],
                    "payments": [],
                    "pendingOrdersNotIncluded": []
                  }
                }
                """.trimIndent()
            )
        )

        val result = repository.createPayment(
            sessionId = "sess-123",
            idempotencyKey = "key-abc-1",
            amountMinor = 1000L,
            tipMinor = 100L,
            payerLabel = "Mesa 4"
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/staff/sessions/sess-123/payments", request.requestUrl?.encodedPath)
        assertEquals("key-abc-1", request.getHeader("Idempotency-Key"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"amountMinor\":1000"))
        assertTrue(body.contains("\"tipMinor\":100"))
        assertTrue(body.contains("\"payerLabel\":\"Mesa 4\""))

        assertTrue(result.isSuccess)
        val res = result.getOrThrow()
        assertEquals("pay-1", res.payment.id)
        assertEquals(0L, res.bill.balanceMinor)
    }

    @Test
    fun `voidPayment sends correct path, header and ReasonRequest body`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "payment": {
                    "id": "pay-1",
                    "kind": "PAYMENT",
                    "method": "CASH_COUNTER",
                    "status": "VOIDED",
                    "amountMinor": 1000,
                    "tipMinor": 0,
                    "createdAt": "2026-11-08T12:05:00Z"
                  },
                  "bill": {
                    "currency": "ARS",
                    "chargesMinor": 1000,
                    "adjustmentsMinor": 0,
                    "paidMinor": 0,
                    "balanceMinor": 1000,
                    "tipsMinor": 0,
                    "lines": [],
                    "adjustments": [],
                    "payments": [],
                    "pendingOrdersNotIncluded": []
                  }
                }
                """.trimIndent()
            )
        )

        val result = repository.voidPayment(
            paymentId = "pay-1",
            idempotencyKey = "key-void-1",
            reason = "Error de digitación en caja"
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/staff/payments/pay-1/void", request.requestUrl?.encodedPath)
        assertEquals("key-void-1", request.getHeader("Idempotency-Key"))
        assertTrue(request.body.readUtf8().contains("\"reason\":\"Error de digitación en caja\""))

        assertTrue(result.isSuccess)
        assertEquals("VOIDED", result.getOrThrow().payment.status)
    }

    @Test
    fun `refundPayment sends correct path, header and CreateRefundRequest body`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "payment": {
                    "id": "ref-1",
                    "kind": "REFUND",
                    "method": "CASH_COUNTER",
                    "status": "SUCCEEDED",
                    "amountMinor": 500,
                    "tipMinor": 0,
                    "createdAt": "2026-11-08T12:10:00Z"
                  },
                  "bill": {
                    "currency": "ARS",
                    "chargesMinor": 1000,
                    "adjustmentsMinor": 0,
                    "paidMinor": 500,
                    "balanceMinor": 500,
                    "tipsMinor": 0,
                    "lines": [],
                    "adjustments": [],
                    "payments": [],
                    "pendingOrdersNotIncluded": []
                  }
                }
                """.trimIndent()
            )
        )

        val result = repository.refundPayment(
            sessionId = "sess-123",
            idempotencyKey = "key-ref-1",
            amountMinor = 500L,
            reason = "Devolución por plato demorado"
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/staff/sessions/sess-123/refunds", request.requestUrl?.encodedPath)
        assertEquals("key-ref-1", request.getHeader("Idempotency-Key"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"amountMinor\":500"))
        assertTrue(body.contains("\"reason\":\"Devolución por plato demorado\""))

        assertTrue(result.isSuccess)
        assertEquals(500L, result.getOrThrow().payment.amountMinor)
    }

    @Test
    fun `closeSession sends POST without body and without Idempotency-Key`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "id": "sess-123",
                  "status": "CLOSED",
                  "tableLabel": "Mesa 4",
                  "openedAt": "2026-11-08T10:00:00Z",
                  "closedAt": "2026-11-08T12:00:00Z"
                }
                """.trimIndent()
            )
        )

        val result = repository.closeSession(sessionId = "sess-123")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/staff/sessions/sess-123/close", request.requestUrl?.encodedPath)
        assertNull("Cierre normal NO debe enviar Idempotency-Key", request.getHeader("Idempotency-Key"))
        assertEquals("Cierre normal NO debe enviar cuerpo", "", request.body.readUtf8())

        assertTrue(result.isSuccess)
        assertEquals("CLOSED", result.getOrThrow().status)
    }

    @Test
    fun `forceCloseSession sends POST with Idempotency-Key and ReasonRequest body`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "id": "sess-123",
                  "status": "CLOSED",
                  "tableLabel": "Mesa 4",
                  "openedAt": "2026-11-08T10:00:00Z",
                  "closedAt": "2026-11-08T12:00:00Z"
                }
                """.trimIndent()
            )
        )

        val result = repository.forceCloseSession(
            sessionId = "sess-123",
            idempotencyKey = "key-force-close-1",
            reason = "El cliente se fue sin pagar"
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/staff/sessions/sess-123/force-close", request.requestUrl?.encodedPath)
        assertEquals("key-force-close-1", request.getHeader("Idempotency-Key"))
        assertTrue(request.body.readUtf8().contains("\"reason\":\"El cliente se fue sin pagar\""))

        assertTrue(result.isSuccess)
        assertEquals("CLOSED", result.getOrThrow().status)
    }

    @Test
    fun `createAdjustment sends POST with Idempotency-Key and CreateAdjustmentRequest body`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": "adj-1",
                  "kind": "DISCOUNT_PERCENT",
                  "status": "APPLIED",
                  "amountMinor": 100,
                  "percentBp": 1000,
                  "reasonCode": "SERVICE_ISSUE",
                  "reasonText": "Demora en la entrega",
                  "appliedAt": "2026-11-08T11:45:00Z"
                }
                """.trimIndent()
            )
        )

        val result = repository.createAdjustment(
            sessionId = "sess-123",
            idempotencyKey = "key-adj-1",
            kind = "DISCOUNT_PERCENT",
            percentBp = 1000,
            reasonCode = AdjustmentReasonCode.SERVICE_ISSUE.code,
            reasonText = "Demora en la entrega"
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/staff/sessions/sess-123/adjustments", request.requestUrl?.encodedPath)
        assertEquals("key-adj-1", request.getHeader("Idempotency-Key"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"kind\":\"DISCOUNT_PERCENT\""))
        assertTrue(body.contains("\"percentBp\":1000"))
        assertTrue(body.contains("\"reasonCode\":\"SERVICE_ISSUE\""))

        assertTrue(result.isSuccess)
        val adj = result.getOrThrow()
        assertEquals("adj-1", adj.id)
        assertEquals("APPLIED", adj.status)
    }

    @Test
    fun `voidAdjustment sends POST with Idempotency-Key and ReasonRequest body`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "id": "adj-1",
                  "kind": "DISCOUNT_PERCENT",
                  "status": "VOIDED",
                  "amountMinor": 100,
                  "percentBp": 1000,
                  "reasonCode": "SERVICE_ISSUE",
                  "appliedAt": "2026-11-08T11:45:00Z",
                  "voidedAt": "2026-11-08T12:00:00Z",
                  "voidReason": "Error administrativo"
                }
                """.trimIndent()
            )
        )

        val result = repository.voidAdjustment(
            adjustmentId = "adj-1",
            idempotencyKey = "key-void-adj-1",
            reason = "Error administrativo"
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/staff/adjustments/adj-1/void", request.requestUrl?.encodedPath)
        assertEquals("key-void-adj-1", request.getHeader("Idempotency-Key"))
        assertTrue(request.body.readUtf8().contains("\"reason\":\"Error administrativo\""))

        assertTrue(result.isSuccess)
        assertEquals("VOIDED", result.getOrThrow().status)
    }

    @Test
    fun `Problem+JSON 403 maps correctly to ApiFailure with ADJUSTMENT_LIMIT_EXCEEDED`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(403).setHeader("Content-Type", "application/problem+json").setBody(
                """
                {
                  "type": "https://api.somphiezurich.example/errors/adjustment-limit-exceeded",
                  "title": "Requiere autorización del Dueño",
                  "status": 403,
                  "code": "ADJUSTMENT_LIMIT_EXCEEDED",
                  "traceId": "9f1c2a7e4b",
                  "extra": {"limitPercent": 15, "limitMinor": 750000}
                }
                """.trimIndent()
            )
        )

        val result = repository.createAdjustment(
            sessionId = "sess-123",
            idempotencyKey = "key-adj-exceeded",
            kind = "DISCOUNT_PERCENT",
            percentBp = 2000,
            reasonCode = "STAFF"
        )

        assertTrue(result.isFailure)
        val failure = result.exceptionOrNull() as? ApiFailure
        assertNotNull(failure)
        assertEquals(403, failure?.status)
        assertEquals("ADJUSTMENT_LIMIT_EXCEEDED", failure?.code)
    }

    @Test
    fun `2xx with unreadable body maps to ApiFailure UNCERTAIN_RESULT`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("Cuerpo no parseable o truncado")
        )

        val result = repository.closeSession("sess-123")

        assertTrue(result.isFailure)
        val failure = result.exceptionOrNull() as? ApiFailure
        assertNotNull(failure)
        assertEquals(200, failure?.status)
        assertEquals("UNCERTAIN_RESULT", failure?.code)
    }
}
