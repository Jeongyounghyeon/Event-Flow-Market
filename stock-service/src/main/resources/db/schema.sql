CREATE TABLE IF NOT EXISTS stocks
(
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,                          -- 상품 ID (Product Service와 공유, FK 없음)
    quantity   INT    NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stocks_product_id ON stocks (product_id);

-- ──────────────────────────────────────────────────────────
-- Kafka Consumer 멱등성 보장
-- 동일 이벤트 중복 수신 시 재처리 방지
-- ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS processed_events
(
    event_id   VARCHAR(255) PRIMARY KEY,            -- Kafka 메시지 Key 또는 이벤트 고유 ID
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ──────────────────────────────────────────────────────────
-- 재고 예약 추적 (보상 트랜잭션용)
-- OrderCreated 처리 성공 시 저장, OrderCancelled 처리 후 삭제
-- ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reserved_orders
(
    order_id   BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL
);
