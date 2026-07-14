# CLAUDE.md

## 프로젝트 개요

`inv-back`는 투자 도우미 서비스의 백엔드 모노레포입니다. Spring Boot 기반 Gradle 멀티모듈 구조이며, 과거 5개 마이크로서비스였던 것이 현재는 core(portal) 중심으로 통합되었습니다.

- **SP1 병합**: survey-service, market-analyzer, stock-advisor 모듈은 portal로 흡수됨 (모듈 제거, 패키지로 존속)
- **SP2 추출**: AI 추론 기능은 별도 `service:ai` 모듈(:8090)로 분리됨

## 모듈 구조

```
inv-back/
├── common/                  # 공유 라이브러리 (kwak.common — JWT, Security, 공통 DTO, 예외)
└── service/
    ├── api-gateway/         # Spring Cloud Gateway (WebFlux), 포트 9000
    ├── portal/              # core 서비스 — 아래 4개 도메인 패키지 통합, 포트 8080
    └── ai/                  # AI 추론 서버 (kwakai 로컬 LLM + OpenAI 프록시), 포트 8090
```

### portal 내 도메인 패키지 (`com.investment.*`)

```
portal/          # 인증(카카오/네이버/게스트), 유저/닉네임, 포트폴리오, 거래내역,
                 # 실시간 주가(Finnhub), 배당, 종목운세(Fortune), RAG, 활동로그
survey/          # 설문/MBTI (질문 코드, 응답, 통계, 뉴스 크롤러용 시스템 API)
analyzer/        # market_analyzer — 시장 통계, 뉴스, 배당 이력, 스케줄러
stockadvisor/    # 괴리율(Divergence), 구루 포트폴리오/최근활동, 유저 설문 연계
```

### 라우팅 (api-gateway)

`/portal /survey /advisor /market` 접두어는 운영 프론트 호환을 위해 유지되며, 전부 `StripPrefix=1` 후 core(portal, `CORE_URI`)로 전달됩니다. ai(:8090)는 게이트웨이 라우트가 없고 core가 `X-System-Key` 헤더로 내부 호출만 합니다.

## 빌드 및 실행

```bash
# 전체 빌드
./gradlew build -x test

# 특정 모듈 빌드
./gradlew :service:portal:bootJar -x test

# 특정 모듈 실행
./gradlew :service:portal:bootRun
./gradlew :service:ai:bootRun
./gradlew :service:api-gateway:bootRun
```

## 환경 설정

`.env.example`을 복사해 `.env`를 만들고 값을 채워야 합니다. `.env`는 gitignore에 포함되어 있습니다.

주요 환경변수:
- `DB_URL`, `DB_USER`, `DB_PASSWORD` — MySQL
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` — Redis
- `KAFKA_BOOTSTRAP_SERVERS` — Kafka (insight-builder 컨슈머)
- `JWT_SECRET` — 32자 이상
- `KAKAO_CLIENT_ID`, `KAKAO_SECRET`, `NAVER_CLIENT_ID`, `NAVER_SECRET` — OAuth
- `CORE_URI` — api-gateway → core(portal) 주소
- `AI_URI` — core → ai 모듈 주소
- `SYSTEM_API_KEY` — core ↔ ai 내부 인증(X-System-Key) 및 뉴스 크롤러 연동 키
- `KWAKAI_BASE_URL`, `KWAKAI_MODEL` — ai 모듈의 로컬 LLM
- `OPENAI_API_KEY`, `OPENAI_BASE_URL` — OpenAI (ai 모듈)
- `FINNHUB_API_KEY` — 실시간 주가 (portal)
- `AUTH_COOKIE_SECURE` — JWT 쿠키 Secure 플래그 (로컬 false, HTTPS 배포 시 true)

## 패키지 레이어 규칙

각 도메인 패키지는 동일한 레이어 구조를 따릅니다:

```
api/controller/     - HTTP 엔드포인트
application/
  dto/              - 요청/응답 DTO
  service/          - 비즈니스 로직
domain/
  entity/           - DB 엔티티
  repository/       - MyBatis 매퍼 인터페이스
infrastructure/     - 외부 연동 (OpenAI, Finnhub 등)
config/             - Bean 설정
```

## 공통 모듈 (common, `kwak.common`)

모든 서비스가 의존하는 공유 라이브러리입니다. 주요 구성요소:

- `RokResponse<T>` — 통일된 API 응답 포맷
- `ResponseUtil` — HTTP 응답 헬퍼
- JWT 토큰 처리 인프라 (`infrastructure/token`)
- Security 기본 설정 (`config/security`)
- 공통 예외, 이벤트 DTO

## 주요 기술

- **Java 21**, **Spring Boot 3.5.9**, **Gradle** 멀티모듈
- **Spring Cloud Gateway** (WebFlux) — API Gateway
- **MyBatis** — ORM (매퍼 XML은 `classpath*:mapper/**/*.xml`)
- **MySQL** — 주 데이터베이스 (Flyway 마이그레이션 `db/migration/V*.sql`)
- **Redis** — 캐시, JWT 블랙리스트
- **Kafka** — 이벤트 파이프라인 (insight-builder)
- **MapStruct** — DTO 매핑
- **Springdoc OpenAPI** — Swagger UI (`/swagger`)

## API 문서

- portal(core): `http://localhost:8080/swagger`

## 주의사항

- `common` 모듈 변경 시 모든 서비스에 영향을 줍니다.
- api-gateway는 WebFlux 기반이므로 다른 모듈(MVC)과 설정 방식이 다릅니다.
- MyBatis `mapper-locations`는 반드시 `classpath*:` (별표)를 유지해야 합니다 — 병합된 구조에서 라이브러리 모듈의 매퍼 XML까지 스캔하기 위함입니다.
- ai 모듈의 `OPENAI_API_KEY` 기본값 `dummy`는 부팅 실패 방지용 의도된 더미값입니다.
- `dump-invdb.sql`은 gitignore에 포함되어 있습니다.
