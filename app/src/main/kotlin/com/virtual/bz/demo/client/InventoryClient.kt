package com.virtual.bz.demo.client

import com.virtual.bz.demo.exceptions.InventoryApiException
import com.virtual.bz.demo.service.domain.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException

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
                    .executor(Executors.newVirtualThreadPerTaskExecutor())
                    .build()
            ).apply {
                setReadTimeout(timeout)
            })
        .build()

    data class InventoryResponse(val id: String)

    fun executeReservation(itemId: String): Result {
        return try {
            client.post()
                .uri("/inventory/reserve")
                .body(mapOf("itemId" to itemId))
                .retrieve()
                .toEntity<InventoryResponse>()
                .let {
                    Result.Success(
                        it.body?.id ?: throw InventoryApiException.withMessage("Reservation id is null")
                    )
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "Error while executing inventory reservation" }
            Result.Failure(e.message ?: "Unknown error while executing inventory reservation")
        }
    }

    fun rollbackInventory(reservationId: String): Result {
        return try {
            client.post()
                .uri("/inventory/rollback")
                .body(mapOf("reservationId" to reservationId))
                .retrieve()
                .toEntity<InventoryResponse>()
                .let {
                    Result.Success(
                        it.body?.id ?: throw InventoryApiException.withMessage("Reservation rollback id is null")
                    )
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "Error while rolling back inventory reservation" }
            Result.Failure(e.message ?: "Unknown error while rolling back inventory reservation")
        }
    }
}
