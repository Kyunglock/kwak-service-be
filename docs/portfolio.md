# kwak-service 포트폴리오

> Java 21 · Spring Boot 3.5.9 · Gradle 멀티모듈 · MyBatis · Redis · Caffeine · JWT · Kakao OAuth

---

## 목차

1. [기술 스택](#기술-스택)
2. [폴더 구조](#폴더-구조)
3. [아키텍처](#아키텍처)
4. [코드 컨벤션](#코드-컨벤션)
5. [데이터 배치 수집](#데이터-배치-수집)

---

## 기술 스택

| 분류 | 기술 | 비고 |
|------|------|------|
| Language | Java 21 | Virtual Threads 지원 버전 |
| Framework | Spring Boot 3.5.9 | 내장 Tomcat |
| Gateway | Spring Cloud Gateway 2024.0.x | WebFlux 기반 비동기 라우팅 |
| Build | Gradle 멀티모듈 | 6개 모듈(common + 5 서비스) |
| ORM | MyBatis 3.0.5 | XML 기반 SQL 매핑, JPA 미사용 |
| DB | MySQL | HikariCP 커넥션 풀 |
| 세션 캐시 | Redis + Lettuce | JWT 세션 저장 / 토큰 블랙리스트 |
| 실시간 캐시 | Caffeine 3.1.8 | 주가 인메모리 캐시 (최대 5,000종목) |
| 인증 | JWT (JJWT 0.12.3) + Kakao OAuth 2.0 | sessionId만 토큰에 포함 |
| HTTP Client | Spring WebClient (WebFlux) | 비동기 외부 API 호출 |
| 실시간 Push | SSE (Server-Sent Events) | 실시간 주가 브로드캐스트 |
| API 문서 | SpringDoc OpenAPI 2.7.0 | Swagger UI 자동 생성 |
| 컨테이너 | Docker + eclipse-temurin:21-jdk | 서비스별 독립 Dockerfile |

---

## 폴더 구조

```
kwak-service/                          ← Gradle 멀티모듈 루트
├── build.gradle                       ← 공통 플러그인 / 의존성 BOM 관리
├── settings.gradle                    ← 6개 모듈 include 선언
├── ARCHITECTURE.md                    ← 아키텍처 설계 문서
├── Dockerfile.gateway / .portal / ... ← 서비스별 Docker 이미지 빌드
│
├── common/                            ← 공통 라이브러리 (java-library)
│   └── src/main/java/kwak/common/
│       ├── application/dto/           ← RokResponse<T>, PageResponse<T>
│       ├── config/                    ← Redis / Security / Web 공통 설정
│       ├── config/security/           ← JwtTokenProvider, JwtAuthenticationFilter
│       ├── exception/                 ← AuthenticationException
│       ├── infrastructure/token/      ← RedisTokenStore, UserSession
│       └── util/                      ← ResponseUtil, ObjectUtil
│
└── service/
    ├── api-gateway/                   ← 단일 진입점 (포트 8000)
    │   └── src/main/resources/        ← 라우팅 규칙 / CORS 글로벌 설정
    │
    ├── portal/                        ← 핵심 서비스 (포트 8080)
    │   └── src/main/java/com/investment/portal/
    │       ├── api/controller/        ← REST 엔드포인트
    │       │   ├── login/             ← OAuth, 표준 로그인
    │       │   ├── portfolio/         ← 포트폴리오 / 포트폴리오 종목
    │       │   ├── history/           ← 거래내역
    │       │   └── stock/             ← 주가 조회 / SSE
    │       ├── application/
    │       │   ├── dto/               ← Request / Response DTO
    │       │   └── service/           ← 비즈니스 로직 (인터페이스 + 구현체)
    │       ├── domain/
    │       │   ├── entity/            ← 순수 POJO 엔티티 (JPA 어노테이션 없음)
    │       │   ├── enums/             ← SocialProvider 등 열거형
    │       │   └── repository/        ← MyBatis @Mapper 인터페이스
    │       ├── infrastructure/
    │       │   ├── cache/             ← StockPriceCacheStore (Caffeine)
    │       │   ├── scheduler/         ← 주가 배치 스케줄러 2종
    │       │   └── sse/               ← StockPriceSseEmitterService
    │       └── config/                ← Scheduling / Security / WebClient 설정
    │
    ├── survey-service/                ← 설문조사 서비스 (포트 8081)
    ├── stock-advisor/                 ← 투자 조언 서비스 (포트 8082)
    └── market-analyzer/               ← 시장 통계 서비스 (포트 8083)
```

---

## 아키텍처

### 전체 구성 — 마이크로서비스 + API 게이트웨이

```
[Browser / Vue :5173]
        │ :8000 (외부 단일 진입점)
┌───────▼──────────────────────────┐
│          API Gateway              │  Spring Cloud Gateway (WebFlux)
│    경로 기반 라우팅 / 글로벌 CORS  │
└───┬──────────┬──────────┬────────┘
    │          │          │          │
 :8080      :8081      :8082      :8083
portal   survey    advisor    market
```

- 외부에는 **api-gateway(:8000)만 노출**, 하위 서비스 포트는 내부 네트워크에서만 접근
- 서비스 간 직접 호출 없음 — 각 서비스는 독립적으로 실행 가능
- CORS는 게이트웨이에서 일괄 처리

### API Gateway 라우팅 규칙

| 경로 | 라우팅 대상 |
|------|------------|
| `/api/v1/auth/**` | portal :8080 |
| `/api/v1/portfolios/**` | portal :8080 |
| `/api/v1/stocks/**` | portal :8080 |
| `/api/v1/surveys/**` | survey-service :8081 |
| `/api/v1/guru/**` | stock-advisor :8082 |
| `/api/v1/markets/**` | market-analyzer :8083 |

### 서비스 내부 레이어 구조 — 계층형 아키텍처

```
Controller Layer        ← HTTP 요청 수신, 파라미터 바인딩, @Valid
        ↓
Service Layer           ← 비즈니스 로직, DTO ↔ Entity 변환
        ↓
Domain Layer            ← Entity, Enum, Repository 인터페이스
        ↓
Infrastructure Layer    ← MyBatis Mapper, Caffeine Cache, Scheduler, SSE
        ↓
Data Layer              ← MySQL (MyBatis XML 쿼리)
```

### 인증 아키텍처 — JWT + Redis 세션 분리

```
JWT Payload: { sub: "sessionId (UUID)" }   ← userId 미포함 (탈취 피해 최소화)
Redis Key  : auth:session:{sessionId}       ← { userId, nickname, email, profileImgUrl }
Redis Key  : auth:blacklist:{token}         ← 로그아웃 토큰 블랙리스트
```

**흐름:**
1. 카카오 인가코드 → Access Token 교환 → 사용자 정보 조회
2. DB 사용자 생성/조회 → `sessionId(UUID)` 생성
3. JWT 발급 (sessionId만 포함) → Redis 세션 저장 → 쿠키 세팅
4. 이후 요청: `JwtAuthenticationFilter` → JWT 서명 검증 + 블랙리스트 체크 → Redis 세션 조회 → SecurityContext 주입

### 캐시 전략 — 이중 캐시

| 저장소 | 용도 | 선택 이유 |
|--------|------|----------|
| **Redis** | JWT 세션, 토큰 블랙리스트 | 서버 재시작 후에도 유지, 다중 인스턴스 공유 |
| **Caffeine** | 실시간 주가 스냅샷 | 나노초 조회, 네트워크 없이 초단위 빈번한 읽기 최적화 |

---

## 코드 컨벤션

### 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `PortfolioController`, `KakaoAuthServiceImpl` |
| 메서드 / 변수 | camelCase | `getMyPortfolios()`, `sessionId` |
| 상수 | UPPER_SNAKE_CASE | `SESSION_PREFIX = "auth:session:"` |
| DB 테이블 | snake_case (소문자) | `tbl_stock_price_history`, `tbl_portfolio` |
| DTO 접미사 | `Request` / `Response` | `PortfolioAddRequest`, `PortfolioResponse` |
| 서비스 쌍 | `XxxService` + `XxxServiceImpl` | `PortfolioService` / `PortfolioServiceImpl` |

### 레이어별 클래스 구성

```java
// Controller — 파라미터 수신, 인증 주체 추출
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {
    @GetMapping
    public ResponseEntity<?> getMyPortfolios(@AuthenticationPrincipal String userId) { ... }
}

// Service — 인터페이스 + 구현체 분리
public interface PortfolioService {
    List<PortfolioResponse> getMyPortfolios(String userId);
}

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService { ... }

// Mapper — @Mapper 인터페이스, XML 쿼리 별도 관리
@Mapper
public interface PortfolioMapper {
    List<Portfolio> findByUserId(@Param("userId") String userId);
}
```

### 공통 응답 포맷

모든 API는 `common` 모듈의 `RokResponse<T>`로 통일합니다.

```java
// 성공
return ResponseUtil.success(data, "조회 성공");

// 실패
return ResponseUtil.error("에러 메시지");
```

```json
{
  "success": true,
  "message": "조회 성공",
  "data": { ... },
  "timestamp": "2025-01-01T00:00:00"
}
```

### Lombok 활용

- **엔티티 / DTO**: `@Builder`, `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor`
- **서비스 / 컴포넌트**: `@RequiredArgsConstructor` (생성자 주입 자동화)
- **로깅**: `@Slf4j` → `log.info()`, `log.error()`, `log.debug()`

### MyBatis XML 쿼리 컨벤션

- SQL 키워드 대문자, 컬럼 정렬은 `,` 앞에 배치
- `map-underscore-to-camel-case: true` — DB `snake_case` ↔ Java `camelCase` 자동 매핑
- 다중 조건 쿼리는 `<if>`, 반복 쿼리는 `<foreach>` 사용

```xml
<select id="findLatestByStockCodes" resultType="StockPriceHistory">
    SELECT h.stock_cd
         , h.close_price
      FROM tbl_stock_price_history h
     INNER JOIN (
         SELECT stock_cd, MAX(price_dt) AS max_price_dt
           FROM tbl_stock_price_history
          WHERE stock_cd IN
            <foreach collection="stockCodes" item="stockCd" open="(" separator="," close=")">
                #{stockCd}
            </foreach>
          GROUP BY stock_cd
     ) latest ON h.stock_cd = latest.stock_cd
             AND h.price_dt = latest.max_price_dt
</select>
```

### 환경 프로파일 분리

| 프로파일 | DB | Redis | OAuth | Swagger |
|----------|----|-------|-------|---------|
| `local` | 192.168.0.8:3306 | localhost:6379 | 하드코딩 | 활성화 |
| `prod` | 환경변수 `${DB_HOST}` | 환경변수 `${REDIS_HOST}` | 환경변수 | 비활성화 |

---

## 데이터 배치 수집

S&P 500 종목 주가를 외부 API(Finnhub)로부터 수집하여 **인메모리 캐시 → DB 영속화** 파이프라인으로 관리합니다.

### 전체 파이프라인

```
[Finnhub API]
      │ 실시간 주가 응답
      ▼
[StockPriceFetchScheduler]  ← 2분마다 실행
      │ DB에서 최신 종가 로드 → Caffeine 캐시 갱신
      ▼
[StockPriceCacheStore]      ← Caffeine (최대 5,000종목, TTL 6시간)
      │
      ├─→ [StockPriceSseEmitterService]  ← 구독 중인 클라이언트에 SSE Push
      │
      ▼ (장 종료 시)
[StockPriceSnapshotScheduler]  ← 매일 06:00 KST (화~토)
      │ 캐시 전체 → DB 배치 저장
      ▼
[tbl_stock_price_history]   ← MySQL 영속 이력
```

### StockPriceFetchScheduler — 실시간 캐시 갱신

```java
@Scheduled(fixedRate = 120_000) // 2분 주기
public void fetchAndBroadcast() {
    List<String> stockCodes = stockListProvider.getStockCodes();   // S&P 500 종목 목록
    List<StockPriceSnapshot> snapshots = queryService.getPricesFromDb(stockCodes); // DB 최신 종가

    snapshots.forEach(s -> cacheStore.put(s.getStockCd(), s));    // 캐시 갱신
    sseService.broadcastAll(snapshots);                            // SSE 전파
}
```

- **주기**: 2분 (`fixedRate = 120_000`)
- **대상**: S&P 500 전 종목
- **목적**: DB에서 최신 종가를 읽어 Caffeine 캐시를 최신 상태로 유지하고 SSE로 실시간 전달

### StockPriceSnapshotScheduler — 종가 영속화

```java
@Scheduled(cron = "0 0 6 * * TUE-SAT", zone = "Asia/Seoul")  // 06:00 KST = 전날 16:00 ET (미국 장 종료)
public void saveClosingSnapshot() {
    ConcurrentMap<String, StockPriceSnapshot> allPrices = cacheStore.getAllAsMap();

    List<StockPriceHistory> histories = allPrices.values().stream()
            .map(this::toStockPriceHistory)
            .toList();

    stockPriceHistoryMapper.batchInsert(histories);  // MyBatis XML batchInsert
    cacheStore.clear();                              // 캐시 초기화
}
```

- **주기**: 매일 06:00 KST (화~토) = 미국 동부시간 전날 16:00 (장 종료 시점)
- **처리**: 캐시 전체 스냅샷을 `batchInsert`로 DB에 한 번에 저장
- **이후**: 캐시 초기화 → 다음 장 시작까지 빈 상태 유지

### batchInsert — MyBatis `<foreach>` 배치 쿼리

단일 `INSERT ... VALUES (),(),()`로 전 종목을 한 번에 저장합니다. 루프 INSERT 대비 DB 왕복 횟수를 최소화합니다.

```xml
<insert id="batchInsert">
    INSERT INTO tbl_stock_price_history (stock_cd, price_dt, open_price, ..., reg_dt)
    VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.stockCd}, #{item.priceDt}, #{item.openPrice}, ..., NOW())
    </foreach>
</insert>
```

### Caffeine 캐시 설정

| 항목 | 값 |
|------|----|
| 최대 항목 수 | 5,000종목 |
| TTL | 6시간 (장 마감 후 자동 만료) |
| 자료구조 | `Cache<String, StockPriceSnapshot>` (종목코드 → 스냅샷) |

### 데이터 흐름 요약

| 단계 | 시점 | 처리 |
|------|------|------|
| 실시간 수집 | 매 2분 | DB 최신 종가 조회 → 캐시 갱신 → SSE 브로드캐스트 |
| 종가 저장 | 06:00 KST (장 종료) | 캐시 전체 → DB batchInsert → 캐시 초기화 |
| 이력 조회 | 사용자 요청 시 | Caffeine 캐시 우선 조회 → 캐시 미스 시 DB 조회 |
