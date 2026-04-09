package io.github.jeongyounghyeon.stockservice.repository

import io.github.jeongyounghyeon.stockservice.domain.Stock
import org.springframework.data.jpa.repository.JpaRepository

interface StockRepository : JpaRepository<Stock, Long> {
    fun findByProductId(productId: Long): Stock?
    fun existsByProductId(productId: Long): Boolean
}