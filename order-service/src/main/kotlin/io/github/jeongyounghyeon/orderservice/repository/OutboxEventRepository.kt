package io.github.jeongyounghyeon.orderservice.repository

import io.github.jeongyounghyeon.orderservice.domain.OutboxEvent
import io.github.jeongyounghyeon.orderservice.domain.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventRepository : JpaRepository<OutboxEvent, Long> {
    fun findAllByStatusInAndRetryCountLessThan(
        statuses: List<OutboxStatus>,
        maxRetryCount: Int,
    ): List<OutboxEvent>
}