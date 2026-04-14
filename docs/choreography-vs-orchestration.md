# Choreography vs Orchestration Saga

## 왜 Choreography Saga Pattern을 선택했는가?

해당 프로젝트 설계의 핵심 목표는 **서비스 간 결합도 최소화**, **확장성** 등이 있습니다.

| 관점 | Choreography (채택) | Orchestration |
|------|-------------------|---------------|
| 결합도 | 서비스는 이벤트 토픽만 알면 됨 | Orchestrator가 모든 서비스를 직접 호출 → 강한 결합 |
| 확장성 | 새 서비스 추가 시 이벤트 구독만 추가, 기존 서비스 수정 불필요 | Orchestrator 코드 수정 필요 |
| 단일 장애점 | 없음 | Orchestrator가 SPOF |
| Kafka 활용 | 이벤트 기반 구조와 자연스럽게 일치 | Kafka 없이 직접 호출(HTTP/gRPC)로 구현 가능 — 이벤트 버스가 필수 아님 |

## 현재 플로우와의 적합성

Order → Stock의 2단계 플로우에서는 Choreography가 적합합니다.

Orchestration이 유리한 경우는 결제·포인트·배송 등 여러 서비스에 걸친 복잡한 플로우일 때입니다. 
단계가 많아질수록 Choreography는 전체 흐름 파악이 어려워지고, Orchestration의 중앙 상태 관리가 오히려 더 유리합니다.

## 단점 보완

Choreography의 단점인 전체 Saga 흐름 파악의 어려움은 Zipkin 분산 추적으로 보완합니다.
`traceId`로 `order.created` → `stock.reserved` → `CONFIRMED` 전체 흐름을 추적할 수 있습니다.

## 서비스 확장 시 Orchestration 전환 고려

현재 2단계(Order → Stock) 플로우에서는 Choreography가 적합하지만, 서비스가 확장되어 결제·포인트·알림·배송 등이 추가되면 Orchestration 전환을 고려해야 합니다.

**전환을 고려해야 하는 시점:**

- Saga 단계가 늘어나 이벤트 흐름 파악이 어려워질 때
- 여러 서비스에 걸친 보상 트랜잭션 순서를 정확히 제어해야 할 때
- 비즈니스 요구사항으로 Saga 전체 상태를 중앙에서 조회해야 할 때

**전환 방향:**

Spring Modulith 또는 별도 Orchestrator 서비스(예: `order-saga-service`)를 도입하여 Saga 상태 머신을 중앙 관리하는 구조로 전환합니다.

Orchestration은 Kafka 없이 HTTP/gRPC 직접 호출로 구현할 수도 있지만, 이 프로젝트처럼 이미 Kafka 인프라가 갖춰진 경우 Kafka를 유지하면서 통신 방식만 변경할 수 있습니다. 이 경우 Orchestrator가 다음 단계를 명시적으로 지시하는 **Command**를 Kafka로 발행하고, 각 서비스는 처리 결과를 **Event**로 응답합니다. Choreography의 자율적 이벤트 반응과 달리, 전체 흐름의 제어권이 Orchestrator에 집중됩니다.

![Orchestration Saga 구조](../blob/main/docs/orchestration-flow.svg)
