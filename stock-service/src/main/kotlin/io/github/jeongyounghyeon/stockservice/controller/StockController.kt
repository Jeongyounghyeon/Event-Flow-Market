package io.github.jeongyounghyeon.stockservice.controller

import io.github.jeongyounghyeon.common.response.ApiResponse
import io.github.jeongyounghyeon.stockservice.dto.StockRegisterRequest
import io.github.jeongyounghyeon.stockservice.dto.StockResponse
import io.github.jeongyounghyeon.stockservice.service.StockService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stocks")
class StockController(
    private val stockService: StockService
) {
    @GetMapping("/{productId}")
    fun getStock(
        @PathVariable productId: Long
    ): ResponseEntity<ApiResponse<StockResponse>> {
        val stock = stockService.findByProductId(productId)
        return ResponseEntity.ok(ApiResponse.ok(stock))
    }

    @PostMapping("/{productId}")
    fun register(
        @PathVariable productId: Long,
        @RequestBody request: StockRegisterRequest
    ): ResponseEntity<ApiResponse<StockResponse>> {
        val stock = stockService.register(productId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(stock))
    }

    @PatchMapping("/{productId}")
    fun update(
        @PathVariable productId: Long,
        @RequestBody request: StockRegisterRequest
    ): ResponseEntity<ApiResponse<StockResponse>> {
        val stock = stockService.update(productId, request)
        return ResponseEntity.ok(ApiResponse.ok(stock))
    }
}