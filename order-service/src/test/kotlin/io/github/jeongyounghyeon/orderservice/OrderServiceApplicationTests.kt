package io.github.jeongyounghyeon.orderservice

import io.github.jeongyounghyeon.orderservice.service.OrderService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class OrderServiceApplicationTests {

    @MockitoBean
    private lateinit var orderService: OrderService

    @Test
    fun contextLoads() {
    }
}
