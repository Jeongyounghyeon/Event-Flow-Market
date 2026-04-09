package io.github.jeongyounghyeon.orderservice.repository

import io.github.jeongyounghyeon.orderservice.domain.OutboxEvent
import io.github.jeongyounghyeon.orderservice.domain.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventRepository : JpaRepository<OutboxEvent, Long> {
    fun findAllByStatus(status: OutboxStatus): List<OutboxEvent>
}