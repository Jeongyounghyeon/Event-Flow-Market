CREATE TABLE IF NOT EXISTS orders
(
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT      NOT NULL,                        -- members.id (서비스 간 FK 없음)
    product_id BIGINT      NOT NULL,                        -- stocks.product_id (서비스 간 FK 없음)
    quantity   INT         NOT NULL CHECK (quantity > 0),
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',      -- PENDING | CONFIRMED | CANCELLED
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_member_id ON orders (member_id);
CREATE INDEX IF NOT EXISTS idx_orders_status    ON orders (status);

-- ──────────────────────────────────────────────────────────
-- Transactional Outbox 패턴
-- 주문 저장과 동일 트랜잭션으로 삽입 → 폴러가 Kafka에 발행
-- ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS outbox_events
(
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,                   -- 'ORDER'
    aggregate_id   BIGINT       NOT NULL,                   -- orders.id
    event_type     VARCHAR(100) NOT NULL,                   -- 'ORDER_CREATED' | 'ORDER_CANCELLED'
    payload        TEXT         NOT NULL,                   -- 직렬화된 이벤트 데이터
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING | PUBLISHED | FAILED
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published_at   TIMESTAMPTZ                              -- Kafka 발행 성공 시각
);

CREATE INDEX IF NOT EXISTS idx_outbox_status     ON outbox_events (status);
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON outbox_events (created_at);
