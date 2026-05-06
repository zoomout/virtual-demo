package com.virtual.bz.demo.repository.mappers

import com.virtual.bz.demo.repository.entity.OrderEntity
import com.virtual.bz.demo.repository.entity.OrderStatusEntity
import com.virtual.bz.demo.service.domain.Order
import com.virtual.bz.demo.service.domain.OrderStatus

fun OrderEntity.toDomain(): Order {
    return Order(
        id = this.id,
        paymentId = this.paymentId,
        itemId = this.itemId,
        status = status.toDomain()
    )
}

fun OrderStatusEntity.toDomain(): OrderStatus {
    return when (this) {
        OrderStatusEntity.PENDING -> OrderStatus.PENDING
        OrderStatusEntity.PROCESSING -> OrderStatus.PROCESSING
        OrderStatusEntity.COMPLETED -> OrderStatus.COMPLETED
        OrderStatusEntity.FAILED -> OrderStatus.FAILED
    }
}