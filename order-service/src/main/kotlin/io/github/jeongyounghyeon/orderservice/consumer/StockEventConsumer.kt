package io.github.jeongyounghyeon.orderservice.consumer

import io.github.jeongyounghyeon.common.event.OrderCancelledEvent
import io.github.jeongyounghyeon.common.event.StockFailedEvent
import io.github.jeongyounghyeon.common.event.StockReservedEvent
import io.github.jeongyounghyeon.common.kafka.KafkaTopics
import io.github.jeongyounghyeon.orderservice.domain.OrderStatus
import io.github.jeongyounghyeon.orderservice.domain.OutboxEvent
import io.github.jeongyounghyeon.orderservice.repository.OrderRepository
import io.github.jeongyounghyeon.orderservice.repository.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class StockEventConsumer(
    private val orderRepository: OrderRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [KafkaTopics.STOCK_RESERVED], groupId = "order-service")
    @Transactional
    fun handleStockReserved(payload: String) {
        val event = objectMapper.readValue(payload, StockReservedEvent::class.java)
        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("주문을 찾을 수 없음: orderId=${event.orderId}")
            return
        }
        if (order.status != OrderStatus.PENDING) return
        order.confirm()
        log.info("주문 확정: orderId=${event.orderId}")
    }

    @KafkaListener(topics = [KafkaTopics.STOCK_FAILED], groupId = "order-service")
    @Transactional
    fun handleStockFailed(payload: String) {
        val event = objectMapper.readValue(payload, StockFailedEvent::class.java)
        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("주문을 찾을 수 없음: orderId=${event.orderId}")
            return
        }
        if (order.status != OrderStatus.PENDING) return

        order.cancel()

        val cancelledEvent = OrderCancelledEvent(
            orderId = event.orderId,
            memberId = order.memberId,
            productId = event.productId,
            quantity = event.quantity,
        )
        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = "ORDER",
                aggregateId = event.orderId,
                eventType = "ORDER_CANCELLED",
                payload = objectMapper.writeValueAsString(cancelledEvent),
            )
        )
        log.info("주문 취소 (재고 부족): orderId=${event.orderId}, reason=${event.reason}")
    }
}