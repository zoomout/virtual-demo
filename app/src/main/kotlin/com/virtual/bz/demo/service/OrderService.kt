package com.virtual.bz.demo.service

import com.virtual.bz.demo.client.InventoryClient
import com.virtual.bz.demo.client.PaymentClient
import com.virtual.bz.demo.exceptions.OrderProcessingException
import com.virtual.bz.demo.service.domain.FailureReason
import com.virtual.bz.demo.service.domain.Order
import com.virtual.bz.demo.service.domain.Result
import com.virtual.bz.demo.service.domain.Result.Failure
import com.virtual.bz.demo.service.domain.Result.Success
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.StructuredTaskScope

private val log = KotlinLogging.logger {}

@Service
class OrderService(
    private val orderRepositoryService: OrderRepositoryService,
    private val paymentClient: PaymentClient,
    private val inventoryClient: InventoryClient,
) {
    fun createOrder(itemId: String): Order =
        orderRepositoryService.createOrder(itemId)

    fun getOrder(id: UUID): Order =
        orderRepositoryService.getOrder(id)

    fun processOrder(orderId: UUID): Order {
        val order = orderRepositoryService.markAsProcessing(orderId)
        log.info { "ThreadOrder: [${Thread.currentThread()}" }
        val paymentId = StructuredTaskScope.open<Result>().use { scope ->
            log.info { "ThreadOrderCoroutine: [${Thread.currentThread()}" }
            val paymentDeferred = scope.fork(Callable { paymentClient.executePayment(orderId) })
            val inventoryDeferred = scope.fork(Callable { inventoryClient.executeReservation(order.itemId) })
            scope.join()
            val paymentResult = paymentDeferred.get()
            val inventoryResult = inventoryDeferred.get()
            when {
                // 1. Success
                paymentResult is Success && inventoryResult is Success -> {
                    paymentResult.id
                }

                // 2. Inventory Failure (Requires Payment Rollback)
                inventoryResult is Failure && paymentResult is Success -> {
                    orderRepositoryService.markAsFailed(orderId, FailureReason.INVENTORY_FAILURE)
                    paymentClient.rollbackPayment(paymentResult.id)
                    throw OrderProcessingException.withMessage("Inventory failed for orderId=$orderId")
                }

                // 3. Payment Failure (Requires Inventory Rollback)
                paymentResult is Failure && inventoryResult is Success -> {
                    orderRepositoryService.markAsFailed(orderId, FailureReason.PAYMENT_FAILURE)
                    inventoryClient.rollbackInventory(inventoryResult.id)
                    throw OrderProcessingException.withMessage("Payment failed for orderId=$orderId")
                }

                // 3. Payment & Inventory Failures
                else -> {
                    orderRepositoryService.markAsFailed(orderId, FailureReason.PAYMENT_AND_INVENTORY_FAILURE)
                    throw OrderProcessingException.withMessage("Payment and Inventory failed for orderId=$orderId")
                }
            }
        }
        return orderRepositoryService.markAsComplete(orderId, paymentId)
    }

}
