# 인사이트 결과보기 개발 흐름 문서

## 개요

설문 결과(build-context)와 포트폴리오 보유 종목 데이터를 기반으로 RAG 컨텍스트를 재구성하여,  
`inv-fe` 인사이트 메뉴에 타입별 AI 분석 결과를 표시하는 기능.

---

## 전체 아키텍처

```
inv-fe (React + TypeScript)
  └─ InsightsDashboard
       ├─ KeyInsightsCard        ← KEY_FINDINGS
       ├─ MyProfileCard          ← INVESTMENT_STYLE (+ 설문 점수)
       └─ InsightResultCard      ← RISK_ASSESSMENT
                                    PORTFOLIO_ALIGNMENT
                                    INVESTMENT_RECOMMENDATION
                                    + [결과보기] 버튼

         ↕ REST API (port 8080)

kwak-service / portal (Spring Boot 3)
  └─ RagController
       ├─ GET  /api/v1/insights/results              전체 조회
       ├─ GET  /api/v1/insights/results/{typeCd}     타입별 단건 조회
       └─ POST /api/v1/insights/build-context        컨텍스트 빌드 & 저장
            │
            ├─ PortfolioMapper      사용자 포트폴리오 목록
            ├─ PortfolioItemMapper  보유 종목(ticker) 추출
            ├─ YahooFinanceClient   종목별 기업정보 조회 (무료, 키 없음)
            └─ InsightResultMapper  tbl_insight_result upsert
```

---

## DB 스키마

### `tbl_insight_result`

```sql
CREATE TABLE tbl_insight_result (
    result_id       BIGINT       AUTO_INCREMENT PRIMARY KEY  COMMENT '인사이트 결과 ID',
    user_id         VARCHAR(100) NOT NULL                    COMMENT '사용자 ID',
    result_type_cd  VARCHAR(50)  NOT NULL                    COMMENT '인사이트 유형 코드',
    title           VARCHAR(200) NOT NULL                    COMMENT '섹션 제목',
    content         TEXT         NOT NULL                    COMMENT '인사이트 내용',
    use_yn          CHAR(1)      NOT NULL DEFAULT 'Y'        COMMENT '사용여부',
    reg_dt          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upd_dt          DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_insight_user_type (user_id, result_type_cd),
    INDEX      idx_insight_user_id  (user_id)
);
```

### `result_type_cd` 목록

| 코드 | 제목 | 표시 위치 |
|------|------|-----------|
| `KEY_FINDINGS` | 주요 발견사항 | `KeyInsightsCard` |
| `INVESTMENT_STYLE` | 나의 투자성향 | `MyProfileCard` (AI 분석 섹션) |
| `RISK_ASSESSMENT` | 리스크 평가 | `InsightResultCard` |
| `PORTFOLIO_ALIGNMENT` | 포트폴리오 정합성 | `InsightResultCard` |
| `INVESTMENT_RECOMMENDATION` | 투자 추천 | `InsightResultCard` |

> `(user_id, result_type_cd)` Unique Key → 사용자당 타입별 1행 유지,  
> `INSERT ... ON DUPLICATE KEY UPDATE` 로 upsert 처리.

---

## 백엔드 (kwak-service / portal 서비스)

### 추가된 파일

```
service/portal/src/main/
├─ java/com/investment/portal/
│   ├─ api/controller/insight/
│   │   └─ RagController.java                  ← REST 컨트롤러
│   ├─ application/
│   │   ├─ dto/insight/
│   │   │   └─ InsightResultResponse.java      ← 응답 DTO (record)
│   │   └─ service/insight/
│   │       ├─ InsightService.java             ← 인터페이스
│   │       └─ InsightServiceImpl.java         ← 구현체 (핵심 로직)
│   ├─ domain/
│   │   ├─ entity/insight/
│   │   │   └─ InsightResult.java             ← DB 엔티티
│   │   └─ repository/insight/
│   │       └─ InsightResultMapper.java       ← MyBatis 매퍼 인터페이스
│   └─ infrastructure/external/yahoo/
│       ├─ StockInfo.java                     ← Yahoo Finance 응답 모델 (record)
│       └─ YahooFinanceClient.java            ← WebClient 기반 무료 API 클라이언트
└─ resources/
    ├─ db/migration/
    │   └─ V3__add_tbl_insight_result.sql     ← Flyway 마이그레이션
    └─ mapper/
        └─ InsightResultMapper.xml            ← MyBatis SQL (upsert 포함)
```

### API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/api/v1/insights/results` | 전체 타입 결과 목록 |
| `GET` | `/api/v1/insights/results/{resultTypeCd}` | 특정 타입 단건 조회 |
| `POST` | `/api/v1/insights/build-context` | 컨텍스트 빌드 & 전체 upsert |

### build-context 내부 흐름

```
POST /api/v1/insights/build-context
  │
  1. PortfolioMapper.findByUserId(userId)
     └─ 사용자 포트폴리오 목록

  2. PortfolioItemMapper.findByPortfolioId(portfolioId) × 포트폴리오 수
     └─ 전체 보유 종목 (PortfolioItem 리스트)
     └─ stockCd 중복 제거 → ticker 리스트

  3. YahooFinanceClient.fetchBatch(tickers)
     └─ Yahoo Finance 비공식 API 호출 (키 없음, 무료)
        GET https://query1.finance.yahoo.com/v10/finance/quoteSummary/{ticker}
            ?modules=price,assetProfile,summaryDetail
     └─ 종목별 300ms 딜레이 (rate-limit 방지)
     └─ 실패 종목은 skip, 나머지로 계속 진행

  4. 타입별 컨텍스트 생성
     ├─ KEY_FINDINGS             섹터 비중(매수금액 기준) + 상위 3종목 + 집중도 경고
     ├─ INVESTMENT_STYLE         평균 PER·배당수익률 → 성장형/가치형/균형형 분류
     ├─ RISK_ASSESSMENT          52주 가격 위치·분산도·당일 등락 현황
     ├─ PORTFOLIO_ALIGNMENT      섹터 다양성 + 종목별 상세 한 줄 요약
     └─ INVESTMENT_RECOMMENDATION 분석 결과 기반 맞춤 경고 및 제안

  5. InsightResultMapper.upsert(item) × 5
     └─ ON DUPLICATE KEY UPDATE → 항상 최신 결과로 갱신
```

### Yahoo Finance 클라이언트

| 항목 | 내용 |
|------|------|
| 라이브러리 | `spring-boot-starter-webflux` (기존 의존성, 추가 없음) |
| API 키 | 불필요 |
| 일일 한도 | 없음 |
| 필수 헤더 | `User-Agent: Mozilla/5.0 ...` (없으면 429 반환) |
| 타임아웃 | 10초 |
| 조회 데이터 | 현재가, 등락률, 시총, PER, 배당수익률, 52주 고/저, 섹터, 업종 |

---

## 프론트엔드 (inv-fe)

### 추가/수정된 파일

```
src/app/
├─ services/
│   └─ insightService.ts           ← 신규 (Insight API 호출 함수)
├─ types/
│   └─ index.ts                    ← InsightResultResponse, InsightResultTypeCd 추가
└─ components/market/
    ├─ InsightsDashboard.tsx        ← 수정 (결과 1회 로딩 + 카드별 분배)
    └─ insights/
        ├─ KeyInsightsCard.tsx      ← 수정 (KEY_FINDINGS 데이터 표시)
        ├─ MyProfileCard.tsx        ← 수정 (INVESTMENT_STYLE 섹션 추가)
        └─ InsightResultCard.tsx    ← 신규 (나머지 3타입 + 결과보기 버튼)
```

### 컴포넌트별 역할

#### `InsightsDashboard`
- 마운트 시 `getAllInsightResults()` 1회 호출
- 각 카드에 해당 타입 결과를 props로 전달
- `InsightResultCard.onBuildComplete` 콜백으로 전체 결과 갱신

```typescript
const findResult = (typeCd: string) =>
  insightResults.find((r) => r.resultTypeCd === typeCd) ?? null;

<KeyInsightsCard insightResult={findResult("KEY_FINDINGS")} />
<MyProfileCard   insightResult={findResult("INVESTMENT_STYLE")} />
<InsightResultCard
  results={insightResults.filter((r) => RESULT_CARD_TYPES.includes(r.resultTypeCd))}
  onBuildComplete={setInsightResults}
/>
```

#### `KeyInsightsCard`
- `insightResult` prop이 있으면 `content`를 `\n` 기준으로 분리해 bullet 렌더링
- 없으면 기존 하드코딩 문구 표시 (fallback)

#### `MyProfileCard`
- 기존 `getRiskProfile()` 설문 점수 바는 그대로 유지
- `insightResult` prop이 있으면 카드 하단에 "🤖 AI 포트폴리오 분석" 섹션 추가

#### `InsightResultCard`
- `RISK_ASSESSMENT`, `PORTFOLIO_ALIGNMENT`, `INVESTMENT_RECOMMENDATION` 3종 표시
- **"결과보기"** 버튼 → `buildInsightContext()` 호출 → `onBuildComplete(all)` 로 전체 갱신

### "결과보기" 버튼 클릭 시 전체 갱신 흐름

```
[결과보기] 클릭
  └─ buildInsightContext()  POST /api/v1/insights/build-context
       └─ 응답: InsightResultResponse[] (5개 타입 전체)
            └─ onBuildComplete(all)
                 └─ InsightsDashboard.setInsightResults(all)
                      ├─ KeyInsightsCard   자동 재렌더링 (KEY_FINDINGS)
                      ├─ MyProfileCard     자동 재렌더링 (INVESTMENT_STYLE)
                      └─ InsightResultCard 자동 재렌더링 (나머지 3개)
```

---

## 타입 정의 (TypeScript)

```typescript
export type InsightResultTypeCd =
  | "KEY_FINDINGS"
  | "INVESTMENT_STYLE"
  | "RISK_ASSESSMENT"
  | "PORTFOLIO_ALIGNMENT"
  | "INVESTMENT_RECOMMENDATION";

export interface InsightResultResponse {
  resultId:      number;
  userId:        string;
  resultTypeCd:  InsightResultTypeCd;
  title:         string;
  content:       string;
  regDt:         string;
  updDt:         string | null;
}
```

---

## 향후 연동 포인트

| 항목 | 위치 | 설명 |
|------|------|------|
| 실제 AI/LLM 연동 | `InsightServiceImpl.buildAndSaveContext()` | 현재 규칙 기반 stub, 이 메서드에서 외부 AI API 호출 후 타입별 응답 파싱 |
| 설문 결과 반영 | `InsightServiceImpl` | `survey-service` FeignClient 추가 후 설문 점수·성향 코드를 컨텍스트에 포함 |
| 국내주 지원 | `YahooFinanceClient` | KOSPI/KOSDAQ 종목은 야후 ticker 형식(`005930.KS`)으로 변환 필요 |
