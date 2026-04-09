package io.github.jeongyounghyeon.orderservice

import io.github.jeongyounghyeon.orderservice.service.OrderService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class OrderServiceApplicationTests {

    @MockitoBean
    private lateinit var orderService: OrderService

    @MockitoBean
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Test
    fun contextLoads() {
    }
}
