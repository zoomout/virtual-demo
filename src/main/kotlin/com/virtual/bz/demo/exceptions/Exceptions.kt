package com.virtual.bz.demo.exceptions

import java.util.*

sealed class OrderException(
    message: String,
    val type: String,
) : RuntimeException(message)

sealed class NotFoundException(
    message: String,
) : OrderException(
    message = message,
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

class OrderProcessingException(
    message: String,
) : OrderException(
    message = message,
    type = "order-processing-exception",
) {
    companion object {
        fun withMessage(message: String) =
            OrderProcessingException(
                message = message,
            )
    }
}