package io.github.jeongyounghyeon.stockservice.producer

import io.github.jeongyounghyeon.common.event.StockFailedEvent
import io.github.jeongyounghyeon.common.event.StockReleasedEvent
import io.github.jeongyounghyeon.common.event.StockReservedEvent
import io.github.jeongyounghyeon.common.kafka.KafkaTopics
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class StockEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    fun publishReserved(orderId: Long, productId: Long, quantity: Int) {
        val event = StockReservedEvent(orderId = orderId, productId = productId, quantity = quantity)
        kafkaTemplate.send(KafkaTopics.STOCK_RESERVED, orderId.toString(), objectMapper.writeValueAsString(event))
    }

    fun publishFailed(orderId: Long, productId: Long, quantity: Int, reason: String) {
        val event = StockFailedEvent(orderId = orderId, productId = productId, quantity = quantity, reason = reason)
        kafkaTemplate.send(KafkaTopics.STOCK_FAILED, orderId.toString(), objectMapper.writeValueAsString(event))
    }

    fun publishReleased(orderId: Long, productId: Long, quantity: Int) {
        val event = StockReleasedEvent(orderId = orderId, productId = productId, quantity = quantity)
        kafkaTemplate.send(KafkaTopics.STOCK_RELEASED, orderId.toString(), objectMapper.writeValueAsString(event))
    }
}