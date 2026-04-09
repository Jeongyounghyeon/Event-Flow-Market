package io.github.jeongyounghyeon.common.event

import java.time.Instant
import java.util.UUID

data class OrderCreatedEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val orderId: Long,
    val memberId: Long,
    val productId: Long,
    val quantity: Int,
    val occurredAt: Instant = Instant.now(),
)