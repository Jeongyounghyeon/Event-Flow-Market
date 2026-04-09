package io.github.jeongyounghyeon.common.kafka

object KafkaTopics {
    const val ORDER_CREATED = "order.created"
    const val ORDER_CANCELLED = "order.cancelled"
    const val STOCK_RESERVED = "stock.reserved"
    const val STOCK_FAILED = "stock.failed"
    const val STOCK_RELEASED = "stock.released"
}