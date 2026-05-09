package com.virtual.bz.demo.controller

import com.virtual.bz.demo.controller.dto.CreateOrderDto
import com.virtual.bz.demo.controller.dto.OrderDto
import com.virtual.bz.demo.controller.mappers.toDto
import com.virtual.bz.demo.service.OrderService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(@RequestBody body: CreateOrderDto): OrderDto =
        orderService.createOrder(body.itemId).toDto()

    @PostMapping("/{id}/process")
    fun processOrder(@PathVariable id: UUID): OrderDto {
        return orderService.processOrder(id).toDto()
    }

    @GetMapping("/{id}")
    fun getOrder(@PathVariable id: UUID): OrderDto =
        orderService.getOrder(id).toDto()
}
