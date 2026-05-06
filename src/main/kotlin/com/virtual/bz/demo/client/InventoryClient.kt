package com.virtual.bz.demo.client

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class InventoryClient(restClientBuilder: RestClient.Builder) {

    private val client = restClientBuilder.baseUrl("http://inventory-api.com").build()

    fun reserveInventory(itemId: String): Int {
        // Mocking a blocking call
        Thread.sleep(150)
        return 1
    }
}
