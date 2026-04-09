package io.github.jeongyounghyeon.stockservice.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "reserved_orders")
class ReservedOrder(
    @Id
    @Column(name = "order_id")
    val orderId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val quantity: Int,
)