package io.github.jeongyounghyeon.orderservice.dto

data class OrderCreateRequest(
    val productId: Long,
    val quantity: Int
)