# Event-Flow-Market

대규모 트래픽 대응을 위한 이벤트 기반 분산 커머스 시스템.

## 프로젝트 목적

- 서비스 간 결합도를 낮추고 확장성을 극대화한 MSA 아키텍처 설계
- 핵심 가치: 데이터 일관성 (Saga Pattern), 고가용성 (K8s), 비동기 처리 (Kafka)

## 기술 스택

| 영역 | 기술 |
|------|------|
| 언어 | Kotlin |
| 프레임워크 | Spring Boot 4.0.5, Spring Cloud 2025.1.1 |
| 빌드 | Gradle 8.x (Kotlin DSL), Java 24 |
| 메시징 | Apache Kafka (Choreography Saga, Outbox 패턴) |
| 캐싱/분산락 | Redis (Look-aside 캐싱, Redisson 분산락) |
| 인프라 | Docker, Kubernetes (kind/minikube → AWS EKS) |
| 오토스케일 | KEDA (Kafka Message Lag 기반) |
| 관측성 | Prometheus, Grafana, Zipkin |
| 부하 테스트 | k6 / Locust |

## 서비스 아키텍처

```
Client
  │
  ▼
[Gateway]  ← Spring Cloud Gateway (WebMvc), JWT 검증
  │
  ├──► [Member Service]   ← OAuth 2.0 & JWT 인증/인가
  ├──► [Order Service]    ← 주문 라이프사이클, Outbox 패턴
  └──► [Stock Service]    ← Redisson 분산락, 선착순 재고 차감

[Kafka] ← 서비스 간 이벤트 버스 (Choreography Saga)

[Common] ← 공유 라이브러리 (이벤트 DTO, 공통 예외 등)
```

## 모듈 목록

| 모듈 | 역할 | CLAUDE.md |
|------|------|-----------|
| `common` | 공통 라이브러리 (이벤트 클래스, 공통 예외, DTO) | [common/CLAUDE.md](common/CLAUDE.md) |
| `gateway` | API Gateway, 라우팅, JWT 필터 | [gateway/CLAUDE.md](gateway/CLAUDE.md) |
| `member-service` | 회원 가입/로그인, OAuth 2.0, JWT 발급 | [member-service/CLAUDE.md](member-service/CLAUDE.md) |
| `order-service` | 주문 생성/취소, Transactional Outbox 패턴 | [order-service/CLAUDE.md](order-service/CLAUDE.md) |
| `stock-service` | 재고 차감/복구, Redis 분산락 | [stock-service/CLAUDE.md](stock-service/CLAUDE.md) |

## 빌드 및 실행

```bash
# 전체 빌드
./gradlew build

# 특정 서비스 빌드
./gradlew :order-service:build

# 특정 서비스 실행
./gradlew :order-service:bootRun

# 테스트
./gradlew test
```

## 베이스 패키지

```
io.github.jeongyounghyeon.<모듈명>
```

예) `io.github.jeongyounghyeon.orderservice`

## 핵심 기술 시나리오

### Choreography Saga (이벤트 기반 분산 트랜잭션)

주문 → 재고 차감 → 결제 흐름에서 실패 시 보상 트랜잭션으로 롤백:

```
OrderCreated  →  StockReserved  →  PaymentApproved
     ↑                ↑                  ↑
OrderCanceled ←  StockReleased  ←  PaymentFailed  (보상 트랜잭션)
```

### Transactional Outbox 패턴

Order Service에서 DB 트랜잭션과 Kafka 이벤트 발행의 원자성 보장:
- 주문 저장과 Outbox 레코드 삽입을 같은 트랜잭션으로 처리
- 별도 폴러(Poller)가 Outbox 테이블을 읽어 Kafka에 발행

### Redis 분산락 (Redisson)

Stock Service에서 선착순 재고 차감 시 Race Condition 방지:
- `RLock`으로 상품 ID 기준 락 획득 후 차감

## 개발 로드맵

| 단계 | 목표 | 주요 작업 |
|------|------|-----------|
| 1단계 | 도메인 설계 | DB Schema, API 명세 (Swagger/RestDocs) |
| 2단계 | 기본 기능 | 각 서비스 핵심 CRUD, Gateway 라우팅 설정 |
| 3단계 | 이벤트 통합 | Kafka 연동, Saga 패턴, Outbox 패턴 구현 |
| 4단계 | 인프라 고도화 | Dockerizing, K8s Manifest, KEDA HPA |
| 5단계 | 품질 검증 | k6 부하 테스트, Grafana 대시보드, Zipkin 분산 추적 |

## 코드 컨벤션

- Kotlin 공식 코딩 컨벤션 준수
- 레이어드 아키텍처: `controller` → `service` → `repository` (도메인 로직은 `domain` 패키지)
- 이벤트 클래스는 `common` 모듈에 위치
- 각 서비스는 독립적으로 배포 가능해야 함 (서비스 간 직접 의존 금지)
