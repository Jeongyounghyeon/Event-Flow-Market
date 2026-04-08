package io.github.jeongyounghyeon.orderservice.dto

data class OrderResponse(
    val orderId: Long,
    val memberId: Long,
    val productId: Long,
    val quantity: Int,
    val status: String
)