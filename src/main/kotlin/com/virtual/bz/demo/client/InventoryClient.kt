package com.virtual.bz.demo.client

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.*

@Component
class InventoryClient(restClientBuilder: RestClient.Builder) {

    private val client = restClientBuilder.baseUrl("http://inventory-api.com").build()

    fun checkInventory(itemId: String): String {
        // Mocking a blocking call
        Thread.sleep(150)
        return "INV-" + UUID.randomUUID().toString().take(8)
    }
}
