package com.virtual.bz.demo.repository.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener::class)
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
    val version: Long = 0,
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant? = null,
) {
}

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
