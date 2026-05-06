package com.virtual.bz.demo.service.domain

import java.util.*

data class Order(
    val id: UUID,
    val paymentId: String? = null,
    val itemId: String,
    val status: OrderStatus
)

enum class OrderStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}