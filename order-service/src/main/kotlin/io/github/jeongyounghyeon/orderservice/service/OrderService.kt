package io.github.jeongyounghyeon.orderservice.service

import io.github.jeongyounghyeon.orderservice.dto.OrderCreateRequest
import io.github.jeongyounghyeon.orderservice.dto.OrderResponse

interface OrderService {
    fun create(memberId: Long, request: OrderCreateRequest): OrderResponse
    fun findById(orderId: Long, memberId: Long): OrderResponse
    fun findAllByMember(memberId: Long): List<OrderResponse>
    fun cancel(orderId: Long, memberId: Long)
}