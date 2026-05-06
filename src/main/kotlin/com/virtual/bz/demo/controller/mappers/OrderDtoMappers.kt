package com.virtual.bz.demo.controller.mappers

import com.virtual.bz.demo.controller.dto.OrderDto
import com.virtual.bz.demo.controller.dto.OrderStatusDto
import com.virtual.bz.demo.service.domain.Order
import com.virtual.bz.demo.service.domain.OrderStatus

fun Order.toDto(): OrderDto {
    return OrderDto(
        id = this.id,
        paymentId = this.paymentId,
        itemId = this.itemId,
        status = status.toDto()
    )
}

fun OrderStatus.toDto(): OrderStatusDto =
    when (this) {
        OrderStatus.PENDING -> OrderStatusDto.PENDING
        OrderStatus.PROCESSING -> OrderStatusDto.PROCESSING
        OrderStatus.COMPLETED -> OrderStatusDto.COMPLETED
        OrderStatus.FAILED -> OrderStatusDto.FAILED
    }