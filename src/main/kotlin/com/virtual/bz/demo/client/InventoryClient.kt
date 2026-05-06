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

private val log = KotlinLogging.logger {}

@Component
class InventoryClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${clients.inventory.url}") baseUrl: String,
    @Value("\${clients.inventory.timeout}") timeout: Duration,
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

    data class InventoryResponse(val id: String)

    fun executeReservation(itemId: String): Result {
        return try {
            log.info { "ThreadInventory: [${Thread.currentThread()}" }
            client.post()
                .uri("/inventory/reserve")
                .body(mapOf("itemId" to itemId))
                .retrieve()
                .onStatus({ status -> status.isError }) { _, response ->
                    log.error { "Error while executing inventory reservation, status code: ${response.statusCode}" }

                }
                .toEntity<InventoryResponse>()
                .let { response ->
                    val body = response.body
                    if (response.statusCode.is2xxSuccessful && body != null) {
                        Result.Success(body.id)
                    } else {
                        Result.Failure
                    }
                }
        } catch (e: Exception) {
            log.error(e) { "Error while executing inventory reservation" }
            Result.Failure
        }
    }

    fun rollbackInventory(reservationId: String): Result {
        return try {
            client.post()
                .uri("/inventory/rollback")
                .body(mapOf("reservationId" to reservationId))
                .retrieve()
                .onStatus({ status -> status.isError }) { _, response ->
                    log.error { "Error while rolling back inventory reservation, status code: ${response.statusCode}" }
                }
                .toEntity<InventoryResponse>()
                .let { response ->
                    val body = response.body
                    if (response.statusCode.is2xxSuccessful && body != null) {
                        Result.Success(body.id)
                    } else {
                        Result.Failure
                    }
                }
        } catch (e: Exception) {
            log.error(e) { "Error while rolling back inventory reservation" }
            Result.Failure
        }
    }
}
