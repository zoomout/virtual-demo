package com.virtual.bz.demo.controller

import com.virtual.bz.demo.controller.dto.CreateOrderDto
import com.virtual.bz.demo.controller.dto.OrderDto
import com.virtual.bz.demo.controller.mappers.toDto
import com.virtual.bz.demo.service.OrderService
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    @PostMapping()
    fun createOrder(@RequestBody body: CreateOrderDto): OrderDto =
        orderService.createOrder(body.itemId).toDto()

    @PostMapping("/{id}/process")
    fun processOrder(@PathVariable id: UUID): OrderDto =
        orderService.processOrder(id).toDto()
}
