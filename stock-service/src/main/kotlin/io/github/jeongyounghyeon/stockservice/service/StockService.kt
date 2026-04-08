package io.github.jeongyounghyeon.stockservice.service

import io.github.jeongyounghyeon.stockservice.dto.StockRegisterRequest
import io.github.jeongyounghyeon.stockservice.dto.StockResponse

interface StockService {
    fun findByProductId(productId: Long): StockResponse
    fun register(productId: Long, request: StockRegisterRequest): StockResponse
    fun update(productId: Long, request: StockRegisterRequest): StockResponse
}