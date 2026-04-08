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
- [ ] 공통 API 응답 래퍼 `ApiResponse<T>`
- [ ] 공통 예외 클래스 (`BusinessException`, `ErrorCode`)
- [ ] Kafka 이벤트 DTO 클래스 정의
  - [ ] `OrderCreatedEvent`
  - [ ] `OrderCancelledEvent`
  - [ ] `StockReservedEvent`
  - [ ] `StockFailedEvent`
  - [ ] `StockReleasedEvent`
- [ ] Kafka 토픽 이름 상수 (`KafkaTopics`)

### Member Service
- [ ] Member 엔티티 및 Repository
- [ ] 회원 가입 API (`POST /api/members/signup`)
- [ ] 로그인 API (`POST /api/members/login`) — JWT 발급
- [ ] JWT Provider 구현 (생성/검증)
- [ ] 리프레시 토큰 Redis 저장 및 재발급 API
- [ ] 내 프로필 조회/수정 API
- [ ] Spring Security 설정

### Order Service
- [ ] Order 엔티티 및 Repository
- [ ] 주문 생성 API (`POST /api/orders`)
- [ ] 주문 조회 API (`GET /api/orders/{orderId}`)
- [ ] 주문 취소 API (`DELETE /api/orders/{orderId}`)

### Stock Service
- [ ] Stock 엔티티 및 Repository
- [ ] 재고 등록 API (`POST /api/stocks/{productId}`)
- [ ] 재고 조회 API (`GET /api/stocks/{productId}`)
- [ ] 재고 차감/복구 서비스 로직

### Gateway
- [ ] 서비스별 라우팅 규칙 설정
- [ ] JWT 검증 필터 구현 (`JwtAuthFilter`)
- [ ] 인증 불필요 경로 화이트리스트 설정

---

## 3단계: 이벤트 통합 (Kafka + Saga)

### Kafka 인프라
- [ ] `docker-compose.yml` 작성 (Kafka, Zookeeper, Redis, 각 서비스 DB)
- [ ] Kafka 토픽 생성 설정 (`order.created`, `order.cancelled`, `stock.reserved`, `stock.failed`, `stock.released`)

### Order Service — Transactional Outbox 패턴
- [ ] `OutboxEvent` 엔티티 및 Repository
- [ ] 주문 생성 시 Outbox 레코드 동시 저장 (단일 트랜잭션)
- [ ] Outbox 폴러 구현 (스케줄러로 PENDING 레코드 Kafka 발행)
- [ ] 발행 성공 후 status → PUBLISHED 업데이트

### Order Service — Kafka Consumer
- [ ] `StockReservedEvent` 수신 → 주문 상태 CONFIRMED
- [ ] `StockFailedEvent` 수신 → 주문 상태 CANCELLED, `OrderCancelledEvent` 발행

### Stock Service — Kafka Consumer + Redisson 분산락
- [ ] Redisson 의존성 추가 및 `RedissonConfig` 설정
- [ ] `OrderCreatedEvent` 수신 → 분산락 획득 후 재고 차감
  - [ ] 차감 성공 → `StockReservedEvent` 발행
  - [ ] 재고 부족 / 락 실패 → `StockFailedEvent` 발행
- [ ] `OrderCancelledEvent` 수신 → 재고 복구 (보상 트랜잭션) → `StockReleasedEvent` 발행
- [ ] Kafka Consumer 멱등성 보장 (처리된 이벤트 ID 중복 방지)

### 통합 테스트
- [ ] 주문 생성 → 재고 차감 → 주문 확정 E2E 흐름 검증
- [ ] 재고 부족 시 주문 취소 보상 트랜잭션 검증

---

## 4단계: 인프라 고도화 (Kubernetes)

### Docker
- [ ] 각 서비스 `Dockerfile` 작성 (멀티 스테이지 빌드)
- [ ] 로컬 `docker-compose.yml` 완성 (전체 스택)

### Kubernetes (로컬: kind 또는 minikube)
- [ ] 각 서비스 `Deployment` / `Service` Manifest 작성
- [ ] `ConfigMap` / `Secret` 분리 (DB URL, Kafka 주소, JWT 시크릿)
- [ ] `Liveness Probe` / `Readiness Probe` 설정
- [ ] KEDA `ScaledObject` 설정 (Kafka Message Lag 기반 오토스케일)

### AWS EKS (최종 단계)
- [ ] ECR에 이미지 푸시 자동화
- [ ] EKS 클러스터 생성 및 Manifest 적용

---

## 5단계: 품질 검증

### 관측성 (Observability)
- [ ] Prometheus + Grafana 설정 (`docker-compose` or Helm)
- [ ] 각 서비스 Actuator / Micrometer 메트릭 노출
- [ ] Grafana 대시보드 구성 (JVM, Kafka Lag, 재고 차감 TPS 등)
- [ ] Zipkin 분산 트래킹 설정 (Spring Cloud Sleuth or Micrometer Tracing)

### 부하 테스트
- [ ] k6 또는 Locust 스크립트 작성
- [ ] 선착순 재고 차감 시나리오 (동시 100명 요청)
- [ ] 부하 테스트 결과 문서화 (TPS, 에러율, 응답 시간 P99)
