package com.virtual.bz.demo.service

import com.fasterxml.uuid.Generators
import com.virtual.bz.demo.client.InventoryClient
import com.virtual.bz.demo.client.PaymentClient
import com.virtual.bz.demo.exceptions.OrderNotFoundException
import com.virtual.bz.demo.exceptions.PaymentException
import com.virtual.bz.demo.repository.OrderJpaRepository
import com.virtual.bz.demo.repository.entity.OrderEntity
import com.virtual.bz.demo.repository.entity.OrderStatusEntity
import com.virtual.bz.demo.repository.mappers.toDomain
import com.virtual.bz.demo.repository.mappers.toEntity
import com.virtual.bz.demo.service.domain.FailureReason
import com.virtual.bz.demo.service.domain.Order
import com.virtual.bz.demo.service.domain.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class OrderService(
    private val orderRepository: OrderJpaRepository,
    private val paymentClient: PaymentClient,
    private val inventoryClient: InventoryClient,
    private val dispatcher: CoroutineDispatcher
) {

    fun createOrder(itemId: String): Order =
        orderRepository.save(
            OrderEntity(
                id = Generators.timeBasedEpochGenerator().generate(),
                itemId = itemId,
                status = OrderStatusEntity.PENDING,
            )
        ).toDomain()

    fun processOrder(orderId: UUID): Order {
        val order = markAsProcessing(orderId)
        val paymentId = runBlocking(dispatcher) {
            val paymentDeferred = async { paymentClient.initiatePayment(orderId) }
            val inventoryDeferred = async { inventoryClient.reserveInventory(order.itemId) }

            val paymentResult = paymentDeferred.await()
            val inventoryResult = inventoryDeferred.await()

            when (paymentResult) {
                is Result.Success if inventoryResult is Result.Success -> {
                    return@runBlocking paymentResult.id
                }

                is Result.Success if inventoryResult is Result.Failure -> {
                    markAsFailed(orderId, FailureReason.INVENTORY_FAILURE)
                    paymentClient.rollbackPayment(paymentResult.id)
                }

                is Result.Failure if inventoryResult is Result.Success -> {
                    markAsFailed(orderId, FailureReason.PAYMENT_FAILURE)
                    inventoryClient.rollbackInventory(inventoryResult.id)
                }

                else -> {
                    markAsFailed(orderId, FailureReason.PAYMENT_AND_INVENTORY_FAILURE)
                }
            }
            throw PaymentException.withMessage("Payment failed for orderId=$orderId")

        }
        return markAsComplete(orderId, paymentId)
    }

    @Transactional
    fun markAsProcessing(orderId: UUID): Order =
        orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException.withId(orderId) }
            .also {
                if (it.status != OrderStatusEntity.PENDING) {
                    throw IllegalStateException("Order $orderId is not in ${OrderStatusEntity.PENDING} state (current: ${it.status})")
                }
            }
            .let { orderRepository.save(it.copy(status = OrderStatusEntity.PROCESSING)) }
            .toDomain()

    @Transactional
    fun markAsComplete(orderId: UUID, paymentId: String): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException.withId(orderId) }
            .also {
                if (it.status != OrderStatusEntity.PENDING) {
                    throw IllegalStateException("Order $orderId is not in ${OrderStatusEntity.PENDING} state (current: ${it.status})")
                }
            }

        return orderRepository.save(
            order.copy(
                paymentId = paymentId,
                status = OrderStatusEntity.COMPLETED
            )
        ).toDomain()
    }

    @Transactional
    fun markAsFailed(
        orderId: UUID,
        failureReason: FailureReason,
        paymentId: String? = null,
    ): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException.withId(orderId) }

        return orderRepository.save(
            order.copy(
                paymentId = paymentId,
                status = OrderStatusEntity.FAILED,
                failureReason = failureReason.toEntity()
            )
        ).toDomain()
    }

}
