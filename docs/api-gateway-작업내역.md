# API Gateway 추가 작업내역

> 작업일: 2026-04-05
> 브랜치: `claude/add-spring-gateway-qyDAn`

---

## 작업 목적

각 마이크로서비스의 포트 번호를 외부에 직접 노출하지 않고, **단일 진입점(9000 포트)**을 통해 모든 요청을 라우팅하기 위해 Spring Cloud Gateway를 추가함.

---

## 추가된 파일

```
kwak-service/
├── Dockerfile.gateway                                         # Gateway Docker 빌드
├── service/api-gateway/
│   ├── build.gradle                                           # Spring Cloud Gateway 의존성
│   └── src/main/
│       ├── java/com/investment/gateway/
│       │   └── ApiGatewayApplication.java                     # 메인 클래스
│       └── resources/
│           ├── application.yml                                # 라우팅 + CORS 설정
│           ├── application-local.yml                          # 로컬 환경 URI
│           └── application-prod.yml                           # 운영(Docker) 환경 URI
```

---

## 서비스 구조

```
Client
  │
  ▼ :9000
api-gateway  ← 단일 진입점 (외부 노출)
  │
  ├──▶ portal        :8080  (내부 전용)
  ├──▶ survey        :8081  (내부 전용)
  ├──▶ stock-advisor :8082  (내부 전용)
  └──▶ market-analyzer :8083 (내부 전용)
```

---

## 라우팅 규칙

| 요청 경로 | 라우팅 서비스 |
|---|---|
| `/api/v1/auth/**` | portal :8080 |
| `/api/v1/portfolios/**` | portal :8080 |
| `/api/v1/portfolio-items/**` | portal :8080 |
| `/api/v1/transactions/**` | portal :8080 |
| `/api/v1/stocks/**` | portal :8080 |
| `/api/v1/codes/**` | survey :8081 |
| `/api/v1/surveys/**` | survey :8081 |
| `/api/v1/surveys-stats/**` | survey :8081 |
| `/api/v1/guru/**` | stock-advisor :8082 |
| `/api/v1/users/**` | stock-advisor :8082 |
| `/api/v1/markets/**` | market-analyzer :8083 |

---

## docker-compose.yml 추가 내용

> `docker-compose.yml`은 git으로 관리하지 않으므로 직접 추가 필요

```yaml
api-gateway:
  build:
    context: ./kwak-service
    dockerfile: Dockerfile.gateway
  ports:
    - "9000:9000"
  depends_on:
    - portal
    - survey
    - stock-advisor
  environment:
    - SPRING_PROFILES_ACTIVE=prod
```

---

## 트러블슈팅 기록

### 1. Spring Cloud 버전 호환성 오류

**증상**
```
Change Spring Boot version to one of the following versions [3.4.x]
```

**원인**
Spring Cloud `2024.0.1`은 Spring Boot 3.4.x까지만 지원. 프로젝트는 Spring Boot 3.5.9 사용 중.

**해결**
`build.gradle`의 Spring Cloud 버전을 `2025.0.0`으로 변경.

```groovy
// 변경 전
set('springCloudVersion', "2024.0.1")

// 변경 후
set('springCloudVersion', "2025.0.0")
```

---

### 2. application.yml 설정 키 경고 (Deprecated)

**증상**
```
Key: spring.cloud.gateway.globalcors...
Replacement: spring.cloud.gateway.server.webflux.globalcors...
```

**원인**
Spring Cloud Gateway 2025.0.0부터 WebFlux / WebMVC 이중 지원으로 네임스페이스가 변경됨.

**해결**
`application.yml` 키 경로 일괄 변경.

```yaml
# 변경 전
spring.cloud.gateway.globalcors...
spring.cloud.gateway.routes...

# 변경 후
spring.cloud.gateway.server.webflux.globalcors...
spring.cloud.gateway.server.webflux.routes...
```

---

### 3. 포트 충돌

**증상**
8000 포트를 다른 프로세스가 이미 사용 중.

**해결**
Gateway 포트를 `8000` → `9000`으로 변경.
- `application.yml` `server.port: 9000`
- `Dockerfile.gateway` `EXPOSE 9000`
- `docker-compose.yml` 포트 매핑 `9000:9000`

---

### 4. CORS 에러 (Preflight canceled)

**증상**
브라우저 네트워크 탭에서 모든 요청이 `CORS error`로 실패.

**원인 1.** CORS 허용 Origin 목록에 실제 운영 도메인(`kyungroak.iptime.org`)이 없었음.

**해결 1.** `application.yml`에 운영 도메인 추가.

```yaml
allowedOrigins:
  - "http://localhost:5173"
  - "http://192.168.0.8:5173"
  - "http://kyungroak.iptime.org"   # 추가
```

**원인 2.** Gateway와 하위 서비스(portal 등) 양쪽에서 CORS 헤더를 동시에 추가해 브라우저가 중복 헤더를 거부.

**해결 2.** `DedupeResponseHeader` default 필터 추가.

```yaml
default-filters:
  - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials
```

---

### 5. docker-compose 서비스명 불일치

**증상**
Gateway가 `survey-service` 컨테이너를 찾지 못함.

**원인**
`application-prod.yml`에 `http://survey-service:8081`로 설정했으나 실제 docker-compose 서비스명은 `survey`.

**해결**
`application-prod.yml` 수정.

```yaml
# 변경 전
SURVEY_URI: http://survey-service:8081

# 변경 후
SURVEY_URI: http://survey:8081
```

---

### 6. stock-advisor 기동 실패 (Docker에서만 발생)

**증상**
```
Could not resolve type alias 'GuruPortfolio'
java.lang.ClassNotFoundException: Cannot find class: GuruPortfolio
```

**원인**
`application.yml`의 MyBatis `type-aliases-package`가 `com.investment.stockAdvisor` (대문자 A)로 설정되어 있었음.
Windows는 파일시스템이 대소문자 구분 없어 로컬에서 정상 동작했으나, Docker(Linux)는 대소문자를 엄격히 구분해 클래스를 찾지 못함.

**해결**
`application.yml` 패키지명 수정.

```yaml
# 변경 전
type-aliases-package: com.investment.stockAdvisor

# 변경 후
type-aliases-package: com.investment.stockadvisor
```

---

## 수정된 기존 파일 목록

| 파일 | 수정 내용 |
|---|---|
| `settings.gradle` | `service:api-gateway` 모듈 추가 |
| `ARCHITECTURE.md` | Gateway 섹션 및 서비스 구조 다이어그램 추가 |
| `service/portal/src/main/resources/application.yml` | 프론트 URL IP 변경 |
| `service/portal/src/main/resources/application-local.yml` | DB URL, OAuth redirect URI, 프론트 URL IP 변경 |
| `service/survey-service/src/main/resources/application.yml` | 프론트 URL IP 변경 |
| `service/survey-service/src/main/resources/application-local.yml` | DB URL, 프론트 URL IP 변경 |
| `service/stock-advisor/src/main/resources/application.yml` | MyBatis 패키지명 대소문자 수정, 프론트 URL IP 변경 |
| `service/stock-advisor/src/main/resources/application-local.yml` | DB URL, 프론트 URL IP 변경 |
| `service/market-analyzer/src/main/resources/application-local.yml` | DB URL IP 변경 |
| `service/portal/src/main/java/.../GlobalExceptionHandler.java` | `@Value` 기본값 IP 변경 |
| `service/portal/src/main/java/.../CustomErrorController.java` | `@Value` 기본값 IP 변경 |
