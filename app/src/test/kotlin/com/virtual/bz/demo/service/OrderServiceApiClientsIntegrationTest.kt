package com.virtual.bz.demo.service

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.virtual.bz.demo.client.InventoryClient
import com.virtual.bz.demo.client.PaymentClient
import com.virtual.bz.demo.exceptions.OrderProcessingException
import com.virtual.bz.demo.service.domain.FailureReason
import com.virtual.bz.demo.service.domain.Order
import com.virtual.bz.demo.service.domain.OrderStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import java.time.Duration
import java.util.*

@WireMockTest
class OrderServiceApiClientsIntegrationTest {

    private val orderRepositoryService = mockk<OrderRepositoryService>(relaxed = true)

    private lateinit var paymentClient: PaymentClient
    private lateinit var inventoryClient: InventoryClient
    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp(wmRuntimeInfo: WireMockRuntimeInfo) {
        val builder = RestClient.builder()
        val baseUrl = wmRuntimeInfo.httpBaseUrl
        val timeout = Duration.ofSeconds(10)

        paymentClient = PaymentClient(builder, baseUrl, timeout)
        inventoryClient = InventoryClient(builder, baseUrl, timeout)
        orderService = OrderService(orderRepositoryService, paymentClient, inventoryClient)
    }

    @Test
    fun `processOrder should succeed when both payment and inventory clients succeed`() {
        val orderId = UUID.randomUUID()
        val itemId = "item-1"
        val paymentId = "pay-1"
        val order = Order(id = orderId, itemId = itemId, status = OrderStatus.PENDING)
        every { orderRepositoryService.markAsProcessing(orderId) } returns order
        every {
            orderRepositoryService.markAsComplete(
                orderId,
                paymentId
            )
        } returns order.copy(status = OrderStatus.COMPLETED, paymentId = paymentId)

        // Payment Success
        stubFor(
            post(urlEqualTo("/payment/execute"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "$paymentId"}""")
                )
        )

        // Inventory Success
        stubFor(
            post(urlEqualTo("/inventory/reserve"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "inv-1"}""")
                )
        )

        val result = orderService.processOrder(orderId)

        assertEquals(OrderStatus.COMPLETED, result.status)
        assertEquals(paymentId, result.paymentId)

        verify(postRequestedFor(urlEqualTo("/payment/execute")))
        verify(postRequestedFor(urlEqualTo("/inventory/reserve")))
        verify(exactly = 1) { orderRepositoryService.markAsComplete(orderId, paymentId) }
    }

    @Test
    fun `processOrder should trigger failure when payment client fails`() {
        val orderId = UUID.randomUUID()
        val order = Order(id = orderId, itemId = "item-1", status = OrderStatus.PENDING)
        every { orderRepositoryService.markAsProcessing(orderId) } returns order
        every { orderRepositoryService.markAsFailed(orderId, FailureReason.PAYMENT_FAILURE) } returns mockk()

        stubFor(
            post(urlEqualTo("/payment/execute"))
                .willReturn(aResponse().withStatus(500))
        )

        stubFor(
            post(urlEqualTo("/inventory/reserve"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "inv-1"}""")
                )
        )

        stubFor(
            post(urlEqualTo("/inventory/rollback"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "inv-rollback-1"}""")
                )
        )

        val exception = assertThrows(OrderProcessingException::class.java) {
            orderService.processOrder(orderId)
        }

        assertEquals("Payment failed for orderId=$orderId", exception.message)

        verify(postRequestedFor(urlEqualTo("/inventory/reserve")))
        verify(postRequestedFor(urlEqualTo("/inventory/rollback")))
        verify(exactly = 1) { orderRepositoryService.markAsFailed(orderId, FailureReason.PAYMENT_FAILURE) }
    }

    @Test
    fun `processOrder should trigger failure and rollback payment when inventory client fails`() {
        val orderId = UUID.randomUUID()
        val order = Order(id = orderId, itemId = "item-1", status = OrderStatus.PENDING)
        every { orderRepositoryService.markAsProcessing(orderId) } returns order
        every { orderRepositoryService.markAsFailed(orderId, FailureReason.INVENTORY_FAILURE) } returns mockk()

        stubFor(
            post(urlEqualTo("/payment/execute"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "pay-1"}""")
                )
        )

        stubFor(
            post(urlEqualTo("/inventory/reserve"))
                .willReturn(aResponse().withStatus(500))
        )

        stubFor(
            post(urlEqualTo("/payment/rollback"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "pay-rollback-1"}""")
                )
        )

        val exception = assertThrows(OrderProcessingException::class.java) {
            orderService.processOrder(orderId)
        }

        assertEquals("Inventory failed for orderId=$orderId", exception.message)

        verify(postRequestedFor(urlEqualTo("/payment/execute")))
        verify(postRequestedFor(urlEqualTo("/payment/rollback")))
        verify(exactly = 1) { orderRepositoryService.markAsFailed(orderId, FailureReason.INVENTORY_FAILURE) }
    }

    @Test
    fun `processOrder should trigger failure when both payment and inventory clients fail`() {
        val orderId = UUID.randomUUID()
        val order = Order(id = orderId, itemId = "item-1", status = OrderStatus.PENDING)
        every { orderRepositoryService.markAsProcessing(orderId) } returns order
        every {
            orderRepositoryService.markAsFailed(
                orderId,
                FailureReason.PAYMENT_AND_INVENTORY_FAILURE
            )
        } returns mockk()

        stubFor(
            post(urlEqualTo("/payment/execute"))
                .willReturn(aResponse().withStatus(500))
        )

        stubFor(
            post(urlEqualTo("/inventory/reserve"))
                .willReturn(aResponse().withStatus(500))
        )

        val exception = assertThrows(OrderProcessingException::class.java) {
            orderService.processOrder(orderId)
        }

        assertEquals("Payment and Inventory failed for orderId=$orderId", exception.message)

        verify(postRequestedFor(urlEqualTo("/payment/execute")))
        verify(postRequestedFor(urlEqualTo("/inventory/reserve")))
        verify(exactly = 1) {
            orderRepositoryService.markAsFailed(
                orderId,
                FailureReason.PAYMENT_AND_INVENTORY_FAILURE
            )
        }
    }
}
