package io.github.jeongyounghyeon.orderservice.domain

enum class OutboxStatus { PENDING, PUBLISHED, FAILED, DEAD_LETTER }