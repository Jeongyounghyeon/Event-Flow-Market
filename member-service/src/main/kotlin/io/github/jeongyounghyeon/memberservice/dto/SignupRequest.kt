package io.github.jeongyounghyeon.memberservice.dto

data class SignupRequest(
    val email: String,
    val password: String,
    val nickname: String
)