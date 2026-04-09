package io.github.jeongyounghyeon.orderservice.exception

import io.github.jeongyounghyeon.common.exception.BusinessException
import io.github.jeongyounghyeon.common.response.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(e.errorCode.status)
            .body(ApiResponse.error(e.errorCode.status, e.errorCode.message))
    }
}