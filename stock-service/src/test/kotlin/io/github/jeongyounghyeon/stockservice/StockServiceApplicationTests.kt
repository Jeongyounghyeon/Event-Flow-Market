package io.github.jeongyounghyeon.stockservice

import io.github.jeongyounghyeon.stockservice.service.StockService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class StockServiceApplicationTests {

    @MockitoBean
    private lateinit var stockService: StockService

    @Test
    fun contextLoads() {
    }
}
