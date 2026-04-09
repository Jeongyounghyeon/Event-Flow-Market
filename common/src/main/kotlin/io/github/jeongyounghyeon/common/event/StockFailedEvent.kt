package io.github.jeongyounghyeon.common.event

import java.time.Instant
import java.util.UUID

data class StockFailedEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val orderId: Long,
    val productId: Long,
    val quantity: Int,
    val reason: String,
    val occurredAt: Instant = Instant.now(),
)