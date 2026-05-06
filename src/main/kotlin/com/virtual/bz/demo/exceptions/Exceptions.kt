package com.virtual.bz.demo.exceptions

import java.util.*

sealed class OrderException(
    message: String,
    val title: String,
    val type: String,
) : RuntimeException(message)

sealed class NotFoundException(
    message: String,
) : OrderException(
    message = message,
    title = "Item not found",
    type = "not-found-exception",
)

class OrderNotFoundException private constructor(
    message: String,
) : NotFoundException(message) {
    companion object {
        fun withId(id: UUID) =
            OrderNotFoundException(
                message = "Order with id $id not found",
            )
    }
}

class PaymentException(
    message: String,
) : OrderException(
    message = message,
    title = "Payment exception",
    type = "payment-exception",
) {
    companion object {
        fun withMessage(message: String) =
            PaymentException(
                message = message,
            )
    }
}