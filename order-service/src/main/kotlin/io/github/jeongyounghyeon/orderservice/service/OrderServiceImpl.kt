package io.github.jeongyounghyeon.orderservice.service

import io.github.jeongyounghyeon.common.event.OrderCancelledEvent
import io.github.jeongyounghyeon.common.event.OrderCreatedEvent
import io.github.jeongyounghyeon.common.exception.BusinessException
import io.github.jeongyounghyeon.common.exception.ErrorCode
import io.github.jeongyounghyeon.orderservice.domain.Order
import io.github.jeongyounghyeon.orderservice.domain.OutboxEvent
import io.github.jeongyounghyeon.orderservice.dto.OrderCreateRequest
import io.github.jeongyounghyeon.orderservice.dto.OrderResponse
import io.github.jeongyounghyeon.orderservice.repository.OrderRepository
import io.github.jeongyounghyeon.orderservice.repository.OutboxEventRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
@Transactional
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) : OrderService {

    override fun create(memberId: Long, request: OrderCreateRequest): OrderResponse {
        val order = Order(
            memberId = memberId,
            productId = request.productId,
            quantity = request.quantity,
        )
        val saved = orderRepository.save(order)

        val event = OrderCreatedEvent(
            orderId = saved.id,
            memberId = memberId,
            productId = request.productId,
            quantity = request.quantity,
        )
        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = "ORDER",
                aggregateId = saved.id,
                eventType = "ORDER_CREATED",
                payload = objectMapper.writeValueAsString(event),
            )
        )

        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    override fun findById(orderId: Long, memberId: Long): OrderResponse {
        val order = orderRepository.findByIdAndMemberId(orderId, memberId)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)
        return order.toResponse()
    }

    @Transactional(readOnly = true)
    override fun findAllByMember(memberId: Long): List<OrderResponse> {
        return orderRepository.findAllByMemberId(memberId).map { it.toResponse() }
    }

    override fun cancel(orderId: Long, memberId: Long) {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)
        if (order.memberId != memberId) throw BusinessException(ErrorCode.ORDER_FORBIDDEN)
        order.cancel()

        val event = OrderCancelledEvent(
            orderId = orderId,
            memberId = memberId,
            productId = order.productId,
            quantity = order.quantity,
        )
        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = "ORDER",
                aggregateId = orderId,
                eventType = "ORDER_CANCELLED",
                payload = objectMapper.writeValueAsString(event),
            )
        )
    }

    private fun Order.toResponse() = OrderResponse(
        orderId = id,
        memberId = memberId,
        productId = productId,
        quantity = quantity,
        status = status.name,
    )
}