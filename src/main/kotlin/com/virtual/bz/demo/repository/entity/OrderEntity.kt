package com.virtual.bz.demo.repository.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "orders")
data class OrderEntity(
    @Id
    val id: UUID,
    val paymentId: String? = null,
    val itemId: String,
    val status: OrderStatusEntity
)

enum class OrderStatusEntity {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}