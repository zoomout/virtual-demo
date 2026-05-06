package com.virtual.bz.demo.client

import com.virtual.bz.demo.service.domain.Result
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.*

@Component
class InventoryClient(restClientBuilder: RestClient.Builder) {

    private val client = restClientBuilder.baseUrl("http://inventory-api.com").build()

    fun executeReservation(itemId: String): Result {
        try {
            Thread.sleep(150)
            return Result.Success("Reservation-" + UUID.randomUUID())
        } catch (e: Exception) {
            return Result.Failure
        }
    }

    fun rollbackInventory(reservationId: String): Result {
        try {
            Thread.sleep(150)
            return Result.Success("ReservationRollback-" + UUID.randomUUID())
        } catch (e: Exception) {
            return Result.Failure
        }
    }
}
