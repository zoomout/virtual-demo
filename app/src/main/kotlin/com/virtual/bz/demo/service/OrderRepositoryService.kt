package com.virtual.bz.demo.service

import com.fasterxml.uuid.Generators
import com.virtual.bz.demo.exceptions.OrderNotFoundException
import com.virtual.bz.demo.repository.OrderJpaRepository
import com.virtual.bz.demo.repository.entity.OrderEntity
import com.virtual.bz.demo.repository.entity.OrderStatusEntity
import com.virtual.bz.demo.repository.mappers.toDomain
import com.virtual.bz.demo.repository.mappers.toEntity
import com.virtual.bz.demo.service.domain.FailureReason
import com.virtual.bz.demo.service.domain.Order
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class OrderRepositoryService(
    private val orderRepository: OrderJpaRepository,
) {

    fun createOrder(itemId: String): Order = orderRepository.save(
        OrderEntity(
            itemId = itemId,
            status = OrderStatusEntity.PENDING,
        )
    ).toDomain()

    @Transactional
    fun markAsProcessing(orderId: UUID): Order =
        orderRepository.markAsProcessing(orderId)
            ?.toDomain()
            ?: run {
                val order = orderRepository.findById(orderId)
                    .orElseThrow { OrderNotFoundException.withId(orderId) }
                throw IllegalStateException("Order $orderId is not in ${OrderStatusEntity.PENDING} state (current: ${order.status})")
            }

    @Transactional
    fun markAsCompleted(orderId: UUID, paymentId: String): Order =
        orderRepository.markAsCompleted(orderId, paymentId)
            ?.toDomain()
            ?: run {
                val order = orderRepository.findById(orderId)
                    .orElseThrow { OrderNotFoundException.withId(orderId) }
                throw IllegalStateException("Order $orderId is not in ${OrderStatusEntity.PROCESSING} state (current: ${order.status})")
            }

    fun getOrder(orderId: UUID): Order =
        orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException.withId(orderId) }
            .toDomain()

    @Transactional
    fun markAsFailed(
        orderId: UUID,
        failureReason: FailureReason,
        paymentId: String? = null,
    ): Order =
        orderRepository.markAsFailed(orderId, failureReason.toEntity().name, paymentId)
            ?.toDomain()
            ?: throw OrderNotFoundException.withId(orderId)

}
