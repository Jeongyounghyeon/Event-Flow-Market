package io.github.jeongyounghyeon.stockservice.repository

import io.github.jeongyounghyeon.stockservice.domain.ProcessedEvent
import org.springframework.data.jpa.repository.JpaRepository

interface ProcessedEventRepository : JpaRepository<ProcessedEvent, String>