package com.virtual.bz.demo.controller.dto

import java.util.*

data class OrderDto(
    val id: UUID,
    val paymentId: String? = null,
    val itemId: String,
    val status: OrderStatusDto,
)

enum class OrderStatusDto {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}
