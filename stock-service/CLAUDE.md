# stock-service

Redis 분산락(Redisson)을 활용하여 선착순 재고 차감을 안전하게 처리하는 서비스.

## 역할

- 상품별 재고 수량 관리 (등록, 조회, 차감, 복구)
- **Redisson 분산락**으로 동시 요청 시 Race Condition 방지
- Kafka 이벤트를 구독하여 재고 차감/복구 처리

## 베이스 패키지

```
io.github.jeongyounghyeon.stockservice
```

## 권장 패키지 구조

```
stock-service/src/main/kotlin/io/github/jeongyounghyeon/stockservice/
├── StockServiceApplication.kt
├── controller/
│   └── StockController.kt        ← 재고 조회 API
├── service/
│   └── StockService.kt           ← 재고 차감/복구 핵심 로직
├── domain/
│   └── Stock.kt                  ← 재고 엔티티
├── repository/
│   └── StockRepository.kt
├── consumer/
│   └── OrderEventConsumer.kt     ← OrderCreated / OrderCancelled 이벤트 수신
├── producer/
│   └── StockEventProducer.kt     ← StockReserved / StockFailed 발행
└── config/
    └── RedissonConfig.kt         ← Redisson 클라이언트 설정
```

## Redis 분산락 구현 원칙

### 재고 차감 흐름

```kotlin
fun reserveStock(productId: Long, quantity: Int) {
    val lock = redissonClient.getLock("stock:lock:$productId")
    lock.lock(10, TimeUnit.SECONDS)  // 최대 10초 보유
    try {
        val stock = stockRepository.findByProductId(productId)
            ?: throw StockNotFoundException(productId)
        if (stock.quantity < quantity) {
            throw InsufficientStockException(productId)
        }
        stock.decrease(quantity)
        stockRepository.save(stock)
    } finally {
        if (lock.isHeldByCurrentThread) lock.unlock()
    }
}
```

- 락 키 네이밍: `stock:lock:{productId}`
- 락 유효 시간은 비즈니스 로직 처리 시간보다 충분히 길게 설정
- `finally` 블록에서 반드시 락 해제 (currentThread 보유 확인 후)

## 핵심 도메인: Stock

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| productId | Long | 상품 ID (유니크) |
| quantity | Int | 현재 재고 수량 |
| updatedAt | Instant | 최종 수정 일시 |

## Choreography Saga 이벤트 흐름

```
[Order Service]                     [Stock Service]
OrderCreated   ──────────────────► 재고 차감 시도
                                    ├─ 성공: StockReserved 발행
                                    └─ 실패: StockFailed 발행

OrderCancelled ──────────────────► 재고 복구 (보상 트랜잭션)
                                       StockReleased 발행
```

### 구독하는 이벤트 (Consumer)

| 토픽 | 이벤트 | 처리 |
|------|--------|------|
| `order.created` | OrderCreatedEvent | 분산락 획득 후 재고 차감 |
| `order.cancelled` | OrderCancelledEvent | 재고 복구 (보상 트랜잭션) |

### 발행하는 이벤트 (Producer)

| 토픽 | 이벤트 | 조건 |
|------|--------|------|
| `stock.reserved` | StockReservedEvent | 재고 차감 성공 |
| `stock.failed` | StockFailedEvent | 재고 부족 또는 락 획득 실패 |
| `stock.released` | StockReleasedEvent | 재고 복구 완료 |

## 주요 API

| Method | Path | 설명 | 인증 필요 |
|--------|------|------|-----------|
| GET | `/api/stocks/{productId}` | 상품 재고 조회 | N |
| POST | `/api/stocks/{productId}` | 재고 등록 (관리자) | Y (ADMIN) |
| PATCH | `/api/stocks/{productId}` | 재고 수량 수정 (관리자) | Y (ADMIN) |

## 주의사항

- Kafka Consumer는 멱등성 보장: 동일 이벤트 중복 수신 시 재처리 방지 (처리된 이벤트 ID 추적)
- 재고 차감은 음수가 되어선 안 됨 (도메인 레벨에서 검증)
- 락 획득 실패(타임아웃) 시 `StockFailed` 이벤트 발행으로 보상 트랜잭션 유도
- Redisson은 `spring-boot-starter-data-redis` 대신 `redisson-spring-boot-starter` 의존성 사용