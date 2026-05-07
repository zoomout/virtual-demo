package com.virtual.bz.demo.repository.mappers

import com.virtual.bz.demo.repository.entity.FailureReasonEntity
import com.virtual.bz.demo.repository.entity.OrderEntity
import com.virtual.bz.demo.repository.entity.OrderStatusEntity
import com.virtual.bz.demo.service.domain.FailureReason
import com.virtual.bz.demo.service.domain.Order
import com.virtual.bz.demo.service.domain.OrderStatus

fun OrderEntity.toDomain(): Order {
    return Order(
        id = this.id,
        paymentId = this.paymentId,
        itemId = this.itemId,
        status = status.toDomain(),
        failureReason = failureReason?.toDomain(),
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

fun FailureReason.toEntity(): FailureReasonEntity =
    when (this) {
        FailureReason.PAYMENT_FAILURE -> FailureReasonEntity.PAYMENT_FAILURE
        FailureReason.INVENTORY_FAILURE -> FailureReasonEntity.PAYMENT_FAILURE
        FailureReason.PAYMENT_AND_INVENTORY_FAILURE -> FailureReasonEntity.PAYMENT_AND_INVENTORY_FAILURE
    }

fun FailureReasonEntity.toDomain(): FailureReason =
    when (this) {
        FailureReasonEntity.PAYMENT_FAILURE -> FailureReason.PAYMENT_FAILURE
        FailureReasonEntity.INVENTORY_FAILURE -> FailureReason.INVENTORY_FAILURE
        FailureReasonEntity.PAYMENT_AND_INVENTORY_FAILURE -> FailureReason.PAYMENT_AND_INVENTORY_FAILURE
    }