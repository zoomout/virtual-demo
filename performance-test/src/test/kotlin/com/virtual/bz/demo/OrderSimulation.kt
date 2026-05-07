package com.virtual.bz.demo

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status

class OrderSimulation : Simulation() {

    private val httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")

    private val scn = scenario("Order Processing Scenario")
        .exec(
            http("Create Order")
                .post("/api/orders")
                .body(StringBody("""{ "itemId": "item-123" }""")).asJson()
                .check(status().`is`(201))
                .check(jsonPath("$.id").saveAs("orderId"))
        )
        .pause(1)
        .exec(
            http("Process Order")
                .post("/api/orders/#{orderId}/process")
                .check(status().`is`(200))
        )
        .pause(1)
        .exec(
            http("Get Order")
                .get("/api/orders/#{orderId}")
                .check(status().`is`(200))
                .check(jsonPath("$.status").`is`("COMPLETED"))
        )

    init {
        setUp(
            scn.injectOpen(
                nothingFor(2),                        // Warm up
                atOnceUsers(10),                             // Initial spike
                rampUsers(1000).during(60),            // Ramp up to 1,000 users over 1 min
                constantUsersPerSec(100.0).during(60)  // Sustained load: 100 req/sec for 1 min
            )
        ).protocols(httpProtocol)
    }
}
