package com.virtual.bz.demo.client

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.*

@Component
class PaymentClient(restClientBuilder: RestClient.Builder) {

    private val client = restClientBuilder.baseUrl("http://payment-api.com").build()

    fun initiatePayment(orderId: UUID): String {
        // Mocking a blocking call
        Thread.sleep(100)
        return "PAY-" + UUID.randomUUID().toString().take(8)
    }
}
