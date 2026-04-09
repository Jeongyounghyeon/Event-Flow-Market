package io.github.jeongyounghyeon.orderservice.repository

import io.github.jeongyounghyeon.orderservice.domain.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByIdAndMemberId(id: Long, memberId: Long): Order?
    fun findAllByMemberId(memberId: Long): List<Order>
}