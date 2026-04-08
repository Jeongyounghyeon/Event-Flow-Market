package io.github.jeongyounghyeon.orderservice.controller

import io.github.jeongyounghyeon.common.response.ApiResponse
import io.github.jeongyounghyeon.orderservice.dto.OrderCreateRequest
import io.github.jeongyounghyeon.orderservice.dto.OrderResponse
import io.github.jeongyounghyeon.orderservice.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {
    @PostMapping
    fun create(
        @RequestHeader("Member-Id") memberId: Long,
        @RequestBody request: OrderCreateRequest
    ): ResponseEntity<ApiResponse<OrderResponse>> {
        val order = orderService.create(memberId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(order))
    }

    @GetMapping("/{orderId}")
    fun getOrder(
        @PathVariable orderId: Long,
        @RequestHeader("Member-Id") memberId: Long
    ): ResponseEntity<ApiResponse<OrderResponse>> {
        val order = orderService.findById(orderId, memberId)
        return ResponseEntity.ok(ApiResponse.ok(order))
    }

    @GetMapping
    fun getOrders(
        @RequestHeader("Member-Id") memberId: Long
    ): ResponseEntity<ApiResponse<List<OrderResponse>>> {
        val orders = orderService.findAllByMember(memberId)
        return ResponseEntity.ok(ApiResponse.ok(orders))
    }

    @DeleteMapping("/{orderId}")
    fun cancel(
        @PathVariable orderId: Long,
        @RequestHeader("Member-Id") memberId: Long
    ): ResponseEntity<ApiResponse<Nothing>> {
        orderService.cancel(orderId, memberId)
        return ResponseEntity.ok(ApiResponse.noContent())
    }
}