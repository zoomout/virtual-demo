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
import com.virtual.bz.demo.service.domain.Order
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
                status = OrderStatusEntity.PENDING
            )
        ).toDomain()

    @Transactional
    fun processOrder(orderId: UUID): Order {
        val orderEntity = orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException.withId(orderId) }

        val processingOrder = orderEntity.copy(status = OrderStatusEntity.PROCESSING)
        orderRepository.saveAndFlush(processingOrder)

        return runBlocking(dispatcher) {
            val paymentDeferred = async { paymentClient.initiatePayment(processingOrder.id) }
            val inventoryDeferred = async { inventoryClient.reserveInventory(processingOrder.itemId) }

            val (paymentResult, paymentId) = paymentDeferred.await()
            if (!paymentResult) {
                throw PaymentException.withMessage("Payment failed for orderId=$orderId and paymentId=$paymentId")
            }
            val inventoryResult = inventoryDeferred.await()

            if (inventoryResult < 1) {
                val rolledBackPayment = async { paymentClient.rollbackPayment(orderId) }
                val (rollbackResult, paymentId) = rolledBackPayment.await()
                if (!rollbackResult) {
                    throw PaymentException.withMessage("Payment rollback failed for orderId=$orderId and paymentId=$paymentId")
                }
                return@runBlocking orderRepository.saveAndFlush(
                    orderEntity.copy(
                        paymentId = paymentId,
                        status = OrderStatusEntity.FAILED,
                    )
                ).toDomain()
            }

            orderRepository.saveAndFlush(
                orderEntity.copy(
                    paymentId = paymentId,
                    status = OrderStatusEntity.COMPLETED,
                )
            ).toDomain()
        }
    }

}
