# Event-Flow-Market 구현 계획

## 1단계: 도메인 설계

### DB Schema 설계
- [x] 프로젝트 멀티모듈 구조 세팅 (common, gateway, member-service, order-service, stock-service)
- [x] Member 테이블 설계
- [x] Order 테이블 설계
- [x] OutboxEvent 테이블 설계
- [x] Stock 테이블 설계

### API 명세 설계
- [x] Member Service API 명세 (Spring REST Docs)
- [x] Order Service API 명세
- [x] Stock Service API 명세

---

## 2단계: 기본 기능 구현

### Common 모듈
- [x] 공통 API 응답 래퍼 `ApiResponse<T>`
- [x] 공통 예외 클래스 (`BusinessException`, `ErrorCode`)
- [x] Kafka 이벤트 DTO 클래스 정의
  - [x] `OrderCreatedEvent`
  - [x] `OrderCancelledEvent`
  - [x] `StockReservedEvent`
  - [x] `StockFailedEvent`
  - [x] `StockReleasedEvent`
- [x] Kafka 토픽 이름 상수 (`KafkaTopics`)

### Member Service
- [x] Member 엔티티 및 Repository
- [x] 회원 가입 API (`POST /api/members/signup`)
- [x] 로그인 API (`POST /api/members/login`) — JWT 발급
- [x] JWT Provider 구현 (생성/검증)
- [x] 리프레시 토큰 Redis 저장 및 재발급 API
- [x] 내 프로필 조회/수정 API
- [x] Spring Security 설정

### Order Service
- [x] Order 엔티티 및 Repository
- [x] 주문 생성 API (`POST /api/orders`)
- [x] 주문 조회 API (`GET /api/orders/{orderId}`)
- [x] 주문 취소 API (`DELETE /api/orders/{orderId}`)

### Stock Service
- [x] Stock 엔티티 및 Repository
- [x] 재고 등록 API (`POST /api/stocks/{productId}`)
- [x] 재고 조회 API (`GET /api/stocks/{productId}`)
- [x] 재고 차감/복구 서비스 로직

### Gateway
- [x] 서비스별 라우팅 규칙 설정
- [x] JWT 검증 필터 구현 (`JwtAuthFilter`)
- [x] 인증 불필요 경로 화이트리스트 설정

---

## 3단계: 이벤트 통합 (Kafka + Saga)

### Kafka 인프라
- [x] `docker-compose.yml` 작성 (Kafka KRaft, Redis, 각 서비스 DB)
- [x] Kafka 토픽 자동 생성 (`KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"` → KafkaAdmin으로 생성 가능)

### Order Service — Transactional Outbox 패턴
- [x] `OutboxEvent` 엔티티 및 Repository
- [x] 주문 생성 시 Outbox 레코드 동시 저장 (단일 트랜잭션)
- [x] Outbox 폴러 구현 (`OrderEventPublisher`, @Scheduled fixedDelay=1s)
- [x] 발행 성공 후 status → PUBLISHED 업데이트

### Order Service — Kafka Consumer
- [x] `StockReservedEvent` 수신 → 주문 상태 CONFIRMED (`StockEventConsumer`)
- [x] `StockFailedEvent` 수신 → 주문 상태 CANCELLED, `OrderCancelledEvent` 발행

### Stock Service — Kafka Consumer + Redisson 분산락
- [x] Redisson 의존성 추가 (`redisson:3.43.0`) 및 `RedissonConfig` 설정
- [x] `OrderCreatedEvent` 수신 → 분산락 획득 후 재고 차감 (`OrderEventConsumer`)
  - [x] 차감 성공 → `StockReservedEvent` 발행
  - [x] 재고 부족 / 락 실패 → `StockFailedEvent` 발행
- [x] `OrderCancelledEvent` 수신 → 재고 복구 (보상 트랜잭션) → `StockReleasedEvent` 발행
- [x] Kafka Consumer 멱등성 보장 (`ProcessedEvent` 테이블로 eventId 중복 방지)
- [x] 보상 트랜잭션 정확성 (`ReservedOrder` 테이블로 실제 예약된 주문만 복구)

### 통합 테스트
- [x] 주문 생성 → 재고 차감 → 주문 확정 E2E 흐름 검증
- [x] 재고 부족 시 주문 취소 보상 트랜잭션 검증

---

## 4단계: 인프라 고도화 (Kubernetes)

### Docker
- [x] 각 서비스 `Dockerfile` 작성 (멀티 스테이지 빌드)
- [x] 로컬 `docker-compose.yml` 완성 (전체 스택)

### Kubernetes (로컬: kind 또는 minikube)
- [x] 각 서비스 `Deployment` / `Service` Manifest 작성
- [x] `ConfigMap` / `Secret` 분리 (DB URL, Kafka 주소, JWT 시크릿)
- [x] `Liveness Probe` / `Readiness Probe` 설정
- [x] KEDA `ScaledObject` 설정 (Kafka Message Lag 기반 오토스케일)


---

## 5단계: 품질 검증

### 관측성 (Observability)
- [x] Prometheus + Grafana 설정 (`docker-compose`)
  - [x] `kafka-exporter` 컨테이너로 Kafka 메트릭 노출
  - [x] Grafana datasource / dashboard provider 자동 프로비저닝
- [x] 각 서비스 Actuator / Micrometer 메트릭 노출 (`/actuator/prometheus`)
- [x] Grafana 대시보드 구성 (JVM, Kafka Lag, Spring Boot Statistics)
- [x] Zipkin 분산 추적 설정 (Micrometer Tracing + Brave)
  - [x] `spring-boot-starter-zipkin` 의존성 추가 (4개 서비스)
  - [x] traceId / spanId 로그 패턴 연동 (`logging.pattern.level`)
  - [x] Kafka producer/consumer observation 활성화 (`observation-enabled: true`)
  - [x] Gateway → 서비스 간 traceId 전파 확인 (B3 propagation)
  - [x] Kafka Saga 흐름 traceId 연결 확인 (order-service → stock-service → order-service)

### 부하 테스트
- [x] k6 스크립트 작성 (`k6/stock-reservation.js`)
- [x] 선착순 재고 차감 시나리오 (동시 100명 요청, 재고 100개)
- [x] 부하 테스트 결과 확인 (TPS, 에러율, 응답 시간 P95)
