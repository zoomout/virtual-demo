package com.virtual.bz.demo.client

import com.virtual.bz.demo.service.domain.Result
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.*

@Component
class InventoryClient(restClientBuilder: RestClient.Builder) {

    private val client = restClientBuilder.baseUrl("http://inventory-api.com").build()

    fun reserveInventory(itemId: String): Result {
        // Mocking a blocking call
        Thread.sleep(150)
        return Result.Success("Reservation-" + UUID.randomUUID())
    }

    fun rollbackInventory(reservationId: String): Result {
        // Mocking a blocking call
        Thread.sleep(150)
        return Result.Success("ReservationRollback-" + UUID.randomUUID())
    }
}
