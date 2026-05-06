package com.virtual.bz.demo.repository.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "orders")
data class OrderEntity(
    @Id
    val id: UUID,
    val paymentId: String? = null,
    val itemId: String,
    @Enumerated(EnumType.STRING)
    val status: OrderStatusEntity,
    @Enumerated(EnumType.STRING)
    val failureReason: FailureReasonEntity? = null,
    @Version
    val version: Long = 0
)

enum class OrderStatusEntity {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}

enum class FailureReasonEntity {
    PAYMENT_FAILURE,
    INVENTORY_FAILURE,
    PAYMENT_AND_INVENTORY_FAILURE,
}