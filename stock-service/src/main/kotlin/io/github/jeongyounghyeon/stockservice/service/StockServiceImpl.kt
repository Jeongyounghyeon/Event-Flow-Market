package io.github.jeongyounghyeon.stockservice.service

import io.github.jeongyounghyeon.common.exception.BusinessException
import io.github.jeongyounghyeon.common.exception.ErrorCode
import io.github.jeongyounghyeon.stockservice.domain.Stock
import io.github.jeongyounghyeon.stockservice.dto.StockRegisterRequest
import io.github.jeongyounghyeon.stockservice.dto.StockResponse
import io.github.jeongyounghyeon.stockservice.repository.StockRepository
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
@Transactional
class StockServiceImpl(
    private val stockRepository: StockRepository,
    private val redissonClient: RedissonClient,
    private val transactionTemplate: TransactionTemplate,
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

    // @Transactional 없음: 락 획득 후 TransactionTemplate으로 직접 트랜잭션 관리
    // TransactionTemplate이 commit된 뒤 finally에서 락 해제 → Race Condition 방지
    override fun reserve(productId: Long, quantity: Int): Pair<Boolean, String> {
        val lock = redissonClient.getLock("stock:lock:$productId")
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                return Pair(false, "락 획득 실패")
            }
            return transactionTemplate.execute {
                val stock = stockRepository.findByProductId(productId)
                    ?: return@execute Pair(false, "재고를 찾을 수 없습니다")
                if (stock.quantity < quantity) return@execute Pair(false, "재고 부족")
                stock.decrease(quantity)
                Pair(true, "")
            } ?: Pair(false, "트랜잭션 실패")
        } finally {
            if (lock.isHeldByCurrentThread) lock.unlock()
        }
    }

    override fun restore(productId: Long, quantity: Int) {
        val stock = stockRepository.findByProductId(productId)
            ?: throw BusinessException(ErrorCode.STOCK_NOT_FOUND)
        stock.increase(quantity)
    }

    private fun Stock.toResponse() = StockResponse(
        productId = productId,
        quantity = quantity,
    )
}