package io.github.jeongyounghyeon.orderservice.controller

import io.github.jeongyounghyeon.orderservice.dto.OrderCreateRequest
import io.github.jeongyounghyeon.orderservice.dto.OrderResponse
import io.github.jeongyounghyeon.orderservice.service.OrderService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.given
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ExtendWith(RestDocumentationExtension::class)
class OrderControllerRestDocsTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var orderService: OrderService

    @MockitoBean
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(documentationConfiguration(restDocumentation))
            .build()
    }

    private val orderResponse = OrderResponse(
        orderId = 1L,
        memberId = 1L,
        productId = 100L,
        quantity = 2,
        status = "PENDING"
    )

    @Test
    fun `주문 생성`() {
        given(orderService.create(any(), any()))
            .willReturn(orderResponse)

        mockMvc.perform(
            post("/api/orders")
                .header("Member-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(OrderCreateRequest(productId = 100L, quantity = 2)))
        )
            .andExpect(status().isCreated)
            .andDo(
                document(
                    "order-create",
                    requestHeaders(
                        headerWithName("Member-Id").description("Gateway가 전달하는 인증된 회원 ID")
                    ),
                    requestFields(
                        fieldWithPath("productId").description("상품 ID"),
                        fieldWithPath("quantity").description("주문 수량 (1 이상)")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data.orderId").description("주문 ID"),
                        fieldWithPath("data.memberId").description("회원 ID"),
                        fieldWithPath("data.productId").description("상품 ID"),
                        fieldWithPath("data.quantity").description("주문 수량"),
                        fieldWithPath("data.status").description("주문 상태 (PENDING / CONFIRMED / CANCELLED)"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }

    @Test
    fun `주문 단건 조회`() {
        given(orderService.findById(any(), any()))
            .willReturn(orderResponse)

        mockMvc.perform(
            get("/api/orders/{orderId}", 1L)
                .header("Member-Id", "1")
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "order-get",
                    requestHeaders(
                        headerWithName("Member-Id").description("Gateway가 전달하는 인증된 회원 ID")
                    ),
                    pathParameters(
                        parameterWithName("orderId").description("조회할 주문 ID")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data.orderId").description("주문 ID"),
                        fieldWithPath("data.memberId").description("회원 ID"),
                        fieldWithPath("data.productId").description("상품 ID"),
                        fieldWithPath("data.quantity").description("주문 수량"),
                        fieldWithPath("data.status").description("주문 상태"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }

    @Test
    fun `내 주문 목록 조회`() {
        given(orderService.findAllByMember(any()))
            .willReturn(listOf(orderResponse))

        mockMvc.perform(
            get("/api/orders")
                .header("Member-Id", "1")
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "order-list",
                    requestHeaders(
                        headerWithName("Member-Id").description("Gateway가 전달하는 인증된 회원 ID")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data[].orderId").description("주문 ID"),
                        fieldWithPath("data[].memberId").description("회원 ID"),
                        fieldWithPath("data[].productId").description("상품 ID"),
                        fieldWithPath("data[].quantity").description("주문 수량"),
                        fieldWithPath("data[].status").description("주문 상태"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }

    @Test
    fun `주문 취소`() {
        doNothing().whenever(orderService).cancel(any(), any())

        mockMvc.perform(
            delete("/api/orders/{orderId}", 1L)
                .header("Member-Id", "1")
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "order-cancel",
                    requestHeaders(
                        headerWithName("Member-Id").description("Gateway가 전달하는 인증된 회원 ID")
                    ),
                    pathParameters(
                        parameterWithName("orderId").description("취소할 주문 ID")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data").optional().description("응답 데이터 (없음)"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }
}
