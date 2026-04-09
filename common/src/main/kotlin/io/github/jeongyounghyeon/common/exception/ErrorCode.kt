package io.github.jeongyounghyeon.common.exception

enum class ErrorCode(
    val status: Int,
    val message: String
) {
    // Member
    MEMBER_NOT_FOUND(404, "회원을 찾을 수 없습니다"),
    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(401, "비밀번호가 올바르지 않습니다"),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(401, "만료된 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND(401, "리프레시 토큰을 찾을 수 없습니다"),
    // Order
    ORDER_NOT_FOUND(404, "주문을 찾을 수 없습니다"),
    ORDER_FORBIDDEN(403, "접근 권한이 없는 주문입니다"),
    ORDER_ALREADY_CANCELLED(400, "이미 취소된 주문입니다"),
    ORDER_NOT_CANCELLABLE(400, "취소할 수 없는 주문 상태입니다"),
    // Stock
    STOCK_NOT_FOUND(404, "재고를 찾을 수 없습니다"),
    STOCK_ALREADY_EXISTS(409, "이미 등록된 상품 재고입니다"),
    INSUFFICIENT_STOCK(409, "재고가 부족합니다"),
}
