# kwak-service (inv-back) 아키텍처

## 프로젝트 구조

```
kwak-service - 멀티모듈 Gradle, Java 21, Spring Boot 3.5.9
│
├── settings.gradle              루트 설정 (6개 모듈 include)
├── build.gradle                 공통 플러그인/의존성 관리
│
├── common/                      공통 라이브러리 (java-library)
│   ├── RokResponse<T>           통일된 API 응답 포맷
│   ├── PageResponse<T>          페이지네이션 응답
│   ├── ResponseUtil             HTTP 응답 헬퍼 (success, error, redirect 등)
│   ├── AuthenticationException  인증 예외
│   └── ObjectUtil               null/string 유틸
│
├── service/api-gateway/         API 게이트웨이 (포트 8000) ← 단일 진입점
│   ├── Spring Cloud Gateway (WebFlux 기반)
│   ├── 경로 기반 라우팅 (하위 서비스 포트 비노출)
│   └── 글로벌 CORS 설정
│
├── service/portal/              메인 서비스 (포트 8080)
│   ├── 카카오 OAuth 소셜 로그인
│   ├── JWT 인증 + Redis 세션 관리
│   ├── 포트폴리오 CRUD
│   ├── 거래내역 관리
│   ├── 실시간 주가 (Finnhub API + Caffeine 캐시)
│   └── Swagger, Actuator
│
├── service/survey-service/      설문조사 서비스 (포트 8081)
│   ├── 설문 CRUD / 응답 제출
│   ├── 설문 통계
│   ├── 공통코드 관리
│   └── JWT 인증 (독립)
│
├── service/stock-advisor/       주식 조언 서비스 (포트 8082)
│
└── service/market-analyzer/     시장 분석 서비스 (포트 8083)
    └── 시장 통계 API
```

## 의존성 관계

```
common ← portal, stock-advisor, market-analyzer, survey-service

                         ┌─────────────────────────┐
                         │   Client (Vue :5173)     │
                         └────────────┬────────────┘
                                      │ :8000
                         ┌────────────▼────────────┐
                         │      api-gateway         │  Spring Cloud Gateway
                         └──┬──────┬──────┬────┬───┘
                            │      │      │    │
                         :8080  :8081  :8082  :8083
                         portal survey advisor market
```

각 서비스는 독립 실행 가능한 Spring Boot 앱이며, 서비스 간 직접 호출은 없음.
외부에는 api-gateway(:8000)만 노출하고, 하위 서비스 포트는 내부 네트워크에서만 접근한다.

---

## API Gateway

### 목적

클라이언트가 하위 서비스의 포트를 직접 알지 않아도 되도록 단일 진입점을 제공한다.
모든 요청은 `:8000`으로 들어와 경로에 따라 적절한 서비스로 라우팅된다.

### 기술

- **Spring Cloud Gateway** (Spring Cloud 2024.0.x)
- WebFlux 기반 비동기/논블로킹 처리
- `RewritePath` 필터로 경로 그대로 하위 서비스에 전달

### 라우팅 규칙

| 클라이언트 요청 경로         | 라우팅 대상           | 환경변수      |
| ---------------------------- | --------------------- | ------------- |
| `/api/v1/auth/**`            | portal :8080          | `PORTAL_URI`  |
| `/api/v1/portfolios/**`      | portal :8080          | `PORTAL_URI`  |
| `/api/v1/portfolio-items/**` | portal :8080          | `PORTAL_URI`  |
| `/api/v1/transactions/**`    | portal :8080          | `PORTAL_URI`  |
| `/api/v1/stocks/**`          | portal :8080          | `PORTAL_URI`  |
| `/api/v1/codes/**`           | survey-service :8081  | `SURVEY_URI`  |
| `/api/v1/surveys/**`         | survey-service :8081  | `SURVEY_URI`  |
| `/api/v1/surveys-stats/**`   | survey-service :8081  | `SURVEY_URI`  |
| `/api/v1/guru/**`            | stock-advisor :8082   | `ADVISOR_URI` |
| `/api/v1/users/**`           | stock-advisor :8082   | `ADVISOR_URI` |
| `/api/v1/markets/**`         | market-analyzer :8083 | `MARKET_URI`  |

### CORS

글로벌 CORS를 게이트웨이에서 일괄 처리한다. 하위 서비스의 개별 CORS 설정은 불필요하다.

```
허용 Origin : http://localhost:5173, http://192.168.0.8:5173
허용 Method : GET, POST, PUT, DELETE, PATCH, OPTIONS
허용 Header : *
Credentials : true
```

### 환경 프로파일

| 프로파일       | 하위 서비스 URI                                          |
| -------------- | -------------------------------------------------------- |
| `local` (기본) | `http://localhost:{port}`                                |
| `docker`       | `http://{service-name}:{port}` (docker-compose 서비스명) |

### 환경변수

| 변수          | 기본값 (local)          | Docker 값                     |
| ------------- | ----------------------- | ----------------------------- |
| `PORTAL_URI`  | `http://localhost:8080` | `http://portal:8080`          |
| `SURVEY_URI`  | `http://localhost:8081` | `http://survey-service:8081`  |
| `ADVISOR_URI` | `http://localhost:8082` | `http://stock-advisor:8082`   |
| `MARKET_URI`  | `http://localhost:8083` | `http://market-analyzer:8083` |

### 실행

```bash
# 로컬
./gradlew :service:api-gateway:bootRun

# Docker 빌드
docker build -f Dockerfile.gateway -t api-gateway .
```

---

## 인증 아키텍처 (Portal)

### JWT 토큰 구조

```
JWT Payload: { sub: "sessionId (UUID)", iat: 발급시간, exp: 만료시간 }
```

- JWT에는 **sessionId(UUID)만 포함**, userId 등 유저 정보는 미포함
- 유저 정보는 Redis에서만 관리 → JWT 탈취 시에도 유저 식별 불가

### Redis 저장 구조

| Redis Key                  | Value                                        | TTL                  | 용도                       |
| -------------------------- | -------------------------------------------- | -------------------- | -------------------------- |
| `auth:session:{sessionId}` | `{ userId, nickname, email, profileImgUrl }` | 24h (jwt.expiration) | 유저 세션 (sessionId 기반) |
| `auth:blacklist:{token}`   | `"logout"`                                   | JWT 남은 만료시간    | 로그아웃된 토큰 무효화     |

### 인증 흐름

```
[프론트 (Vue, :5173)]
       │
       ▼
① GET /api/v1/auth/kakao/login
   → 카카오 OAuth 인증 페이지로 리다이렉트 URL 반환
       │
       ▼
② GET /api/v1/auth/kakao/callback?code=xxx
   → KakaoAuthServiceImpl.loginWithKakao()
   │
   ├─ 카카오 인가코드로 Access Token 발급
   ├─ 카카오 사용자 정보 조회
   ├─ DB에 사용자 생성/조회
   ├─ sessionId(UUID) 생성
   ├─ JWT 토큰 생성 (sessionId만 포함, userId 미포함)
   ├─ Redis에 유저 세션 저장 (auth:session:{sessionId})
   └─ accessToken 쿠키 세팅 → 프론트로 리다이렉트
       │
       ▼
③ 이후 인증이 필요한 모든 요청
   → JwtAuthenticationFilter
   │
   ├─ 쿠키/헤더에서 JWT 추출
   ├─ JWT 서명 검증 + Redis 블랙리스트 체크
   ├─ JWT에서 sessionId 추출
   ├─ Redis에서 유저 세션 조회 (auth:session:{sessionId})
   └─ SecurityContext에 userId + UserSession 세팅
       │
       ▼
④ POST /api/v1/auth/logout
   → AuthController.logout()
   │
   ├─ JWT에서 sessionId 추출
   ├─ Redis 세션 삭제 (auth:session:{sessionId})
   ├─ 블랙리스트 등록 (auth:blacklist:{token}, 남은 TTL만큼)
   └─ accessToken 쿠키 삭제
```

### SecurityContext에서 유저 정보 접근

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String userId = (String) auth.getPrincipal();              // 사용자 ID
UserSession session = (UserSession) auth.getCredentials();  // 세션 정보 (nickname, email 등)
```

---

## 캐시 전략 (분리 운영)

| 저장소       | 용도               | 선택 이유                                                 |
| ------------ | ------------------ | --------------------------------------------------------- |
| **Redis**    | JWT 인증 세션 관리 | 서버 재시작 시에도 유지, 다중 서버 시 공유 가능           |
| **Caffeine** | 실시간 주가 캐시   | 네트워크 없이 나노초 조회, 초단위 빈번한 읽기/쓰기에 최적 |

### Caffeine 캐시 (StockPriceCacheStore)

- 최대 5,000 종목
- TTL: 6시간 (장 마감 후 자동 만료)
- Finnhub API에서 받은 주가 데이터를 메모리에 보관

---

## 환경 설정 (프로필 분리)

### 프로필별 설정

| 설정       | local (기본)     | prod                          |
| ---------- | ---------------- | ----------------------------- |
| DB         | 192.168.0.8:3306 | `${DB_HOST}:${DB_PORT}`       |
| Redis      | localhost:6379   | `${REDIS_HOST}:${REDIS_PORT}` |
| 프론트 URL | 192.168.0.8:5173 | rokstock.co.kr                |
| OAuth 콜백 | 192.168.0.8:8080 | rokstock.co.kr                |
| Swagger    | 활성화           | 비활성화                      |
| 로그 레벨  | DEBUG            | INFO                          |

### 실행 방법

```bash
# 로컬 (기본)
./gradlew :service:portal:bootRun

# 운영
./gradlew :service:portal:bootRun --args='--spring.profiles.active=prod'

# 또는 환경변수
SPRING_PROFILES_ACTIVE=prod java -jar portal.jar
```

### prod 환경변수

| 변수                                         | 설명            |
| -------------------------------------------- | --------------- |
| `DB_HOST`, `DB_PORT`                         | MySQL 접속 정보 |
| `DB_USERNAME`, `DB_PASSWORD`                 | MySQL 인증      |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis 접속 정보 |
| `JWT_SECRET`                                 | JWT 서명 키     |
| `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`     | 카카오 OAuth    |
| `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`     | 네이버 OAuth    |
| `FINNHUB_API_KEY`                            | 주가 API 키     |

---

## 서비스별 포트

| 서비스          | 포트 | 외부 노출       |
| --------------- | ---- | --------------- |
| api-gateway     | 8000 | O (단일 진입점) |
| portal          | 8080 | X (내부 전용)   |
| survey-service  | 8081 | X (내부 전용)   |
| stock-advisor   | 8082 | X (내부 전용)   |
| market-analyzer | 8083 | X (내부 전용)   |

---

## 주요 기술 스택

| 구분        | 기술                           |
| ----------- | ------------------------------ |
| Language    | Java 21                        |
| Framework   | Spring Boot 3.5.9              |
| Gateway     | Spring Cloud Gateway 2024.0.x  |
| Build       | Gradle (멀티모듈)              |
| ORM         | MyBatis 3.0.5                  |
| DB          | MySQL                          |
| Cache       | Redis (세션) + Caffeine (주가) |
| Auth        | JWT + 카카오 OAuth             |
| API Docs    | SpringDoc OpenAPI (Swagger)    |
| HTTP Client | Spring WebFlux (WebClient)     |
