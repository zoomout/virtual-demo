package com.virtual.bz.demo.service.domain

import java.util.*

data class Order(
    val id: UUID,
    val paymentId: String? = null,
    val itemId: String,
    val status: OrderStatus,
    val failureReason: FailureReason? = null,
)

enum class OrderStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}

enum class FailureReason {
    PAYMENT_FAILURE,
    INVENTORY_FAILURE,
    PAYMENT_AND_INVENTORY_FAILURE,
}