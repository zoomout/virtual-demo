package com.virtual.bz.demo.service

import com.fasterxml.uuid.Generators
import com.virtual.bz.demo.client.InventoryClient
import com.virtual.bz.demo.client.PaymentClient
import com.virtual.bz.demo.exceptions.OrderNotFoundException
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
        orderRepository.save(processingOrder)

        return runBlocking(dispatcher) {
            val paymentDeferred = async { paymentClient.initiatePayment(processingOrder.id) }
            val inventoryDeferred = async { inventoryClient.checkInventory(processingOrder.itemId) }

            val paymentId = paymentDeferred.await()
            val inventoryResult = inventoryDeferred.await()

            val completedOrder = orderEntity.copy(
                paymentId = paymentId,
                status = OrderStatusEntity.COMPLETED,
            )
            orderRepository.save(completedOrder).toDomain()
        }
    }

}
