package io.github.jeongyounghyeon.stockservice.consumer

import io.github.jeongyounghyeon.common.event.OrderCancelledEvent
import io.github.jeongyounghyeon.common.event.OrderCreatedEvent
import io.github.jeongyounghyeon.common.kafka.KafkaTopics
import io.github.jeongyounghyeon.stockservice.domain.ProcessedEvent
import io.github.jeongyounghyeon.stockservice.domain.ReservedOrder
import io.github.jeongyounghyeon.stockservice.producer.StockEventProducer
import io.github.jeongyounghyeon.stockservice.repository.ProcessedEventRepository
import io.github.jeongyounghyeon.stockservice.repository.ReservedOrderRepository
import io.github.jeongyounghyeon.stockservice.service.StockService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class OrderEventConsumer(
    private val stockService: StockService,
    private val stockEventProducer: StockEventProducer,
    private val processedEventRepository: ProcessedEventRepository,
    private val reservedOrderRepository: ReservedOrderRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [KafkaTopics.ORDER_CREATED], groupId = "stock-service")
    @Transactional
    fun handleOrderCreated(payload: String) {
        val event = objectMapper.readValue(payload, OrderCreatedEvent::class.java)
        if (processedEventRepository.existsById(event.eventId)) return

        val (success, reason) = stockService.reserve(event.productId, event.quantity)
        if (success) {
            reservedOrderRepository.save(
                ReservedOrder(orderId = event.orderId, productId = event.productId, quantity = event.quantity)
            )
            stockEventProducer.publishReserved(event.orderId, event.productId, event.quantity)
            log.info("재고 예약 성공: orderId=${event.orderId}, productId=${event.productId}")
        } else {
            stockEventProducer.publishFailed(event.orderId, event.productId, event.quantity, reason)
            log.warn("재고 예약 실패: orderId=${event.orderId}, reason=$reason")
        }
        processedEventRepository.save(ProcessedEvent(eventId = event.eventId, eventType = "ORDER_CREATED"))
    }

    @KafkaListener(topics = [KafkaTopics.ORDER_CANCELLED], groupId = "stock-service")
    @Transactional
    fun handleOrderCancelled(payload: String) {
        val event = objectMapper.readValue(payload, OrderCancelledEvent::class.java)
        if (processedEventRepository.existsById(event.eventId)) return

        val reserved = reservedOrderRepository.findById(event.orderId).orElse(null)
        if (reserved != null) {
            stockService.restore(event.productId, reserved.quantity)
            reservedOrderRepository.delete(reserved)
            stockEventProducer.publishReleased(event.orderId, event.productId, reserved.quantity)
            log.info("재고 복구 완료: orderId=${event.orderId}, productId=${event.productId}")
        } else {
            log.info("예약된 재고 없음, 복구 생략: orderId=${event.orderId}")
        }
        processedEventRepository.save(ProcessedEvent(eventId = event.eventId, eventType = "ORDER_CANCELLED"))
    }
}