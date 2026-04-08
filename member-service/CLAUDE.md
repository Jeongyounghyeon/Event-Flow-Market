# member-service

회원 가입, 로그인, 인증/인가를 담당하는 서비스.

## 역할

- OAuth 2.0 (소셜 로그인) 및 자체 이메일/비밀번호 인증
- JWT 액세스 토큰 & 리프레시 토큰 발급
- 사용자 프로필 조회 및 수정

## 베이스 패키지

```
io.github.jeongyounghyeon.memberservice
```

## 권장 패키지 구조

```
member-service/src/main/kotlin/io/github/jeongyounghyeon/memberservice/
├── MemberServiceApplication.kt
├── controller/
│   ├── AuthController.kt        ← 로그인, 토큰 재발급
│   └── MemberController.kt      ← 프로필 조회/수정
├── service/
│   ├── AuthService.kt
│   └── MemberService.kt
├── domain/
│   ├── Member.kt                ← 회원 엔티티
│   └── MemberRole.kt            ← ROLE_USER, ROLE_ADMIN
├── repository/
│   └── MemberRepository.kt
├── security/
│   ├── JwtProvider.kt           ← JWT 생성/검증
│   └── SecurityConfig.kt
└── dto/
    ├── LoginRequest.kt
    ├── TokenResponse.kt
    └── MemberProfileResponse.kt
```

## 핵심 도메인: Member

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| email | String | 유니크, 로그인 식별자 |
| password | String | BCrypt 해시 |
| nickname | String | 표시 이름 |
| role | MemberRole | USER / ADMIN |
| oauthProvider | String? | KAKAO / NAVER / null |
| oauthId | String? | OAuth 제공자의 사용자 ID |
| createdAt | Instant | 가입 일시 |

## JWT 토큰 전략

- **액세스 토큰**: 유효기간 30분, 요청마다 검증 (Gateway에서 검증)
- **리프레시 토큰**: 유효기간 14일, Redis에 저장 (로그아웃 시 삭제)
- 토큰 Payload: `memberId`, `role`, `iat`, `exp`

## 주요 API

| Method | Path | 설명 | 인증 필요 |
|--------|------|------|-----------|
| POST | `/api/members/signup` | 회원 가입 | N |
| POST | `/api/members/login` | 로그인, 토큰 발급 | N |
| POST | `/api/members/token/refresh` | 액세스 토큰 재발급 | N |
| GET | `/api/members/me` | 내 프로필 조회 | Y |
| PATCH | `/api/members/me` | 프로필 수정 | Y |

## 보안 주의사항

- 비밀번호는 반드시 BCrypt로 해시 저장 (평문 저장 절대 금지)
- JWT 시크릿 키는 환경변수로 관리 (`JWT_SECRET`)
- 리프레시 토큰 재사용 감지(RTR, Refresh Token Rotation) 구현 권장