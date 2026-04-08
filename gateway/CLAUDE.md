# gateway

클라이언트 요청을 각 마이크로서비스로 라우팅하는 API Gateway.

## 역할

- 단일 진입점(Single Entry Point)으로 모든 외부 요청을 수신
- JWT 토큰 검증 및 인증된 사용자 정보를 헤더로 전달
- 각 서비스로 요청 라우팅
- (선택) Rate Limiting, 로깅 필터

## 기술

- **Spring Cloud Gateway (WebMvc)**: 블로킹 방식의 게이트웨이 (WebFlux 아님)
- Spring Cloud 2025.1.1

## 베이스 패키지

```
io.github.jeongyounghyeon.gateway
```

## 권장 패키지 구조

```
gateway/src/main/kotlin/io/github/jeongyounghyeon/gateway/
├── GatewayApplication.kt
├── config/
│   └── RouteConfig.kt       ← 라우팅 규칙 정의
└── filter/
    └── JwtAuthFilter.kt     ← JWT 검증 필터
```

## 라우팅 규칙 설계 원칙

- URI 패턴 기준으로 라우팅: `/api/members/**` → member-service
- 인증이 필요 없는 경로 (회원가입, 로그인) 는 필터에서 제외
- 라우팅 설정은 `application.yml` 또는 `RouteConfig.kt`(Java DSL) 중 하나로 통일

```yaml
# application.yml 라우팅 예시
spring:
  cloud:
    gateway:
      mvc:
        routes:
          - id: member-service
            uri: http://member-service:8081
            predicates:
              - Path=/api/members/**
          - id: order-service
            uri: http://order-service:8082
            predicates:
              - Path=/api/orders/**
          - id: stock-service
            uri: http://stock-service:8083
            predicates:
              - Path=/api/stocks/**
```

## JWT 필터 처리 방식

1. `Authorization: Bearer <token>` 헤더에서 토큰 추출
2. 토큰 서명 및 만료 검증 (공개키 또는 공유 시크릿 사용)
3. 검증 성공 시 `Member-Id`, `X-Member-Role` 헤더를 추가하여 downstream 서비스에 전달
4. 검증 실패 시 `401 Unauthorized` 반환

## 서비스 포트 (기본값)

| 서비스 | 포트 |
|--------|------|
| gateway | 8080 |
| member-service | 8081 |
| order-service | 8082 |
| stock-service | 8083 |