package com.virtual.bz.demo

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.virtual.bz.demo.controller.dto.CreateOrderDto
import com.virtual.bz.demo.controller.dto.OrderDto
import com.virtual.bz.demo.controller.dto.OrderStatusDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@Sql(scripts = ["classpath:db/test_cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderE2ETest {

    @Autowired
    lateinit var testRestClient: RestTestClient

    companion object {
        @RegisterExtension
        val wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("clients.inventory.url") { wireMock.baseUrl() }
            registry.add("clients.payment.url") { wireMock.baseUrl() }
        }
    }

    @Test
    fun `should create and process order successfully`() {
        // Given
        val itemId = "item-123"

        // Mock Inventory
        wireMock.stubFor(
            post(urlEqualTo("/inventory/reserve"))
                .withRequestBody(matchingJsonPath("$.itemId", equalTo(itemId)))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "inv-1"}""")
                )
        )

        // Mock Payment
        wireMock.stubFor(
            post(urlEqualTo("/payment/execute"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "pay-1"}""")
                )
        )

        // When: Create Order
        val orderDto = testRestClient.post()
            .uri("/api/orders")
            .body(CreateOrderDto(itemId))
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<OrderDto>()
            .returnResult()
            .responseBody
        val orderId = orderDto?.id
        assertNotNull(orderId)

        // When: Process Order
        val processResponse = testRestClient.post()
            .uri("/api/orders/$orderId/process")
            .body(CreateOrderDto(itemId))
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<OrderDto>()
            .returnResult()
            .responseBody

        // Then
        assertEquals(OrderStatusDto.COMPLETED, processResponse!!.status)
        assertEquals("pay-1", processResponse.paymentId)
    }

    @Test
    fun `should fail order processing and rollback when inventory fails`() {
        // Given
        val itemId = "item-fail"

        // Mock Inventory Failure
        wireMock.stubFor(
            post(urlEqualTo("/inventory/reserve"))
                .willReturn(aResponse().withStatus(500))
        )

        // Mock Payment Success
        wireMock.stubFor(
            post(urlEqualTo("/payment/execute"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "pay-fail"}""")
                )
        )

        // Mock Payment Rollback
        wireMock.stubFor(
            post(urlEqualTo("/payment/rollback"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "rollback-pay-1"}""")
                )
        )

        // When: Create Order
        val createResponse = testRestClient.post()
            .uri("/api/orders")
            .body(CreateOrderDto(itemId))
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<OrderDto>()
            .returnResult()
            .responseBody
        val orderId = createResponse!!.id

        // When: Process Order
        testRestClient.post()
            .uri("/api/orders/$orderId/process")
            .body(CreateOrderDto(itemId))
            .exchange()
            .expectStatus().is5xxServerError

        // Verify final status in DB
        val getResponse = testRestClient.get()
            .uri("/api/orders/$orderId")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<OrderDto>()
            .returnResult()
            .responseBody
        assertEquals(OrderStatusDto.FAILED, getResponse!!.status)

        // Verify rollback was called
        wireMock.verify(postRequestedFor(urlEqualTo("/payment/rollback")))
    }
}
