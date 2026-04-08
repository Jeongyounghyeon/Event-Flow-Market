# common

모든 마이크로서비스가 공유하는 라이브러리 모듈.

## 역할

- 서비스 간 공유가 필요한 클래스를 한 곳에서 관리
- **실행 가능한 애플리케이션이 아님** (`bootJar` 비활성화, `jar` 활성화)
- 모든 서비스가 `implementation(project(":common"))`으로 의존

## 베이스 패키지

```
io.github.jeongyounghyeon.common
```

## 권장 패키지 구조

```
common/src/main/kotlin/io/github/jeongyounghyeon/common/
├── event/          ← Kafka 이벤트 DTO (OrderCreatedEvent, StockReservedEvent 등)
├── exception/      ← 공통 예외 클래스 (BusinessException, ErrorCode 등)
├── response/       ← 공통 API 응답 래퍼 (ApiResponse<T>)
└── util/           ← 공유 유틸리티 (날짜 변환 등)
```

## 여기에 넣어야 할 것

- **Kafka 이벤트 클래스**: 서비스 간 메시지 계약 (Producer/Consumer 양쪽이 사용)
- **공통 예외**: `BusinessException`, `ErrorCode` enum
- **공통 API 응답**: `ApiResponse<T>` 래퍼
- **공유 상수**: 토픽 이름 상수 (`KafkaTopics.ORDER_CREATED` 등)

## 여기에 넣으면 안 되는 것

- 특정 서비스에만 해당하는 비즈니스 로직
- DB Entity 클래스 (각 서비스의 영속성 계층은 분리)
- Spring Security 설정 (각 서비스가 독립적으로 보안 설정)
- 서비스 특화 DTO (예: `OrderCreateRequest`는 order-service에 위치)

## 이벤트 클래스 작성 규칙

```kotlin
// 이벤트 클래스는 불변으로 설계
data class OrderCreatedEvent(
    val orderId: Long,
    val memberId: Long,
    val productId: Long,
    val quantity: Int,
    val occurredAt: Instant = Instant.now()
)
```

- 이벤트 클래스는 `data class`로 작성
- `occurredAt` 필드로 발생 시각 포함
- JSON 직렬화 가능하도록 기본 생성자 또는 `@JsonCreator` 적용

## 빌드 특이사항

- `bootJar` 비활성화 → 실행 가능한 JAR 생성 안 됨
- `jar` 활성화 → 일반 라이브러리 JAR로 다른 모듈이 참조