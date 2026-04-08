package io.github.jeongyounghyeon.memberservice.controller

import io.github.jeongyounghyeon.memberservice.dto.LoginRequest
import io.github.jeongyounghyeon.memberservice.dto.MemberResponse
import io.github.jeongyounghyeon.memberservice.dto.MemberUpdateRequest
import io.github.jeongyounghyeon.memberservice.dto.SignupRequest
import io.github.jeongyounghyeon.memberservice.dto.TokenResponse
import io.github.jeongyounghyeon.memberservice.service.MemberService
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
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ExtendWith(RestDocumentationExtension::class)
class MemberControllerRestDocsTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var memberService: MemberService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(documentationConfiguration(restDocumentation))
            .build()
    }

    private val memberResponse = MemberResponse(
        id = 1L,
        email = "test@example.com",
        nickname = "테스터",
        role = "USER"
    )

    private val tokenResponse = TokenResponse(
        accessToken = "test_access_token.access",
        refreshToken = "test_refresh_token.refresh"
    )

    @Test
    fun `회원가입`() {
        given(memberService.signup(any()))
            .willReturn(memberResponse)

        mockMvc.perform(
            post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SignupRequest("test@example.com", "password123!", "테스터")))
        )
            .andExpect(status().isCreated)
            .andDo(
                document(
                    "member-signup",
                    requestFields(
                        fieldWithPath("email").description("이메일 (유니크)"),
                        fieldWithPath("password").description("비밀번호"),
                        fieldWithPath("nickname").description("닉네임")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data.id").description("회원 ID"),
                        fieldWithPath("data.email").description("이메일"),
                        fieldWithPath("data.nickname").description("닉네임"),
                        fieldWithPath("data.role").description("권한 (USER / ADMIN)"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }

    @Test
    fun `로그인`() {
        given(memberService.login(any()))
            .willReturn(tokenResponse)

        mockMvc.perform(
            post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest("test@example.com", "password123!")))
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "member-login",
                    requestFields(
                        fieldWithPath("email").description("이메일"),
                        fieldWithPath("password").description("비밀번호")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data.accessToken").description("액세스 토큰 (유효기간 30분)"),
                        fieldWithPath("data.refreshToken").description("리프레시 토큰 (유효기간 14일)"),
                        fieldWithPath("data.tokenType").description("토큰 타입 (Bearer)"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }

    @Test
    fun `토큰 재발급`() {
        given(memberService.refresh(any()))
            .willReturn(tokenResponse)

        mockMvc.perform(
            post("/api/members/token/refresh")
                .header("Refresh-Token", "test_refresh_token.refresh")
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "member-token-refresh",
                    requestHeaders(
                        headerWithName("Refresh-Token").description("리프레시 토큰")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data.accessToken").description("새로 발급된 액세스 토큰"),
                        fieldWithPath("data.refreshToken").description("새로 발급된 리프레시 토큰"),
                        fieldWithPath("data.tokenType").description("토큰 타입 (Bearer)"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }

    @Test
    fun `내 프로필 조회`() {
        given(memberService.findById(any()))
            .willReturn(memberResponse)

        mockMvc.perform(
            get("/api/members/me")
                .header("Member-Id", "1")
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "member-get-me",
                    requestHeaders(
                        headerWithName("Member-Id").description("Gateway가 전달하는 인증된 회원 ID")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data.id").description("회원 ID"),
                        fieldWithPath("data.email").description("이메일"),
                        fieldWithPath("data.nickname").description("닉네임"),
                        fieldWithPath("data.role").description("권한"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }

    @Test
    fun `내 프로필 수정`() {
        val updated = memberResponse.copy(nickname = "새닉네임")
        given(memberService.update(any(), any()))
            .willReturn(updated)

        mockMvc.perform(
            patch("/api/members/me")
                .header("Member-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(MemberUpdateRequest("새닉네임")))
        )
            .andExpect(status().isOk)
            .andDo(
                document(
                    "member-update-me",
                    requestHeaders(
                        headerWithName("Member-Id").description("Gateway가 전달하는 인증된 회원 ID")
                    ),
                    requestFields(
                        fieldWithPath("nickname").description("변경할 닉네임")
                    ),
                    responseFields(
                        fieldWithPath("status").description("HTTP 상태 코드"),
                        fieldWithPath("data.id").description("회원 ID"),
                        fieldWithPath("data.email").description("이메일"),
                        fieldWithPath("data.nickname").description("변경된 닉네임"),
                        fieldWithPath("data.role").description("권한"),
                        fieldWithPath("message").optional().description("에러 메시지")
                    )
                )
            )
    }
}
