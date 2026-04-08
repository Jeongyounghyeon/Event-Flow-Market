package io.github.jeongyounghyeon.memberservice.dto

data class LoginRequest(
    val email: String,
    val password: String
)