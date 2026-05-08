package com.virtual.bz.demo.service

import com.virtual.bz.demo.client.InventoryClient
import com.virtual.bz.demo.client.PaymentClient
import com.virtual.bz.demo.exceptions.OrderProcessingException
import com.virtual.bz.demo.service.domain.FailureReason
import com.virtual.bz.demo.service.domain.Order
import com.virtual.bz.demo.service.domain.OrderStatus
import com.virtual.bz.demo.service.domain.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class OrderServiceTest {

    private val orderRepositoryService = mockk<OrderRepositoryService>()
    private val paymentClient = mockk<PaymentClient>()
    private val inventoryClient = mockk<InventoryClient>()

    private val orderService = OrderService(
        orderRepositoryService,
        paymentClient,
        inventoryClient,
    )

    @Test
    fun `processOrder should succeed when both payment and inventory are successful`() {
        val orderId = UUID.randomUUID()
        val itemId = "item-1"
        val paymentId = "payment-1"
        val reservationId = "reservation-1"
        val order = Order(id = orderId, itemId = itemId, status = OrderStatus.PENDING)
        val completedOrder = order.copy(status = OrderStatus.COMPLETED, paymentId = paymentId)

        every { orderRepositoryService.markAsProcessing(orderId) } returns order
        every { paymentClient.executePayment(orderId) } returns Result.Success(paymentId)
        every { inventoryClient.executeReservation(itemId) } returns Result.Success(reservationId)
        every { orderRepositoryService.markAsComplete(orderId, paymentId) } returns completedOrder

        val result = orderService.processOrder(orderId)

        assertEquals(completedOrder, result)
        verify { orderRepositoryService.markAsProcessing(orderId) }
        verify { paymentClient.executePayment(orderId) }
        verify { inventoryClient.executeReservation(itemId) }
        verify { orderRepositoryService.markAsComplete(orderId, paymentId) }
    }

    @Test
    fun `processOrder should fail and rollback payment when inventory fails`() {
        val orderId = UUID.randomUUID()
        val itemId = "item-1"
        val paymentId = "payment-1"
        val order = Order(id = orderId, itemId = itemId, status = OrderStatus.PENDING)

        every { orderRepositoryService.markAsProcessing(orderId) } returns order
        every { paymentClient.executePayment(orderId) } returns Result.Success(paymentId)
        every { inventoryClient.executeReservation(itemId) } returns Result.Failure("error")
        every { orderRepositoryService.markAsFailed(orderId, FailureReason.INVENTORY_FAILURE) } returns mockk()
        every { paymentClient.rollbackPayment(paymentId) } returns Result.Success("rollback-1")

        assertThrows<OrderProcessingException> {
            orderService.processOrder(orderId)
        }

        verify { orderRepositoryService.markAsProcessing(orderId) }
        verify { paymentClient.executePayment(orderId) }
        verify { inventoryClient.executeReservation(itemId) }
        verify { orderRepositoryService.markAsFailed(orderId, FailureReason.INVENTORY_FAILURE) }
        verify { paymentClient.rollbackPayment(paymentId) }
    }

    @Test
    fun `processOrder should fail and rollback inventory when payment fails`() {
        val orderId = UUID.randomUUID()
        val itemId = "item-1"
        val reservationId = "reservation-1"
        val order = Order(id = orderId, itemId = itemId, status = OrderStatus.PENDING)

        every { orderRepositoryService.markAsProcessing(orderId) } returns order
        every { paymentClient.executePayment(orderId) } returns Result.Failure("error")
        every { inventoryClient.executeReservation(itemId) } returns Result.Success(reservationId)
        every { orderRepositoryService.markAsFailed(orderId, FailureReason.PAYMENT_FAILURE) } returns mockk()
        every { inventoryClient.rollbackInventory(reservationId) } returns Result.Success("rollback-1")

        assertThrows<OrderProcessingException> {
            orderService.processOrder(orderId)
        }

        verify { orderRepositoryService.markAsProcessing(orderId) }
        verify { paymentClient.executePayment(orderId) }
        verify { inventoryClient.executeReservation(itemId) }
        verify { orderRepositoryService.markAsFailed(orderId, FailureReason.PAYMENT_FAILURE) }
        verify { inventoryClient.rollbackInventory(reservationId) }
    }

    @Test
    fun `processOrder should fail when both payment and inventory fail`() {
        val orderId = UUID.randomUUID()
        val itemId = "item-1"
        val order = Order(id = orderId, itemId = itemId, status = OrderStatus.PENDING)

        every { orderRepositoryService.markAsProcessing(orderId) } returns order
        every { paymentClient.executePayment(orderId) } returns Result.Failure("error")
        every { inventoryClient.executeReservation(itemId) } returns Result.Failure("error")
        every {
            orderRepositoryService.markAsFailed(
                orderId,
                FailureReason.PAYMENT_AND_INVENTORY_FAILURE
            )
        } returns mockk()

        assertThrows<OrderProcessingException> {
            orderService.processOrder(orderId)
        }

        verify { orderRepositoryService.markAsProcessing(orderId) }
        verify { paymentClient.executePayment(orderId) }
        verify { inventoryClient.executeReservation(itemId) }
        verify { orderRepositoryService.markAsFailed(orderId, FailureReason.PAYMENT_AND_INVENTORY_FAILURE) }
    }

    @Test
    fun `createOrder should call repository service`() {
        val itemId = "item-1"
        val expectedOrder = mockk<Order>()
        every { orderRepositoryService.createOrder(itemId) } returns expectedOrder

        val result = orderService.createOrder(itemId)

        assertEquals(expectedOrder, result)
        verify { orderRepositoryService.createOrder(itemId) }
    }
}
