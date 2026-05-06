package com.virtual.bz.demo.client

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
            client.post()
                .uri("/payment/execute")
                .body(mapOf("orderId" to orderId))
                .retrieve()
                .onStatus({ status -> status.isError }) { _, response ->
                    log.error { "Error while executing payment, status code: ${response.statusCode}" }
                    throw RuntimeException("Payment API returned status code: ${response.statusCode}")
                }
                .toEntity<PaymentResponse>()
                .let { response ->
                    val body = response.body
                    if (response.statusCode.is2xxSuccessful && body != null) {
                        Result.Success(body.id)
                    } else {
                        Result.Failure
                    }
                }
        } catch (e: Exception) {
            log.error(e) { "Error while executing payment" }
            Result.Failure
        }
    }

    fun rollbackPayment(paymentId: String): Result {
        return try {
            client.post()
                .uri("/payment/rollback")
                .body(mapOf("paymentId" to paymentId))
                .retrieve()
                .onStatus({ status -> status.isError }) { _, response ->
                    log.error { "Error while rolling back payment, status code: ${response.statusCode}" }
                }
                .toEntity<PaymentResponse>()
                .let { response ->
                    val body = response.body
                    if (response.statusCode.is2xxSuccessful && body != null) {
                        Result.Success(body.id)
                    } else {
                        Result.Failure
                    }
                }
        } catch (e: Exception) {
            log.error(e) { "Error while rolling back payment" }
            Result.Failure
        }
    }
}
