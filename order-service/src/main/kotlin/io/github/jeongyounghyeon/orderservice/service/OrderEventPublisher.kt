package io.github.jeongyounghyeon.orderservice.service

import io.github.jeongyounghyeon.common.kafka.KafkaTopics
import io.github.jeongyounghyeon.orderservice.domain.OutboxStatus
import io.github.jeongyounghyeon.orderservice.repository.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OrderEventPublisher(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun publish() {
        val pending = outboxEventRepository.findAllByStatus(OutboxStatus.PENDING)
        if (pending.isEmpty()) return

        pending.forEach { event ->
            val topic = when (event.eventType) {
                "ORDER_CREATED" -> KafkaTopics.ORDER_CREATED
                "ORDER_CANCELLED" -> KafkaTopics.ORDER_CANCELLED
                else -> {
                    log.warn("알 수 없는 이벤트 타입: ${event.eventType}")
                    return@forEach
                }
            }
            try {
                kafkaTemplate.send(topic, event.aggregateId.toString(), event.payload).get()
                event.status = OutboxStatus.PUBLISHED
                event.publishedAt = Instant.now()
                log.debug("Outbox 이벤트 발행 완료: id=${event.id}, topic=$topic")
            } catch (e: Exception) {
                log.error("Outbox 이벤트 발행 실패: id=${event.id}", e)
                event.status = OutboxStatus.FAILED
            }
        }
    }
}