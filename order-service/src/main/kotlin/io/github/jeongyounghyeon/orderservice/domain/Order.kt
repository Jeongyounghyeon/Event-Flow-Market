package io.github.jeongyounghyeon.orderservice.domain

import io.github.jeongyounghyeon.common.exception.BusinessException
import io.github.jeongyounghyeon.common.exception.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "orders")
class Order(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val quantity: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun cancel() {
        if (status == OrderStatus.CANCELLED) throw BusinessException(ErrorCode.ORDER_ALREADY_CANCELLED)
        if (status != OrderStatus.PENDING) throw BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE)
        status = OrderStatus.CANCELLED
        updatedAt = Instant.now()
    }

    fun confirm() {
        status = OrderStatus.CONFIRMED
        updatedAt = Instant.now()
    }
}