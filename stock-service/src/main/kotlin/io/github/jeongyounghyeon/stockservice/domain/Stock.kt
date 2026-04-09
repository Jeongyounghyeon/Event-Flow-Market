package io.github.jeongyounghyeon.stockservice.domain

import io.github.jeongyounghyeon.common.exception.BusinessException
import io.github.jeongyounghyeon.common.exception.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "stocks")
class Stock(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "product_id", unique = true, nullable = false)
    val productId: Long,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun decrease(amount: Int) {
        if (quantity < amount) throw BusinessException(ErrorCode.INSUFFICIENT_STOCK)
        quantity -= amount
        updatedAt = Instant.now()
    }

    fun increase(amount: Int) {
        quantity += amount
        updatedAt = Instant.now()
    }
}