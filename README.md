# inv-back

투자 도우미 서비스 백엔드 모노레포입니다. Spring Boot 기반 멀티모듈 마이크로서비스 아키텍처로 구성되어 있습니다.

## 아키텍처

```
Client (Vue.js)
       │
       ▼ :9000
  api-gateway
  (Spring Cloud Gateway)
       │
   ┌───┼───────────┐
   ▼   ▼           ▼           ▼
:8080  :8081    :8082       :8083
portal survey  stock-advisor market-analyzer
```

모든 서비스는 `common` 공유 모듈에 의존합니다.

## 서비스 구성

| 서비스 | 포트 | 역할 |
|--------|------|------|
| api-gateway | 9000 | 라우팅, JWT 검증, CORS |
| portal | 8080 | 사용자 인증, 포트폴리오, 주가 조회 |
| survey-service | 8081 | 투자 성향 설문, 통계 |
| stock-advisor | 8082 | AI 기반 주식 조언, 다이버전스 분석 |
| market-analyzer | 8083 | 시장 통계, 배당 정보 |

## 기술 스택

- **Java 21** / **Spring Boot 3.5.9** / **Gradle** 멀티모듈
- **Spring Cloud Gateway** (WebFlux)
- **MyBatis** + **MySQL**
- **Redis** (캐시, JWT 블랙리스트)
- **Spring AI** + **OpenAI**
- **Kakao / Naver OAuth2**
- **Finnhub API** (실시간 주가)
- **Springdoc OpenAPI** (Swagger)

## 시작하기

### 사전 요구사항

- Java 21
- MySQL 8.x
- Redis 7.x

### 환경 설정

```bash
cp .env.example .env
```

`.env` 파일에 아래 값을 채워넣습니다:

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/invdb?...
DB_USER=
DB_PASSWORD=

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=   # 32자 이상

# OAuth
KAKAO_CLIENT_ID=
KAKAO_SECRET=
NAVER_CLIENT_ID=
NAVER_SECRET=
REDIRECT_URI=

# External APIs
OPENAI_API_KEY=
FINNHUB_API_KEY=
KWAKAI_BASE_URL=

# Internal
SYSTEM_API_KEY=   # 크롤러 연동 시스템 키
APP_FRONT_URL=
```

### 빌드 및 실행

```bash
# 전체 빌드
./gradlew build -x test

# 특정 서비스 실행 (예: portal)
./gradlew :service:portal:bootRun
```

## API 문서

각 서비스 실행 후 Swagger UI에서 확인할 수 있습니다.

- portal: `http://localhost:8080/swagger`
- survey-service: `http://localhost:8081/swagger`
- stock-advisor: `http://localhost:8082/swagger`
- market-analyzer: `http://localhost:8083/swagger`
