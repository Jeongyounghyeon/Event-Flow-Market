package io.github.jeongyounghyeon.common.response

data class ApiResponse<T>(
    val status: Int,
    val data: T? = null,
    val message: String? = null
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(status = 200, data = data)
        fun <T> created(data: T): ApiResponse<T> = ApiResponse(status = 201, data = data)
        fun noContent(): ApiResponse<Nothing> = ApiResponse(status = 204)
        fun error(status: Int, message: String): ApiResponse<Nothing> = ApiResponse(status = status, message = message)
    }
}