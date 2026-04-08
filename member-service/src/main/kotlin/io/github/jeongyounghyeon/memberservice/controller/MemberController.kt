package io.github.jeongyounghyeon.memberservice.controller

import io.github.jeongyounghyeon.common.response.ApiResponse
import io.github.jeongyounghyeon.memberservice.dto.LoginRequest
import io.github.jeongyounghyeon.memberservice.dto.MemberResponse
import io.github.jeongyounghyeon.memberservice.dto.MemberUpdateRequest
import io.github.jeongyounghyeon.memberservice.dto.SignupRequest
import io.github.jeongyounghyeon.memberservice.dto.TokenResponse
import io.github.jeongyounghyeon.memberservice.service.MemberService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members")
class MemberController(
    private val memberService: MemberService
) {
    @PostMapping("/signup")
    fun signup(@RequestBody request: SignupRequest): ResponseEntity<ApiResponse<MemberResponse>> {
        val member = memberService.signup(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(member))
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<ApiResponse<TokenResponse>> {
        val token = memberService.login(request)
        return ResponseEntity.ok(ApiResponse.ok(token))
    }

    @PostMapping("/token/refresh")
    fun refresh(
        @RequestHeader("Refresh-Token") refreshToken: String
    ): ResponseEntity<ApiResponse<TokenResponse>> {
        val token = memberService.refresh(refreshToken)
        return ResponseEntity.ok(ApiResponse.ok(token))
    }

    @GetMapping("/me")
    fun getMe(
        @RequestHeader("Member-Id") memberId: Long
    ): ResponseEntity<ApiResponse<MemberResponse>> {
        val member = memberService.findById(memberId)
        return ResponseEntity.ok(ApiResponse.ok(member))
    }

    @PatchMapping("/me")
    fun updateMe(
        @RequestHeader("Member-Id") memberId: Long,
        @RequestBody request: MemberUpdateRequest
    ): ResponseEntity<ApiResponse<MemberResponse>> {
        val member = memberService.update(memberId, request)
        return ResponseEntity.ok(ApiResponse.ok(member))
    }
}