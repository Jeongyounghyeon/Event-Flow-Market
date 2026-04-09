package io.github.jeongyounghyeon.stockservice

import io.github.jeongyounghyeon.stockservice.service.StockService
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class StockServiceApplicationTests {

    @MockitoBean
    private lateinit var stockService: StockService

    @MockitoBean
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @MockitoBean
    private lateinit var redissonClient: RedissonClient

    @Test
    fun contextLoads() {
    }
}
