package io.github.jeongyounghyeon.stockservice.service

import io.github.jeongyounghyeon.common.exception.BusinessException
import io.github.jeongyounghyeon.common.exception.ErrorCode
import io.github.jeongyounghyeon.stockservice.domain.Stock
import io.github.jeongyounghyeon.stockservice.dto.StockRegisterRequest
import io.github.jeongyounghyeon.stockservice.dto.StockResponse
import io.github.jeongyounghyeon.stockservice.repository.StockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class StockServiceImpl(
    private val stockRepository: StockRepository,
) : StockService {

    @Transactional(readOnly = true)
    override fun findByProductId(productId: Long): StockResponse {
        val stock = stockRepository.findByProductId(productId)
            ?: throw BusinessException(ErrorCode.STOCK_NOT_FOUND)
        return stock.toResponse()
    }

    override fun register(productId: Long, request: StockRegisterRequest): StockResponse {
        if (stockRepository.existsByProductId(productId)) {
            throw BusinessException(ErrorCode.STOCK_ALREADY_EXISTS)
        }
        val stock = Stock(productId = productId, quantity = request.quantity)
        return stockRepository.save(stock).toResponse()
    }

    override fun update(productId: Long, request: StockRegisterRequest): StockResponse {
        val stock = stockRepository.findByProductId(productId)
            ?: throw BusinessException(ErrorCode.STOCK_NOT_FOUND)
        stock.quantity = request.quantity
        stock.updatedAt = Instant.now()
        return stock.toResponse()
    }

    private fun Stock.toResponse() = StockResponse(
        productId = productId,
        quantity = quantity,
    )
}