# AI 인사이트 결과 — RAG 기반 구현 가이드

## 목차
1. [개요](#1-개요)
2. [전체 데이터 흐름](#2-전체-데이터-흐름)
3. [인사이트 타입 6종](#3-인사이트-타입-6종)
4. [백엔드 구현 (kwak-service)](#4-백엔드-구현-kwak-service)
5. [Yahoo Finance 외부 API](#5-yahoo-finance-외부-api)
6. [투자 MBTI (STOCK_MBTI)](#6-투자-mbti-stock_mbti)
7. [프론트엔드 구현 (inv-fe)](#7-프론트엔드-구현-inv-fe)
8. [content 포맷 상세](#8-content-포맷-상세)
9. [트러블슈팅](#9-트러블슈팅)

---

## 1. 개요

사용자의 **포트폴리오 데이터 + 설문 응답**을 컨텍스트로 삼아 6가지 인사이트를 생성하고 DB에 저장한다.  
프론트에서 **"결과보기" 버튼**을 누르면 백엔드가 컨텍스트를 빌드·저장하고 결과를 반환한다.

> RAG(Retrieval-Augmented Generation) 관점에서:
> - **Retrieval** — Yahoo Finance v10 API로 실시간 주가·섹터 데이터 수집, DB에서 포트폴리오·설문 데이터 조회
> - **Generation** — 수집된 데이터를 규칙 기반 로직으로 가공해 자연어 분석 텍스트(content) 생성
> - **Augment** — 생성된 content를 `insight_result` 테이블에 upsert, 이후 AI 프롬프트 컨텍스트로 활용 가능

---

## 2. 전체 데이터 흐름

```
[사용자: 결과보기 클릭]
        │
        ▼
[POST /api/insight/build]                    ← InsightController
        │
        ▼
[InsightServiceImpl.buildAndSaveContext()]   ← 핵심 오케스트레이터
        │
        ├─① portfolioMapper.findByUserId()
        │    └─ portfolioItemMapper.findByPortfolioId()  → allItems (보유 종목 목록)
        │
        ├─② yahooFinanceClient.fetchBatch(tickers)       → stockMap (실시간 시세)
        │
        ├─③ surveyMapper.findRiskProfileScores(userId)   → surveyScores (설문 점수)
        │
        ├─④ portfolioItemMapper.findTopStocksByHolderCount(10) → topStocks (인기 종목)
        │
        ├─⑤ 6개 빌더 메서드 병렬 호출
        │    ├─ buildKeyFindings()
        │    ├─ buildInvestmentStyle()
        │    ├─ buildRiskAssessment()
        │    ├─ buildPortfolioAlignment()
        │    ├─ buildInvestmentRecommendation()
        │    └─ buildStockMbti()
        │
        └─⑥ insightResultMapper.upsert() × 6            → DB 저장
                │
                ▼
        [GET /api/insight/all]  → 프론트 카드 렌더링
```

---

## 3. 인사이트 타입 6종

| resultTypeCd | 제목 | 데이터 소스 | 프론트 컴포넌트 |
|---|---|---|---|
| `KEY_FINDINGS` | 주요 발견사항 | Yahoo Finance (인기 종목 Top 10) | `KeyInsightsCard` |
| `INVESTMENT_STYLE` | 나의 투자성향 | 설문 점수 → 포트폴리오 fallback | `MyProfileCard` |
| `RISK_ASSESSMENT` | 리스크 평가 | 포트폴리오 + 시세 | `InsightResultCard` |
| `PORTFOLIO_ALIGNMENT` | 포트폴리오 정합성 | 포트폴리오 + 시세 | `InsightResultCard` |
| `INVESTMENT_RECOMMENDATION` | 투자 추천 | 포트폴리오 + 시세 | `InsightResultCard` |
| `STOCK_MBTI` | 투자 MBTI | 설문 점수 (3차원) | `StockMbtiCard` |

---

## 4. 백엔드 구현 (kwak-service)

### 4-1. 파일 위치

```
service/portal/src/main/java/com/investment/portal/
├── application/service/insight/
│   ├── InsightService.java            (인터페이스)
│   └── InsightServiceImpl.java        ★ 핵심 로직
├── infrastructure/external/yahoo/
│   ├── YahooFinanceClient.java        ★ 외부 API 클라이언트
│   └── StockInfo.java                 (DTO record)
└── domain/
    ├── entity/insight/InsightResult.java
    └── repository/insight/InsightResultMapper.java
```

### 4-2. InsightServiceImpl 주요 메서드

```java
// 전체 빌드 진입점
public List<InsightResultResponse> buildAndSaveContext(String userId)

// 타입별 빌더
private InsightResult buildKeyFindings(String userId)
private InsightResult buildInvestmentStyle(String userId, List<PortfolioItem>, Map<String,StockInfo>)
private InsightResult buildRiskAssessment(String userId, List<PortfolioItem>, Map<String,StockInfo>)
private InsightResult buildPortfolioAlignment(String userId, List<PortfolioItem>, Map<String,StockInfo>)
private InsightResult buildInvestmentRecommendation(String userId, List<PortfolioItem>, Map<String,StockInfo>)
private InsightResult buildStockMbti(String userId)

// 헬퍼
private InsightResult buildItem(String userId, String typeCd, String content)
private String[] getMbtiMeta(String code)          // 8가지 MBTI 유형 메타
private String generateInvestmentOpinion(...)      // INVESTMENT_STYLE 설문 기반 텍스트
```

### 4-3. KEY_FINDINGS 빌드 로직

```
1. findTopStocksByHolderCount(10) → 플랫폼 전체 인기 종목 Top 10
2. yahooFinanceClient.fetchBatch() → 실시간 시세 조회
3. infoList 비어있으면 → fallback 메시지 반환
4. 섹터 필터: sector 빈값 + "Unknown" 제외
5. 4개 bullet 생성:
   ① 상승세 섹터 심리 (risingEntry - 섹터별 평균 등락률)
   ② 인기 섹터 집중도 (topSectorEntry - 섹터별 종목 수 비율)
   ③ 성장주(PER>25) 비율 → 항상 포함 (unconditional)
   ④ 52주 가격 위치 (avgPos - 현재가 / 52주 범위)
6. String.join("\n", bullets) → content
```

### 4-4. INVESTMENT_STYLE 빌드 로직

```
[설문 완료 시]
1. surveyMapper.findRiskProfileScores(userId)
2. description 기준으로 scoreMap 구성
   - 수익추구, 리스크허용, 장기투자
3. avgScore로 유형명 결정
   - ≥70 : 공격 성장형
   - ≥50 : 중위험 성장형
   - <50  : 안정 추구형
4. generateInvestmentOpinion() → 3문장 조합 텍스트
   - 항목 간 편차 ≥25 이면 강조 문장 추가

[설문 미완료 시]
- stockMap이 있으면 → PER/배당률 기반 포트폴리오 fallback
- stockMap도 없으면 → "설문을 완료하면..." 안내 텍스트
```

---

## 5. Yahoo Finance 외부 API

### 5-1. 파일: `YahooFinanceClient.java`

### 5-2. 인증 방식 (2024년 이후 필수)

Yahoo Finance v10 API는 2024년부터 **crumb + 쿠키 인증**이 필수다.

```
Step 1: GET https://fc.yahoo.com
        └─ Set-Cookie 헤더에서 쿠키 추출 (A1, A1S, A3, GUC 등)

Step 2: GET https://query1.finance.yahoo.com/v1/test/getcrumb
        └─ Cookie: {step1 쿠키} 헤더 포함
        └─ 응답: crumb 문자열 (예: "AbCdEf12345")

Step 3: GET https://query1.finance.yahoo.com/v10/finance/quoteSummary/{ticker}
        └─ ?modules=price,assetProfile,summaryDetail&crumb={crumb}
        └─ Cookie: {step1 쿠키} 헤더 포함
```

### 5-3. Crumb 캐싱 전략

```java
private volatile String crumb;        // 서버 재시작 전까지 캐시
private volatile String cookieHeader;

private synchronized void initCrumb() {
    if (crumb != null) return;        // DCL 패턴 — 이미 초기화되면 스킵
    // ... 2-step 초기화 ...
}

// 401/빈 응답 수신 시 crumb 만료 처리 후 1회 재시도
public Optional<StockInfo> fetchStockInfo(String ticker) {
    initCrumb();
    Optional<StockInfo> result = doFetch(ticker);
    if (result.isEmpty()) {
        resetCrumb(); initCrumb();    // crumb 재발급
        result = doFetch(ticker);
    }
    return result;
}
```

### 5-4. StockInfo 레코드

```java
public record StockInfo(
    String ticker,
    String shortName,
    String sector,
    String industry,
    double price,
    double changePercent,      // 당일 등락률 (%)
    long   marketCap,
    double peRatio,            // Trailing P/E
    double dividendYield,      // 소수 (0.03 = 3%)
    double fiftyTwoWeekLow,
    double fiftyTwoWeekHigh,
    String currency
) {
    // 현재가의 52주 범위 내 위치 (0~100%)
    public double pricePosition() {
        if (fiftyTwoWeekHigh <= fiftyTwoWeekLow) return 50;
        return (price - fiftyTwoWeekLow) / (fiftyTwoWeekHigh - fiftyTwoWeekLow) * 100;
    }
}
```

### 5-5. fetchBatch 호출 간격

Rate-limit 방지를 위해 종목당 **400ms 딜레이** 적용.

```java
private static final long CALL_DELAY_MS = 400;
```

---

## 6. 투자 MBTI (STOCK_MBTI)

### 6-1. 3차원 분류 기준

| 차원 | 설문 항목 (description) | ≥50 | <50 |
|---|---|---|---|
| 수익 | `수익추구` | G (Growth) | V (Value) |
| 리스크 | `리스크허용` | R (Risk) | S (Safe) |
| 기간 | `장기투자` | L (Long) | T (Trade) |

### 6-2. 8가지 유형표

| 코드 | 유형명 | 핵심 성향 | 테마 색 | 이모지 |
|---|---|---|---|---|
| GRL | 성장 개척자 | 공격적 수익 + 리스크 감내 + 장기 | emerald | 🚀 |
| GRT | 모멘텀 헌터 | 공격적 수익 + 리스크 감내 + 단기 | red | ⚡ |
| GSL | 균형 성장가 | 수익 추구 + 안전 중시 + 장기 | teal | 🌱 |
| GST | 신중한 수익가 | 수익 목표 뚜렷 + 안전 + 단기 | blue | 💼 |
| VRL | 가치 탐험가 | 안정 선호 + 리스크 감내 + 장기 | purple | 🔭 |
| VRT | 역발상 트레이더 | 안정 선호 + 리스크 감내 + 단기 | orange | 🎲 |
| VSL | 배당 수호자 | 안정 최우선 + 안전 + 장기 배당 | indigo | 🛡️ |
| VST | 안전 수익가 | 안정 + 안전 + 단기 저위험 | slate | 🏦 |

### 6-3. content 포맷

```
{code}\n{유형명}\n{설명}\n{수익점수}:{리스크점수}:{장기점수}

예시:
GSL
균형 성장가
수익성을 추구하되 리스크를 신중히 관리하며...
72:38:65
```

설문 미완료 시:
```
설문 미완료
투자 MBTI 분석 불가
투자 성향 설문을 완료하면 나만의 투자 MBTI를 확인할 수 있습니다.
0:0:0
```

---

## 7. 프론트엔드 구현 (inv-fe)

### 7-1. 파일 위치

```
src/app/components/market/
├── InsightsDashboard.tsx          ★ 전체 레이아웃
└── insights/
    ├── KeyInsightsCard.tsx        → KEY_FINDINGS
    ├── MyProfileCard.tsx          → INVESTMENT_STYLE
    ├── StockMbtiCard.tsx          ★ STOCK_MBTI (신규)
    ├── InsightResultCard.tsx      → RISK / ALIGNMENT / RECOMMENDATION
    ├── GuruMatchCard.tsx          → 구루 포트폴리오 매칭
    ├── SectorAnalysisCard.tsx     → 섹터 분석
    ├── SentimentIndexCard.tsx     → 시장 심리 지수
    └── InvestorSentimentCard.tsx  → 투자자 심리 트렌드
```

### 7-2. InsightsDashboard 카드 순서

```tsx
<InsightResultCard />        {/* RISK + ALIGNMENT + RECOMMENDATION + 결과보기 버튼 */}
<KeyInsightsCard />          {/* KEY_FINDINGS */}
<MyProfileCard />            {/* INVESTMENT_STYLE */}
<StockMbtiCard />            {/* STOCK_MBTI */}
<GuruMatchCard />
<SectorAnalysisCard />
<SentimentIndexCard />
<InvestorSentimentCard />
```

### 7-3. 결과보기 트리거 흐름

```
[사용자: 결과보기 버튼 클릭]
        │
        ▼
InsightResultCard → POST /api/insight/build
        │
        ▼
onBuildComplete(results) → setInsightResults(results)
        │
        ▼
findResult(typeCd) → 각 카드에 props 전달 → 리렌더링
```

### 7-4. StockMbtiCard 구조

```tsx
// content 파싱
const lines = insightResult?.content?.split("\n") ?? [];
const code  = lines[0];  // "GRL"
const name  = lines[1];  // "성장 개척자"
const desc  = lines[2];  // 설명 텍스트
const [profitScore, riskScore, longScore] = lines[3].split(":").map(Number);

// 렌더링 요소
// 1. 코드 배지 (유형 코드 + 색상 블록)
// 2. 유형명 + 이모지
// 3. 설명 텍스트
// 4. 3차원 점수 바 (G↔V / R↔S / L↔T)
//    - 점수는 0~100, 중앙선(50) 기준
//    - 활성 레이블 bold + 테마 컬러
```

### 7-5. 타입 정의

```typescript
// src/app/types/index.ts
export type InsightResultTypeCd =
  | "KEY_FINDINGS"
  | "INVESTMENT_STYLE"
  | "RISK_ASSESSMENT"
  | "PORTFOLIO_ALIGNMENT"
  | "INVESTMENT_RECOMMENDATION"
  | "STOCK_MBTI";          // ← 추가
```

---

## 8. content 포맷 상세

각 인사이트는 `\n` 구분 평문 텍스트로 저장된다. (DB 컬럼: `content TEXT`)

### KEY_FINDINGS
```
{섹터} 주에 대한 {심리} 되고 있습니다. (평균 {±X.X%})
{섹터} 섹터 선호도가 인기 종목의 {N}%를 차지하며...
투자자의 {N}%가 성장주(PER 25이상) 중심의...
인기 종목 평균 가격이 52주 {위치} {N}% 수준으로...
```

### INVESTMENT_STYLE
```
{유형명}
{3문장 투자 성향 의견} [특이 항목 강조 문장]
```

### RISK_ASSESSMENT
```
분산도: {등급} ({N}종목)
가격 위치: {등급} (평균 {N}%)
시장 흐름: {단기 상승/하락 종목 다수}
```

### PORTFOLIO_ALIGNMENT
```
포트폴리오 정합성: {높음/보통/낮음}
보유 섹터: {N}개 | 종목: {N}개
배당주: {N}종목 · 성장주(PER25↑): {N}종목

[종목 상세]
{ticker}: {name} | {sector} | 가격:{price} | 등락:{changePercent%} | PER:{peRatio}
...
```

### INVESTMENT_RECOMMENDATION
```
1. {추천 사항 1}
2. {추천 사항 2}
...
```

### STOCK_MBTI
```
{GRL|GRT|GSL|GST|VRL|VRT|VSL|VST}
{유형명}
{설명}
{수익점수}:{리스크점수}:{장기점수}
```

---

## 9. 트러블슈팅

### Q1. KEY_FINDINGS에 "시장 데이터를 일시적으로 불러올 수 없습니다" 표시

**원인**: Yahoo Finance API 조회 실패 (crumb 만료 또는 IP 차단)

**확인 방법**: 서버 로그에서 아래 패턴 검색
```
[YahooFinance] HTTP 401 - ticker: ...
[YahooFinance] crumb 획득 실패 - response: ...
[YahooFinance] 조회 실패 - ticker: ...
```

**해결**:
1. 서버 재시작 → crumb 캐시 초기화 후 재시도
2. `HTTP 401` 반복 시 → 서버 IP가 Yahoo에 차단된 것. CloudFlare Workers 등 프록시 레이어 추가 고려
3. 장기 대안 → Alpha Vantage / Financial Modeling Prep 같은 API key 기반 서비스로 교체

### Q2. STOCK_MBTI가 "설문 미완료" 상태로 표시

**원인**: `survey_result` 테이블에 해당 userId의 설문 데이터 없음

**확인**: `surveyMapper.findRiskProfileScores(userId)` 호출 결과 빈 리스트

**해결**: 사용자가 설문(수익추구 / 리스크허용 / 장기투자 3개 항목)을 완료한 뒤 결과보기 클릭

### Q3. 결과보기 후 데이터가 갱신되지 않음

**확인 순서**:
1. Network 탭 → `POST /api/insight/build` 응답 확인
2. DB `insight_result` 테이블에서 `updated_at` 컬럼 갱신 여부 확인
3. `insightResultMapper.upsert()` — PK가 `(userId, resultTypeCd)`인지 확인

### Q4. INVESTMENT_STYLE이 설문 기반이 아닌 포트폴리오 기반으로 나옴

**원인**: `surveyMapper.findRiskProfileScores(userId)` 조회 결과 빈 리스트

**확인**: 설문 description 컬럼 값이 정확히 `수익추구` / `리스크허용` / `장기투자` 인지 확인  
(scoreMap key 매핑이 description 값에 의존함)

---

## 브랜치 정보

| 저장소 | 브랜치 |
|---|---|
| kyunglock/kwak-service | `claude/add-insight-results-view-yB7zK` |
| kyunglock/inv-fe | `claude/add-insight-results-view-yB7zK` |
