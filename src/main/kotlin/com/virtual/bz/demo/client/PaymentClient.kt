package com.virtual.bz.demo.client

import com.virtual.bz.demo.service.domain.Result
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.*

@Component
class PaymentClient(restClientBuilder: RestClient.Builder) {

    private val client = restClientBuilder.baseUrl("http://payment-api.com").build()

    fun initiatePayment(orderId: UUID): Result {
        // Mocking a blocking call
        Thread.sleep(100)
        return Result.Success("Payment-" + UUID.randomUUID())
    }

    fun rollbackPayment(paymentId: String): Result {
        // Mocking a blocking call
        Thread.sleep(100)
        return Result.Success("PaymentRollback-" + UUID.randomUUID())
    }
}
