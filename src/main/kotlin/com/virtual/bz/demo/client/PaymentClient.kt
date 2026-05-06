package com.virtual.bz.demo.client

import com.virtual.bz.demo.service.domain.Result
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.*

@Component
class PaymentClient(restClientBuilder: RestClient.Builder) {

    private val client = restClientBuilder.baseUrl("http://payment-api.com").build()

    fun executePayment(orderId: UUID): Result {
        try {
            Thread.sleep(150)
            return Result.Success("Payment-" + UUID.randomUUID())
        } catch (e: Exception) {
            return Result.Failure
        }
    }

    fun rollbackPayment(paymentId: String): Result {
        try {
            Thread.sleep(150)
            return Result.Success("PaymentRollback-" + UUID.randomUUID())
        } catch (e: Exception) {
            return Result.Failure
        }
    }
}
