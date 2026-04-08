package io.github.jeongyounghyeon.memberservice

import io.github.jeongyounghyeon.memberservice.service.MemberService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class MemberServiceApplicationTests {

    @MockitoBean
    private lateinit var memberService: MemberService

    @Test
    fun contextLoads() {
    }
}
