package io.github.jeongyounghyeon.memberservice.dto

data class MemberResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val role: String
)