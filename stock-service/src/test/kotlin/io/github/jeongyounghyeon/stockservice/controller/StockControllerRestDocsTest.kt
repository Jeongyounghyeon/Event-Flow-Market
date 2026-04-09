package io.github.jeongyounghyeon.stockservice.controller

import io.github.jeongyounghyeon.stockservice.dto.StockRegisterRequest
import io.github.jeongyounghyeon.stockservice.dto.StockResponse
import io.github.jeongyounghyeon.stockservice.service.StockService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.redisson.api.RedissonClient
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ExtendWith(RestDocumentationExtension::class)
class StockControllerRestDocsTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var stockService: StockService

    @MockitoBean
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @MockitoBean
    private lateinit var redissonClient: RedissonClient

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(documentationConfiguration(restDocumentation))
            .build()
    }

    private val stockResponse = StockResponse(productId = 100L, quantity = 50)

    @Test
    fun `재고 조회`() {
        given(stockService.findByProductId(any()))
            .willReturn(stockResponse)

        mockMvc.perform(
            get("/api/stocks/{productId}", 100L)
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "stock-get",
                    pathParameters(
                        parameterWithName("productId").description("상품 ID")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data.productId").description("상품 ID"),
                        fieldWithPath("data.quantity").description("현재 재고 수량"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }

    @Test
    fun `재고 등록`() {
        given(stockService.register(any(), any()))
            .willReturn(stockResponse)

        mockMvc.perform(
            post("/api/stocks/{productId}", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(StockRegisterRequest(quantity = 50)))
        )
            .andExpect(status().isCreated)
            .andDo(
                document(
                    "stock-register",
                    pathParameters(
                        parameterWithName("productId").description("상품 ID")
                    ),
                    requestFields(
                        fieldWithPath("quantity").description("등록할 재고 수량 (0 이상)")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data.productId").description("상품 ID"),
                        fieldWithPath("data.quantity").description("등록된 재고 수량"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }

    @Test
    fun `재고 수정`() {
        val updated = stockResponse.copy(quantity = 30)
        given(stockService.update(any(), any()))
            .willReturn(updated)

        mockMvc.perform(
            patch("/api/stocks/{productId}", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(StockRegisterRequest(quantity = 30)))
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "stock-update",
                    pathParameters(
                        parameterWithName("productId").description("상품 ID")
                    ),
                    requestFields(
                        fieldWithPath("quantity").description("변경할 재고 수량 (0 이상)")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data.productId").description("상품 ID"),
                        fieldWithPath("data.quantity").description("변경된 재고 수량"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }
}
