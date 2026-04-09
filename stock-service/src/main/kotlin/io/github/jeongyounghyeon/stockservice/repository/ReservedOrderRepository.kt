package io.github.jeongyounghyeon.stockservice.repository

import io.github.jeongyounghyeon.stockservice.domain.ReservedOrder
import org.springframework.data.jpa.repository.JpaRepository

interface ReservedOrderRepository : JpaRepository<ReservedOrder, Long>