# 카카오 소셜 로그인 인증/인가 체계

## 목차

1. [전체 아키텍처 개요](#1-전체-아키텍처-개요)
2. [OAuth 2.0 Authorization Code Flow](#2-oauth-20-authorization-code-flow)
3. [JWT 토큰 발급 및 검증](#3-jwt-토큰-발급-및-검증)
4. [Redis 세션 관리](#4-redis-세션-관리)
5. [Spring Security 설정](#5-spring-security-설정)
6. [JWT 인증 필터](#6-jwt-인증-필터)
7. [로그인 전체 흐름 (Sequence)](#7-로그인-전체-흐름-sequence)
8. [로그아웃 흐름](#8-로그아웃-흐름)
9. [인가(Authorization) 체계](#9-인가authorization-체계)
10. [데이터 모델](#10-데이터-모델)
11. [주요 보안 설계 포인트](#11-주요-보안-설계-포인트)
12. [주요 파일 위치](#12-주요-파일-위치)

---

## 1. 전체 아키텍처 개요

```
[Frontend]  ←→  [API Gateway :8000]  ←→  [Portal Service :8080]  ←→  [Kakao Servers]
                                                    ↓
                                              [Redis :6379]
                                                    ↓
                                              [DB (MySQL)]
```

### 핵심 설계 원칙

- **Stateless 인증**: HttpSession 미사용, JWT 기반 요청 인증
- **최소 권한 JWT**: JWT payload에 `sessionId(UUID)` 만 포함 (사용자 정보 없음)
- **Redis 세션**: 실제 사용자 정보(`userId`, `nickname`, `email` 등)는 Redis에 저장
- **토큰 블랙리스트**: 로그아웃 시 Redis에 블랙리스트 등록 → 만료 전 토큰 즉시 무효화

---

## 2. OAuth 2.0 Authorization Code Flow

### 관련 파일

- Controller: `service/portal/.../controller/login/kakao/KakaoAuthController.java`
- Service: `service/portal/.../service/login/kakao/KakaoAuthServiceImpl.java`

### 카카오 OAuth 설정값

```yaml
# application-local.yml
kakao:
  registration:
    client-id: <KAKAO_CLIENT_ID>
    client-secret: <KAKAO_CLIENT_SECRET>
    redirect-uri: http://192.168.0.8:8080/api/v1/auth/kakao/callback
    authorization-grant-type: authorization_code
```

### 사용하는 카카오 API 엔드포인트

| 역할 | URL |
|------|-----|
| OAuth 인가 페이지 | `https://kauth.kakao.com/oauth/authorize` |
| 토큰 발급 | `https://kauth.kakao.com/oauth/token` |
| 사용자 정보 조회 | `https://kapi.kakao.com/v2/user/me` |
| 연결 끊기 | `https://kapi.kakao.com/v1/user/unlink` |

### 노출된 엔드포인트

| Method | Path | 설명 | 인증 필요 |
|--------|------|------|-----------|
| GET | `/api/v1/auth/kakao/login` | 카카오 OAuth 인가 URL 반환 | No |
| GET | `/api/v1/auth/kakao/callback?code=xxx` | OAuth 콜백 처리 | No |
| POST | `/api/v1/auth/kakao/unlink?userId=xxx` | 카카오 계정 연결 해제 | Yes |
| POST | `/api/v1/auth/logout` | 로그아웃 | Yes |

---

## 3. JWT 토큰 발급 및 검증

### 관련 파일

`common/src/main/java/kwak/common/config/security/JwtTokenProvider.java`

### 토큰 설정

```yaml
jwt:
  secret: ${JWT_SECRET:<32자 이상 비밀키>}
  expiration: 86400000   # 24시간 (밀리초)
```

### JWT 구조

```
Header: { alg: "HS256", typ: "JWT" }
Payload: { sub: "<sessionId (UUID)>", iat: <발급시각>, exp: <만료시각 +24h> }
Signature: HMAC-SHA256(base64(Header) + "." + base64(Payload), secretKey)
```

> **중요:** JWT payload에는 `sessionId` 만 포함됩니다. `userId`, `email` 등의 사용자 정보는 Redis에서 조회합니다.

### 주요 메서드

| 메서드 | 설명 |
|--------|------|
| `createToken(sessionId)` | JWT 서명 후 발급 |
| `generateSessionId()` | UUID 기반 sessionId 생성 |
| `validateToken(token)` | 서명 검증 + Redis 블랙리스트 확인 |
| `getSessionId(token)` | JWT subject(sessionId) 추출 |
| `getRemainingExpiration(token)` | 남은 만료 시간(ms) 계산 |
| `invalidateToken(token)` | Redis 세션 삭제 + 블랙리스트 등록 |

### 토큰 전달 방식

클라이언트는 두 가지 방법으로 토큰을 전달할 수 있으며, **Authorization 헤더가 우선** 적용됩니다.

1. **HTTP 헤더**: `Authorization: Bearer <token>`
2. **쿠키**: `accessToken=<token>` (로그인 응답 시 자동 설정)

---

## 4. Redis 세션 관리

### 관련 파일

- `common/src/main/java/kwak/common/infrastructure/token/RedisTokenStore.java`
- `common/src/main/java/kwak/common/infrastructure/token/UserSession.java`
- `common/src/main/java/kwak/common/config/RedisConfig.java`

### Redis 키 구조

| 키 패턴 | 값 | TTL | 목적 |
|---------|----|-----|------|
| `auth:session:{sessionId}` | UserSession JSON | 24시간 | 사용자 세션 저장 |
| `auth:blacklist:{token}` | `"logout"` | 남은 JWT 만료 시간 | 로그아웃된 토큰 블랙리스트 |

### UserSession 모델

```java
public class UserSession {
    private String userId;        // User UUID
    private String nickname;      // 사용자 닉네임
    private String email;         // 이메일
    private String profileImgUrl; // 프로필 이미지 URL
}
```

### Redis 설정

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 3000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

- Key 직렬화: `StringRedisSerializer`
- Value 직렬화: `GenericJackson2JsonRedisSerializer`

---

## 5. Spring Security 설정

### 관련 파일

`service/portal/src/main/java/com/investment/portal/config/SecurityConfig.java`

### 주요 설정

```java
http
  .cors(...)                                    // CORS 활성화
  .csrf(csrf -> csrf.disable())                 // CSRF 비활성화 (Stateless API)
  .sessionManagement(session ->
      session.sessionCreationPolicy(STATELESS)) // HttpSession 미사용
  .formLogin(form -> form.disable())            // Form 로그인 비활성화
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/api/v1/auth/**").permitAll()
      .requestMatchers("/api/v1/stocks/price/**").permitAll()
      .requestMatchers("/swagger", "/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
      .requestMatchers("/error").permitAll()
      .anyRequest().authenticated()
  )
  .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

### 공개(인증 불필요) 엔드포인트

| 패턴 | 설명 |
|------|------|
| `/api/v1/auth/**` | 로그인/로그아웃/OAuth 콜백 |
| `/api/v1/stocks/price/**` | 공개 주식 시세 조회 |
| `/swagger`, `/swagger-ui/**` 등 | API 문서 |
| `/error` | 오류 페이지 |

### CORS 설정

```java
// common/.../config/WebConfig.java
registry.addMapping("/**")
        .allowedOrigins("http://192.168.0.8:5173")  // Frontend URL
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
```

---

## 6. JWT 인증 필터

### 관련 파일

`common/src/main/java/kwak/common/config/security/JwtAuthenticationFilter.java`

### 동작 방식

`OncePerRequestFilter`를 확장하여 모든 HTTP 요청에서 1회 실행됩니다.

```
HTTP 요청 수신
    ↓
토큰 추출 (Authorization 헤더 → Cookie 순)
    ↓
JWT 서명 검증 + 블랙리스트 확인
    ↓ (유효한 경우)
JWT에서 sessionId 추출
    ↓
Redis에서 UserSession 조회
    ↓
UsernamePasswordAuthenticationToken 생성
    principal: userId
    credentials: UserSession
    authorities: [ROLE_USER]
    ↓
SecurityContext에 저장
    ↓
다음 필터 / 컨트롤러로 전달
```

---

## 7. 로그인 전체 흐름 (Sequence)

```
Frontend                  Portal Service              Kakao Servers         Redis / DB
   |                           |                            |                   |
   |-- GET /api/v1/auth/kakao/login -->                    |                   |
   |<-- 카카오 OAuth URL 반환 --                            |                   |
   |                           |                            |                   |
   |-- (브라우저가 카카오 로그인 페이지로 리다이렉트) -------->|                   |
   |                           |                            |                   |
   |<-- code=xxx 포함한 callback URL로 리다이렉트 ----------|                   |
   |                           |                            |                   |
   |-- GET /api/v1/auth/kakao/callback?code=xxx ---------->|                   |
   |                           |                            |                   |
   |                           |-- POST /oauth/token ------>|                   |
   |                           |   (code, client_id, ...)   |                   |
   |                           |<-- accessToken, refreshToken (미저장) ---------|
   |                           |                            |                   |
   |                           |-- GET /v2/user/me -------->|                   |
   |                           |   (Authorization: Bearer accessToken)         |
   |                           |<-- { id, email, nickname, profileImage } -----|
   |                           |                            |                   |
   |                           |-- findOrCreateUser() ---------------------------> DB
   |                           |   [신규 사용자] tbl_user INSERT + tbl_user_social INSERT
   |                           |   [기존 사용자] lastLoginDt UPDATE             |
   |                           |<------------------------------------------------|
   |                           |                            |                   |
   |                           |-- generateSessionId() → UUID                  |
   |                           |-- createToken(sessionId) → JWT (exp 24h)      |
   |                           |                            |                   |
   |                           |-- saveSession(sessionId, UserSession, 24h) ---> Redis
   |                           |   key: auth:session:{sessionId}               |
   |                           |   val: { userId, nickname, email, profileImgUrl }
   |                           |                            |                   |
   |<-- Set-Cookie: accessToken={JWT} + Redirect /oauth/callback ---------------|
   |                           |                            |                   |
```

### 이후 인증된 요청 흐름

```
Frontend                  JwtAuthenticationFilter        Redis              Controller
   |                              |                         |                  |
   |-- GET /api/v1/... (Bearer JWT 또는 accessToken 쿠키) ->|                  |
   |                              |                         |                  |
   |                              |-- validateToken(JWT) -->|                  |
   |                              |   - 서명 검증           |                  |
   |                              |   - 블랙리스트 확인: auth:blacklist:{JWT}  |
   |                              |                         |                  |
   |                              |-- getSessionId(JWT) → sessionId           |
   |                              |                         |                  |
   |                              |-- getSession(sessionId) -> Redis          |
   |                              |   key: auth:session:{sessionId}           |
   |                              |<-- UserSession { userId, nickname, ... } --|
   |                              |                         |                  |
   |                              |-- SecurityContext에 인증 정보 설정        |
   |                              |   principal: userId                        |
   |                              |   authorities: [ROLE_USER]                 |
   |                              |                                            |
   |                              |---------------------------------------------> Controller 처리
   |<-- 응답 -------------------------------------------------------------------|
```

---

## 8. 로그아웃 흐름

```
Frontend                  Portal Service (AuthController)        Redis
   |                               |                               |
   |-- POST /api/v1/auth/logout -->|                               |
   |   (Bearer JWT 또는 쿠키)      |                               |
   |                               |-- invalidateToken(token)      |
   |                               |   1. getSessionId(token)      |
   |                               |   2. deleteSession(sessionId) --> auth:session:{sessionId} 삭제
   |                               |   3. getRemainingExpiration(token) → remainingMs
   |                               |   4. addToBlacklist(token, remainingMs) --> auth:blacklist:{token} 등록 (TTL=remainingMs)
   |                               |                               |
   |<-- 200 OK (accessToken 쿠키 삭제) -----------------------------|
```

> 로그아웃 후 동일한 JWT로 재요청하면 블랙리스트 체크에서 차단됩니다.

---

## 9. 인가(Authorization) 체계

### 현재 구조

모든 인증된 사용자는 단일 권한 `ROLE_USER`를 부여받습니다.
세분화된 역할 기반 접근 제어(RBAC)는 미구현 상태입니다.

```java
// JwtAuthenticationFilter.java
List.of(new SimpleGrantedAuthority("ROLE_USER"))
```

### 접근 제어 계층

```
요청
 ├── /api/v1/auth/**           → 모든 사용자 접근 가능 (인증 불필요)
 ├── /api/v1/stocks/price/**   → 모든 사용자 접근 가능 (인증 불필요)
 ├── /swagger*, /api-docs/**   → 모든 사용자 접근 가능 (인증 불필요)
 └── 그 외 모든 경로            → 인증된 사용자만 접근 가능 (ROLE_USER)
```

### SecurityContext에서 사용자 정보 접근

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String userId = (String) auth.getPrincipal();           // userId
UserSession session = (UserSession) auth.getCredentials(); // 세션 정보
```

---

## 10. 데이터 모델

### tbl_user (사용자 테이블)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `user_id` | VARCHAR | UUID (PK) |
| `user_nm` | VARCHAR | 사용자 이름 |
| `nickname` | VARCHAR | 닉네임 (Unique) |
| `email` | VARCHAR | 이메일 (Unique) |
| `password` | VARCHAR | BCrypt 해시 (소셜 전용 계정은 NULL) |
| `profile_img_url` | VARCHAR | 프로필 이미지 URL |
| `use_yn` | CHAR(1) | 활성 여부 (Y/N) |
| `reg_dt` | DATETIME | 가입일 |
| `upd_dt` | DATETIME | 수정일 |
| `last_login_dt` | DATETIME | 마지막 로그인 일시 |

### tbl_user_social (소셜 연동 테이블)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `social_id` | BIGINT | Auto-increment (PK) |
| `user_id` | VARCHAR | tbl_user FK |
| `provider` | VARCHAR | KAKAO / GOOGLE / NAVER / APPLE |
| `provider_user_id` | VARCHAR | 카카오 고유 사용자 ID |
| `connected_dt` | DATETIME | 최초 연결일 |
| `last_login_dt` | DATETIME | 마지막 소셜 로그인 일시 |
| `token_expired_dt` | DATETIME | (미사용, 향후 확장용) |
| `use_yn` | CHAR(1) | 활성 여부 (Y/N) |

> **카카오 토큰 미저장 정책:** Kakao accessToken / refreshToken은 DB에 저장하지 않습니다.
> accessToken은 사용자 정보 조회 후 즉시 폐기하며, refreshToken도 수신하지만 저장하지 않습니다.

### 신규 사용자 판단 로직

```
tbl_user_social에서 (provider=KAKAO, provider_user_id=카카오ID) 조회
    ├── 존재함 → 기존 사용자: lastLoginDt 업데이트 후 로그인 처리 (isNewUser=false)
    └── 없음   → 신규 사용자: tbl_user + tbl_user_social INSERT (isNewUser=true)
```

---

## 11. 주요 보안 설계 포인트

| 항목 | 설계 |
|------|------|
| **JWT Payload 최소화** | sessionId만 포함 → 토큰 탈취 시 사용자 정보 노출 없음 |
| **세션 서버 저장** | 사용자 정보는 Redis에 보관 → JWT 자체는 무상태 |
| **즉시 로그아웃** | 블랙리스트 방식으로 JWT 만료 전 무효화 가능 |
| **비밀번호 해싱** | BCryptPasswordEncoder 사용 (이메일 로그인) |
| **CORS 제한** | 특정 Origin(프론트엔드 URL)에만 허용 |
| **CSRF 비활성화** | Stateless API, 쿠키 기반 상태 없음 |
| **Stateless 세션** | `SessionCreationPolicy.STATELESS` → HttpSession 미생성 |
| **토큰 전달 이중화** | Authorization 헤더 또는 `accessToken` 쿠키 모두 지원 |
| **소셜 토큰 미저장** | 카카오 토큰은 DB 저장 없이 사용 후 폐기 |

### 현재 미구현 / 개선 가능 항목

| 항목 | 현황 | 비고 |
|------|------|------|
| **JWT Refresh 토큰** | 미구현 | 24시간 후 재로그인 필요 |
| **RBAC** | 단일 `ROLE_USER`만 존재 | 관리자 등 역할 분리 가능 |
| **Kakao Refresh 토큰 활용** | 수신하지만 미저장 | `token_expired_dt` 컬럼 준비됨 |
| **쿠키 Secure 플래그** | 개발환경 false | 운영환경에서 true 필요 |

---

## 12. 주요 파일 위치

```
kwak-service/
├── common/src/main/java/kwak/common/
│   ├── config/
│   │   ├── RedisConfig.java                        # Redis 연결 및 직렬화 설정
│   │   ├── WebConfig.java                          # CORS 설정
│   │   └── security/
│   │       ├── JwtTokenProvider.java               # JWT 발급/검증/무효화
│   │       └── JwtAuthenticationFilter.java        # JWT 인증 필터 (OncePerRequestFilter)
│   └── infrastructure/token/
│       ├── RedisTokenStore.java                    # Redis 세션/블랙리스트 저장소
│       └── UserSession.java                        # Redis 세션 모델
│
└── service/portal/src/main/java/com/investment/portal/
    ├── config/
    │   ├── SecurityConfig.java                     # Spring Security 설정
    │   ├── WebClientConfig.java                    # WebClient 설정 (카카오 API 호출용)
    │   └── exception/
    │       └── GlobalExceptionHandler.java         # 전역 예외 처리
    ├── api/controller/login/
    │   ├── AuthController.java                     # POST /api/v1/auth/logout
    │   ├── kakao/
    │   │   └── KakaoAuthController.java            # 카카오 OAuth 엔드포인트
    │   └── standard/
    │       └── StandardAuthController.java         # 이메일/비밀번호 로그인
    ├── application/
    │   ├── dto/login/
    │   │   ├── OAuth2Response.java                 # 소셜 로그인 응답 인터페이스
    │   │   └── kakao/
    │   │       ├── KakaoTokenResponse.java         # 카카오 토큰 응답 DTO
    │   │       └── KakaoUserInfo.java              # 카카오 사용자 정보 DTO
    │   └── service/login/
    │       ├── LoginResponse.java                  # 로그인 응답 DTO
    │       ├── kakao/
    │       │   └── KakaoAuthServiceImpl.java       # 카카오 OAuth 비즈니스 로직
    │       └── standard/
    │           └── StandardAuthServiceImpl.java    # 이메일 로그인 비즈니스 로직
    └── domain/
        ├── entity/user/
        │   ├── User.java                           # 사용자 엔티티
        │   └── UserSocial.java                     # 소셜 연동 엔티티
        ├── enums/
        │   └── SocialProvider.java                 # KAKAO, GOOGLE, NAVER, APPLE
        └── repository/user/
            ├── UserMapper.java                     # MyBatis User 매퍼 인터페이스
            └── UserSocialMapper.java               # MyBatis UserSocial 매퍼 인터페이스
```
