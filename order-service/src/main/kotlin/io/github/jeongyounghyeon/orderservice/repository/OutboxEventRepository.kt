package io.github.jeongyounghyeon.orderservice.repository

import io.github.jeongyounghyeon.orderservice.domain.OutboxEvent
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventRepository : JpaRepository<OutboxEvent, Long>