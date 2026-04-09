package io.github.jeongyounghyeon.common.exception

class BusinessException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.message)
