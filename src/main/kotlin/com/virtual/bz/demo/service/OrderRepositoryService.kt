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
            id = Generators.timeBasedEpochGenerator().generate(),
            itemId = itemId,
            status = OrderStatusEntity.PENDING,
        )
    ).toDomain()

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
                if (it.status != OrderStatusEntity.PROCESSING) {
                    throw IllegalStateException("Order $orderId is not in ${OrderStatusEntity.PROCESSING} state (current: ${it.status})")
                }
            }

        return orderRepository.save(
            order.copy(
                paymentId = paymentId,
                status = OrderStatusEntity.COMPLETED
            )
        ).toDomain()
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
