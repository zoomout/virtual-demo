package com.virtual.bz.demo.client

import com.virtual.bz.demo.exceptions.InventoryApiException
import com.virtual.bz.demo.exceptions.PaymentApiException
import com.virtual.bz.demo.service.domain.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.net.http.HttpClient
import java.time.Duration
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class PaymentClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${clients.payment.url}") baseUrl: String,
    @Value("\${clients.payment.timeout}") timeout: Duration,
) {

    private val client = restClientBuilder
        .baseUrl(baseUrl)
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(timeout)
                    .build()
            ).apply {
                setReadTimeout(timeout)
            })
        .build()

    data class PaymentResponse(val id: String)

    fun executePayment(orderId: UUID): Result {
        return try {
            log.info { "ThreadPayment: [${Thread.currentThread()}" }
            client.post()
                .uri("/payment/execute")
                .body(mapOf("orderId" to orderId))
                .retrieve()
                .toEntity<PaymentResponse>()
                .let {
                    Result.Success(
                        it.body?.id ?: throw PaymentApiException.withMessage("Payment id is null")
                    )
                }
        } catch (e: Exception) {
            log.error(e) { "Error while executing payment" }
            Result.Failure(e.message ?: "Unknown error while executing  payment")
        }
    }

    fun rollbackPayment(paymentId: String): Result {
        return try {
            client.post()
                .uri("/payment/rollback")
                .body(mapOf("paymentId" to paymentId))
                .retrieve()
                .toEntity<PaymentResponse>()
                .let {
                    Result.Success(
                        it.body?.id ?: throw InventoryApiException.withMessage("Payment rollback id is null")
                    )
                }
        } catch (e: Exception) {
            log.error(e) { "Error while rolling back payment" }
            Result.Failure(e.message ?: "Unknown error while rolling back payment")
        }
    }
}
