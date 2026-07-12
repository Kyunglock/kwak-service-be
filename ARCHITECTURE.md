# kwak-service (inv-back) 아키텍처

> SP1 병합(2026-07): survey-service / market-analyzer / stock-advisor 를 portal(core)로 흡수해 4개 서비스 → 1개 앱으로 통합.
> SP2 추출(2026-07): AI 추론 기능을 별도 앱(`service/ai`, :8090)으로 분리.

## 프로젝트 구조

```
inv-back - 멀티모듈 Gradle, Java 21, Spring Boot 3.5.9
│
├── settings.gradle              루트 설정 (4개 모듈: common, api-gateway, portal, ai)
├── build.gradle                 공통 플러그인/의존성 관리 (Spring AI BOM 포함)
├── docker-compose.kafka.yml     로컬 Kafka (KRaft 단일 노드, :9092)
│
├── common/                      공통 라이브러리 (모든 앱이 의존)
│   ├── ai/AiGatewayClient       core → ai-app 추론 호출 클라이언트 (X-System-Key 인증)
│   ├── application/dto/         RokResponse<T>, PageResponse<T>
│   ├── application/event/       ActivityEvent (활동 로그 이벤트)
│   ├── config/security/         JwtAuthenticationFilter, JwtTokenProvider
│   ├── infrastructure/token/    RedisTokenStore, UserSession
│   └── util/                    ResponseUtil, ObjectUtil
│
├── service/api-gateway/         API 게이트웨이 (포트 9000) ← 단일 진입점
│   ├── Spring Cloud Gateway (WebFlux)
│   ├── 접두어 라우팅 (/portal /survey /advisor /market → 모두 core)
│   └── 글로벌 CORS
│
├── service/portal/              core 앱 (포트 8080) ← 모든 비즈니스 로직
│   │                            패키지 4개가 옛 서비스 경계를 유지한 채 공존
│   ├── com.investment.portal        인증(카카오/일반/게스트), 포트폴리오, 거래내역,
│   │                                실시간 주가(Finnhub+Caffeine), 인사이트(Kafka+AI),
│   │                                배당, 로깅(activity/menu/api)
│   ├── com.investment.survey        설문 CRUD/응답/통계, 공통코드, 크롤러 연동(System API)
│   ├── com.investment.stockadvisor  구루 포트폴리오/최근활동, 다이버전스, 투자성향 설문
│   └── com.investment.analyzer      시장 통계, 배당 히스토리, 뉴스, 스케줄러
│
└── service/ai/                  AI 추론 게이트웨이 앱 (포트 8090, 내부 전용)
    ├── kwakai/                  로컬 LLM(vLLM) 클라이언트 — gemma4-31b
    ├── openai/                  OpenAI 클라이언트
    └── config/                  X-System-Key 인증 (SystemKeyInterceptor)
```

## 시스템 구성도

```
                    ┌──────────────────────────┐
                    │    Client (Vue :5173)     │
                    └────────────┬─────────────┘
                                 │ :9000
                    ┌────────────▼─────────────┐
                    │        api-gateway        │  /portal /survey /advisor /market
                    │   (Spring Cloud Gateway)  │  → StripPrefix=1 → 모두 core로
                    └────────────┬─────────────┘
                                 │ :8080
      ┌──────────────────────────▼──────────────────────────┐
      │                     core (portal)                    │
      │  portal │ survey │ stockadvisor │ analyzer (패키지)  │
      └──┬──────────┬──────────┬──────────┬─────────────┬───┘
         │          │          │          │             │ :8090 (X-System-Key)
      MySQL      Redis      Kafka      Finnhub   ┌──────▼──────┐
      (주 DB)  (세션/캐시) (:9092,     (주가 API) │   ai-app     │
                           인사이트)              └──┬───────┬──┘
                                                    │       │
                                              vLLM(로컬 LLM)  OpenAI API
                                              gemma4-31b
```

- 외부에는 api-gateway(:9000)만 노출한다. core(:8080)와 ai(:8090)는 내부 전용.
- core → ai 호출은 common의 `AiGatewayClient`가 담당하며 `X-System-Key` 헤더로 인증한다.
- 옛 서비스 간 경로 접두어(/portal /survey /advisor /market)는 프론트 호환을 위해 게이트웨이에 유지하고, StripPrefix 후 `/api/v1/**` 형태로 core에 전달된다.

---

## API Gateway

### 목적

클라이언트가 하위 앱의 포트를 직접 알지 않아도 되도록 단일 진입점을 제공한다.
모든 요청은 `:9000`으로 들어와 경로 접두어에 따라 라우팅된다. SP1 병합 이후 모든 라우트의 목적지는 core 하나다.

### 라우팅 규칙 (local 기본)

| 클라이언트 요청 경로 | 필터        | 라우팅 대상               |
| -------------------- | ----------- | ------------------------- |
| `/portal/**`         | StripPrefix | core `${CORE_URI}` :8080  |
| `/survey/**`         | StripPrefix | core `${CORE_URI}` :8080  |
| `/advisor/**`        | StripPrefix | core `${CORE_URI}` :8080  |
| `/market/**`         | StripPrefix | core `${CORE_URI}` :8080  |

> ⚠️ `application-prod.yml`은 아직 병합 이전 라우트(`survey-service:8081`, `stock-advisor:8082`, `market-analyzer:8083`)를 참조한다. 운영 배포 전 core 단일 라우트로 정리 필요. (`/ai/**` → `${AI_URI}` 라우트는 SP2에서 추가 예정이었으나 현재 ai는 내부 호출 전용)

### CORS (글로벌)

```
허용 Origin : localhost:5173, 192.168.0.8:5173,
              kyungroak.iptime.org (http/https), kwaklabs.com, www.kwaklabs.com
허용 Method : GET, POST, PUT, DELETE, PATCH, OPTIONS
허용 Header : *
Credentials : true
```

하위 앱의 개별 CORS 설정은 불필요하다.

---

## Core (portal) — 절충 평탄화

SP1에서 4개 서비스를 하나의 Spring Boot 앱으로 병합하되, **패키지 경계는 옛 서비스 단위로 유지**했다 (도메인 식별 용이 + 추후 재분리 여지).

| 패키지                      | 옛 서비스       | 주요 기능                                                          |
| --------------------------- | --------------- | ------------------------------------------------------------------ |
| `com.investment.portal`     | portal          | 인증(카카오 OAuth·일반 로그인·게스트), 포트폴리오/아이템 CRUD, 거래내역, 실시간 주가, 배당, 인사이트, 활동/메뉴 로그 |
| `com.investment.survey`     | survey-service  | 설문 CRUD·응답·통계, 공통코드, SystemSurveyController(뉴스 크롤러 연동, System API Key 인증) |
| `com.investment.stockadvisor` | stock-advisor | 구루 포트폴리오·최근 활동, 다이버전스 감지, 투자성향 설문, 스케줄러 |
| `com.investment.analyzer`   | market-analyzer | 시장 통계, 배당 히스토리, 포트폴리오 배당, 뉴스, 스케줄러          |

### 레이어 규칙 (패키지 공통)

```
api/controller/     - HTTP 엔드포인트
application/
  dto/              - 요청/응답 DTO
  service/          - 비즈니스 로직
domain/
  entity/           - DB 엔티티
  repository/       - MyBatis 매퍼 인터페이스
infrastructure/     - 외부 연동 (Finnhub, Kafka 메시징 등)
config/             - Bean 설정, 예외 처리
```

### MyBatis 매퍼 주의

`mapper-locations: classpath*:mapper/**/*.xml` — `classpath*:`(별표)가 필수다.
병합 과정에서 매퍼 XML이 여러 classpath 루트에 존재할 수 있어, 단일 `classpath:`로는 일부 매퍼가 로딩되지 않는다.

---

## AI 앱 (service/ai)

core에서 AI 추론을 분리한 내부 전용 앱. LLM 벤더 교체/장애가 core에 전파되지 않도록 격리한다.

| 항목     | 내용                                                        |
| -------- | ----------------------------------------------------------- |
| 포트     | 8090 (외부 비노출)                                          |
| 인증     | `X-System-Key` 헤더 (SystemKeyInterceptor, `SYSTEM_API_KEY`) |
| 엔드포인트 | `POST /api/v1/ai/kwakai/generate` — 로컬 LLM 텍스트 생성  |
|          | `POST /api/v1/ai/openai/chat` — OpenAI 챗 (토큰 수 반환)    |
| 로컬 LLM | vLLM 서버 (`KWAKAI_BASE_URL`, 기본 192.168.0.16:8000/v1), 모델 `gemma4-31b` |
| OpenAI   | `OPENAI_API_KEY` (미설정 시 `dummy` — 부팅 실패 방지용 의도된 값) |

core 쪽 호출부는 common의 `AiGatewayClient` 하나로 통일되어 있다 (`ai.base-url` = `${AI_URI:http://localhost:8090}`).

---

## 인사이트 비동기 빌드 (Kafka)

투자자 인사이트(배당 인사이트 등)는 LLM 추론이 오래 걸리므로 Kafka로 비동기 처리한다.

```
① POST 인사이트 빌드 요청 (RagController)
   → Redis 락 획득 (insight:build:{userId} = PROCESSING, TTL 300s)
   → InsightBuildProducer.send() — topic: insight.build.requested (key = userId)
   → 즉시 202 응답

② InsightBuildConsumer (@KafkaListener, group: insight-builder, concurrency: 1)
   → InsightService.executeBuild(userId)
      ├─ 포트폴리오/배당 데이터 수집 (PortfolioStockInfoProvider 등)
      ├─ 프롬프트 구성 (CombinedInsightPromptBuilder)
      ├─ AiGatewayClient → ai-app → 로컬 LLM 추론
      └─ 결과 파싱(CombinedInsightParser) → tbl_insight_result 저장
   → 성공: insight:build:{userId} = DONE (TTL 60s) / 실패: FAILED

③ 프론트는 상태 폴링 → DONE이면 결과 조회
```

- 로컬 Kafka는 `docker-compose.kafka.yml`로 기동 (KRaft 단일 노드, 컨테이너명 `kwak-kafka`, `localhost:9092` advertise).
- 토픽은 `KafkaTopicConfig`에서 자동 생성.

---

## 인증 아키텍처

### 로그인 방식 3종 (portal)

| 방식   | 컨트롤러               | 비고                        |
| ------ | ---------------------- | --------------------------- |
| 카카오 | KakaoAuthController    | OAuth 인가코드 → 사용자 생성/조회 |
| 일반   | StandardAuthController | ID/PW (V2에서 password 컬럼 추가) |
| 게스트 | GuestAuthController    | 체험용 임시 계정            |

### JWT 토큰 구조

```
JWT Payload: { sub: "sessionId (UUID)", iat, exp }
Access Token  : 1시간  (jwt.expiration = 3600000)
Refresh Token : 7일    (jwt.refresh-expiration = 604800000)
```

- JWT에는 **sessionId(UUID)만 포함**, userId 등 유저 정보는 미포함
- 유저 정보는 Redis에서만 관리 → JWT 탈취 시에도 유저 식별 불가

### Redis 저장 구조

| Redis Key                       | Value                                        | TTL               | 용도                     |
| ------------------------------- | -------------------------------------------- | ----------------- | ------------------------ |
| `auth:session:{sessionId}`      | `{ userId, nickname, email, profileImgUrl }` | 액세스 토큰 만료  | 유저 세션                |
| `auth:refresh:{refreshTokenId}` | sessionId                                    | 리프레시 만료(7d) | 리프레시 토큰 → 세션 매핑 |
| `auth:blacklist:{token}`        | `"logout"`                                   | JWT 남은 만료시간 | 로그아웃 토큰 무효화     |
| `insight:build:{userId}`        | PROCESSING / DONE / FAILED                   | 300s / 60s        | 인사이트 빌드 상태·락    |

### 요청 인증 흐름

```
요청 → JwtAuthenticationFilter (common)
  ├─ 쿠키/헤더에서 JWT 추출
  ├─ 서명 검증 + Redis 블랙리스트 체크
  ├─ sessionId 추출 → Redis 세션 조회 (auth:session:{sessionId})
  └─ SecurityContext에 userId(principal) + UserSession(credentials) 세팅
```

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String userId = (String) auth.getPrincipal();
UserSession session = (UserSession) auth.getCredentials();
```

---

## 로깅 (V4~V6)

| 테이블           | 수집 방식                                        | 조회                                  |
| ---------------- | ------------------------------------------------ | ------------------------------------- |
| `tbl_activity_log` | `ActivityEvent`(common) 발행 → ActivityLogEventListener 비동기 저장 | ActivityLogController (관리자는 전체, `tbl_user.role`='ADMIN' → `ROLE_ADMIN` 기반 `@PreAuthorize`) |
| `tbl_menu_log`   | 프론트 메뉴 진입 시 MenuLogController 호출       | —                                     |
| `tbl_api_log`    | API 요청 로그                                    | —                                     |

---

## 캐시 전략 (분리 운영)

| 저장소       | 용도                                  | 선택 이유                                        |
| ------------ | ------------------------------------- | ------------------------------------------------ |
| **Redis**    | 인증 세션, 리프레시 토큰, 블랙리스트, 인사이트 빌드 상태 | 서버 재시작에도 유지, 다중 인스턴스 공유 가능 |
| **Caffeine** | 실시간 주가 캐시 (StockPriceCacheStore) | 네트워크 없이 나노초 조회, 초단위 읽기/쓰기에 최적 |

Caffeine 주가 캐시: 최대 5,000 종목, TTL 6시간 (Finnhub API 응답 보관).

---

## DB 마이그레이션

`service/portal/src/main/resources/db/migration/`의 SQL을 **수동 실행**한다 (Flyway 미사용).

| 버전 | 내용                          |
| ---- | ----------------------------- |
| V2   | tbl_user에 password 추가 (일반 로그인) |
| V3   | tbl_insight_result 생성       |
| V4   | tbl_activity_log 생성         |
| V5   | tbl_menu_log 생성             |
| V6   | tbl_api_log 생성              |

---

## 환경 설정

### 프로파일

| 앱          | 프로파일        | 비고                                     |
| ----------- | --------------- | ---------------------------------------- |
| portal      | `local`(기본) / `prod` | local: MyBatis SQL 로그, Swagger 활성화 |
| api-gateway | `local`(기본) / `prod` | prod 라우트는 정리 필요 (위 참고)       |
| ai          | 단일            | 환경변수로만 제어                        |

`.env.example`을 복사해 `.env`를 만들고 값을 채운다 (`.env`는 gitignore).

### 주요 환경변수

| 변수                                     | 사용 앱     | 설명                                      |
| ---------------------------------------- | ----------- | ----------------------------------------- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD`       | core        | MySQL                                     |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | core    | Redis (기본 localhost:6379)               |
| `KAFKA_BOOTSTRAP_SERVERS`                | core        | Kafka (기본 localhost:9092)               |
| `JWT_SECRET`                             | core, gateway | JWT 서명 키 (32자 이상)                 |
| `KAKAO_CLIENT_ID`, `KAKAO_SECRET`        | core        | 카카오 OAuth                              |
| `NAVER_CLIENT_ID`, `NAVER_SECRET`        | core        | 네이버 OAuth                              |
| `FINNHUB_API_KEY`                        | core        | 실시간 주가 API                           |
| `SYSTEM_API_KEY`                         | core, ai    | 내부 시스템 API 키 (core↔ai, 크롤러↔survey) |
| `AI_URI`                                 | core        | ai-app 주소 (기본 http://localhost:8090)  |
| `CORE_URI`                               | gateway     | core 주소 (기본 http://localhost:8080)    |
| `KWAKAI_BASE_URL`, `KWAKAI_MODEL`        | ai          | vLLM 서버 주소·모델 (기본 gemma4-31b)     |
| `OPENAI_API_KEY`                         | ai          | OpenAI (미설정 시 dummy)                  |
| `AUTH_COOKIE_SECURE`                     | core        | JWT 쿠키 Secure 플래그 (HTTPS 배포 시 true) |

### 실행

```bash
# 로컬 Kafka 기동 (인사이트 기능 사용 시 필수)
docker compose -f docker-compose.kafka.yml up -d

# core
./gradlew :service:portal:bootRun

# ai (인사이트/AI 기능 사용 시)
./gradlew :service:ai:bootRun

# gateway
./gradlew :service:api-gateway:bootRun

# 운영 프로파일
SPRING_PROFILES_ACTIVE=prod java -jar portal.jar
```

---

## 앱별 포트

| 앱          | 포트 | 외부 노출                       |
| ----------- | ---- | ------------------------------- |
| api-gateway | 9000 | O (단일 진입점)                 |
| core(portal) | 8080 | X (내부 전용)                  |
| ai          | 8090 | X (내부 전용, X-System-Key 인증) |
| Kafka       | 9092 | X (로컬 docker)                 |

---

## 주요 기술 스택

| 구분        | 기술                                       |
| ----------- | ------------------------------------------ |
| Language    | Java 21                                    |
| Framework   | Spring Boot 3.5.9                          |
| Gateway     | Spring Cloud Gateway (WebFlux)             |
| Build       | Gradle 멀티모듈 (common, api-gateway, portal, ai) |
| ORM         | MyBatis 3.0.5                              |
| DB          | MySQL                                      |
| Cache       | Redis (세션·상태) + Caffeine (주가)        |
| Messaging   | Kafka (KRaft, 인사이트 비동기 빌드)        |
| AI          | vLLM 로컬 LLM (gemma4-31b) + OpenAI, Spring AI BOM |
| Auth        | JWT (Access 1h + Refresh 7d) + 카카오/네이버 OAuth |
| Mapping     | MapStruct                                  |
| API Docs    | SpringDoc OpenAPI — `http://localhost:8080/swagger` |
| HTTP Client | Spring WebFlux (WebClient)                 |
