# order-service

주문 라이프사이클을 관리하고 Transactional Outbox 패턴으로 Kafka 이벤트를 안정적으로 발행하는 서비스.

## 역할

- 주문 생성, 조회, 취소
- Choreography Saga의 오케스트레이션 출발점
- **Transactional Outbox 패턴**으로 DB 저장과 이벤트 발행의 원자성 보장

## 베이스 패키지

```
io.github.jeongyounghyeon.orderservice
```

## 권장 패키지 구조

```
order-service/src/main/kotlin/io/github/jeongyounghyeon/orderservice/
├── OrderServiceApplication.kt
├── controller/
│   └── OrderController.kt
├── service/
│   ├── OrderService.kt
│   └── OrderEventPublisher.kt    ← Outbox 폴러 (스케줄러)
├── domain/
│   ├── Order.kt                  ← 주문 엔티티
│   ├── OrderStatus.kt            ← PENDING, CONFIRMED, CANCELLED
│   └── OutboxEvent.kt            ← Outbox 테이블 엔티티
├── repository/
│   ├── OrderRepository.kt
│   └── OutboxEventRepository.kt
├── consumer/
│   └── StockEventConsumer.kt     ← StockReserved / StockFailed 이벤트 수신
└── dto/
    ├── OrderCreateRequest.kt
    └── OrderResponse.kt
```

## Transactional Outbox 패턴 구현 원칙

### 흐름

```
1. OrderService.createOrder()
   └─ [단일 트랜잭션]
       ├─ orders 테이블에 주문 저장 (status=PENDING)
       └─ outbox_events 테이블에 이벤트 저장 (status=PENDING)

2. OrderEventPublisher (스케줄러, 별도 트랜잭션)
   └─ outbox_events에서 PENDING 레코드 조회
       ├─ Kafka에 발행
       └─ outbox_events status → PUBLISHED 업데이트
```

### Outbox 테이블 스키마

| 컬럼 | 설명 |
|------|------|
| id | PK |
| aggregate_type | 집합체 타입 (예: "ORDER") |
| aggregate_id | 집합체 ID (주문 ID) |
| event_type | 이벤트 유형 (예: "ORDER_CREATED") |
| payload | JSON 직렬화된 이벤트 데이터 |
| status | PENDING / PUBLISHED / FAILED |
| created_at | 생성 일시 |
| published_at | Kafka 발행 일시 |

## Choreography Saga 이벤트 흐름

```
[Order Service]                [Stock Service]
OrderCreated 발행  ──────────► StockReserved 발행
                               또는
                               StockFailed 발행
                   ◄──────────
OrderConfirmed (성공)
또는
OrderCancelled (실패, 보상 트랜잭션)
```

### 구독하는 이벤트 (Consumer)

| 토픽 | 이벤트 | 처리 |
|------|--------|------|
| `stock.reserved` | StockReservedEvent | 주문 상태 → CONFIRMED |
| `stock.failed` | StockFailedEvent | 주문 상태 → CANCELLED |

### 발행하는 이벤트 (Outbox → Kafka)

| 토픽 | 이벤트 | 트리거 |
|------|--------|--------|
| `order.created` | OrderCreatedEvent | 주문 생성 |
| `order.cancelled` | OrderCancelledEvent | 주문 취소 (보상 트랜잭션) |

## 주요 API

| Method | Path | 설명 | 인증 필요 |
|--------|------|------|-----------|
| POST | `/api/orders` | 주문 생성 | Y |
| GET | `/api/orders/{orderId}` | 주문 상세 조회 | Y |
| GET | `/api/orders` | 내 주문 목록 | Y |
| DELETE | `/api/orders/{orderId}` | 주문 취소 | Y |

## 주의사항

- 주문 생성과 Outbox 저장은 **반드시 같은 트랜잭션**에서 처리
- Outbox 폴러는 멱등성 보장: 이미 발행된 이벤트는 재발행하지 않음
- Kafka 발행 실패 시 재시도 로직 필요 (status=FAILED 후 재시도 스케줄러)