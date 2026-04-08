package io.github.jeongyounghyeon.memberservice.service

import io.github.jeongyounghyeon.memberservice.dto.LoginRequest
import io.github.jeongyounghyeon.memberservice.dto.MemberResponse
import io.github.jeongyounghyeon.memberservice.dto.MemberUpdateRequest
import io.github.jeongyounghyeon.memberservice.dto.SignupRequest
import io.github.jeongyounghyeon.memberservice.dto.TokenResponse

interface MemberService {
    fun signup(request: SignupRequest): MemberResponse
    fun login(request: LoginRequest): TokenResponse
    fun refresh(refreshToken: String): TokenResponse
    fun findById(id: Long): MemberResponse
    fun update(id: Long, request: MemberUpdateRequest): MemberResponse
}