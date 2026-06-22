# CLAUDE.md

## 프로젝트 개요

`inv-back`는 투자 도우미 서비스의 백엔드 모노레포입니다. Spring Boot 기반 멀티모듈 마이크로서비스 구조로 구성되어 있습니다.

## 모듈 구조

```
inv-back/
├── common/                  # 공유 라이브러리 (JWT, Security, 공통 DTO)
└── service/
    ├── api-gateway/         # Spring Cloud Gateway, 포트 9000
    ├── portal/              # 메인 서비스 (인증, 포트폴리오, 주가), 포트 8080
    ├── survey-service/      # 설문 관리, 포트 8081
    ├── stock-advisor/       # AI 기반 주식 조언, 포트 8082
    └── market-analyzer/     # 시장 분석 및 배당 정보, 포트 8083
```

## 빌드 및 실행

```bash
# 전체 빌드
./gradlew build -x test

# 특정 서비스 빌드
./gradlew :service:portal:bootJar -x test

# 특정 서비스 실행
./gradlew :service:portal:bootRun
```

## 환경 설정

`.env.example`을 복사해 `.env`를 만들고 값을 채워야 합니다. `.env`는 gitignore에 포함되어 있습니다.

필수 환경변수:
- `DB_URL`, `DB_USER`, `DB_PASSWORD` — MySQL
- `REDIS_HOST`, `REDIS_PORT` — Redis
- `JWT_SECRET` — 32자 이상
- `KAKAO_CLIENT_ID`, `KAKAO_SECRET` — 카카오 OAuth
- `NAVER_CLIENT_ID`, `NAVER_SECRET` — 네이버 OAuth
- `KWAKAI_BASE_URL` — 내부 AI 서버 주소
- `SYSTEM_API_KEY` — survey-service 내부 시스템 API 인증키 (크롤러 연동)
- `OPENAI_API_KEY` — OpenAI (portal, stock-advisor)
- `FINNHUB_API_KEY` — 실시간 주가 (portal)

## 패키지 레이어 규칙

각 서비스는 동일한 레이어 구조를 따릅니다:

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

## 공통 모듈 (common)

모든 서비스가 의존하는 공유 라이브러리입니다. 주요 구성요소:

- `RokResponse<T>` — 통일된 API 응답 포맷
- `ResponseUtil` — HTTP 응답 헬퍼
- JWT 토큰 처리 인프라
- Security 기본 설정

## 주요 기술

- **Java 21**, **Spring Boot 3.5.9**, **Gradle** 멀티모듈
- **Spring Cloud Gateway** (WebFlux) — API Gateway
- **MyBatis** — ORM
- **MySQL** — 주 데이터베이스
- **Redis** — 캐시, JWT 블랙리스트
- **Spring AI + OpenAI** — AI 기능
- **MapStruct** — DTO 매핑
- **Springdoc OpenAPI** — Swagger UI (`/swagger`)

## API 문서

각 서비스 실행 후 접근 가능:
- portal: `http://localhost:8080/swagger`
- survey: `http://localhost:8081/swagger`
- stock-advisor: `http://localhost:8082/swagger`
- market-analyzer: `http://localhost:8083/swagger`

## 주의사항

- `common` 모듈 변경 시 모든 서비스에 영향을 줍니다.
- api-gateway는 WebFlux 기반이므로 다른 서비스(MVC)와 설정 방식이 다릅니다.
- `market-analyzer`의 `api-key: dummy`는 Spring AI 자동설정 부팅 실패 방지용 의도된 더미값입니다.
- `dump-invdb.sql`은 gitignore에 포함되어 있습니다.
