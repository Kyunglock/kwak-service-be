# Divergence 자동 추출 파이프라인

> `service/stock-advisor` 모듈에 통합된 재무 이상 신호 탐지 파이프라인

---

## 아키텍처 개요

```
[financial_statements]
        │
        ▼
┌─────────────────────┐
│  Step 1             │  DerivedMetricsServiceImpl
│  Metric Computation │  → 13개 파생 지표 계산 및 저장
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  Step 2             │  DivergenceDetectionServiceImpl
│  Divergence         │  → 7개 Detector (Strategy Pattern)
│  Detection          │  → 결과 DB 저장
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  Step 3             │  DivergenceInterpretationServiceImpl
│  LLM Interpretation │  → Redis 캐시 조회 → OpenAI 호출
└─────────────────────┘
```

**핵심 원칙**: 탐지 로직은 결정론적 규칙 기반, LLM은 자연어 해석에만 사용

**배치 스케줄**: 매일 06:00 KST (`@Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")`)

---

## 패키지 구조

```
service/stock-advisor/src/main/java/com/investment/stockadvisor/
│
├── domain/
│   ├── entity/divergence/
│   │   ├── FinancialStatement.java          # 원시 재무제표
│   │   ├── FinancialDerivedMetrics.java     # 파생 지표 (13종)
│   │   ├── DivergenceDetectionResult.java  # 탐지 결과
│   │   └── StockSector.java                # 종목-섹터 매핑
│   │
│   ├── detector/
│   │   ├── DivergenceDetector.java         # Strategy 인터페이스
│   │   ├── DetectionResult.java            # 탐지 결과 VO
│   │   └── impl/
│   │       ├── AccrualsQualityDetector.java
│   │       ├── RevenueFcfGapDetector.java
│   │       ├── ChannelStuffingDetector.java
│   │       ├── InventoryBuildupDetector.java
│   │       ├── MarginMixDetector.java
│   │       ├── CapExRegimeShiftDetector.java
│   │       └── PeerOutlierDetector.java
│   │
│   └── repository/divergence/
│       ├── FinancialStatementMapper.java
│       ├── FinancialDerivedMetricsMapper.java
│       ├── DivergenceDetectionResultMapper.java
│       └── StockSectorMapper.java
│
├── application/
│   ├── config/
│   │   ├── OpenAiProperties.java           # OpenAI 설정 바인딩
│   │   └── RestTemplateConfig.java
│   │
│   ├── service/divergence/
│   │   ├── DerivedMetricsService.java
│   │   ├── DerivedMetricsServiceImpl.java
│   │   ├── DivergenceDetectionService.java
│   │   ├── DivergenceDetectionServiceImpl.java
│   │   ├── DivergenceDetectionConverter.java  # MapStruct
│   │   ├── DivergenceInterpretationService.java
│   │   ├── DivergenceInterpretationServiceImpl.java
│   │   └── SectorPercentileSeverityCalculator.java
│   │
│   └── dto/divergence/
│       ├── DivergenceResultResponse.java
│       └── DivergenceInterpretationResponse.java
│
├── infrastructure/openai/
│   └── OpenAiClient.java
│
├── api/controller/
│   └── DivergenceController.java
│
└── scheduler/
    └── DivergenceDetectionScheduler.java
```

---

## Phase 1 — 기반 구조 + 2개 Detector

### 파생 지표 13종

| 지표 | 산식 |
|---|---|
| `revenue_yoy` | (revenue − revenue_prior_year) / \|revenue_prior_year\| |
| `revenue_qoq` | (revenue − revenue_prev_q) / \|revenue_prev_q\| |
| `fcf_yoy` | FCF YoY 성장률 |
| `ocf_yoy` | OCF YoY 성장률 |
| `ni_ocf_gap` | (net_income − operating_cash_flow) / total_assets |
| `dso` | accounts_receivable × 90 / revenue |
| `dio` | inventory × 90 / cogs |
| `gross_margin` | gross_profit / revenue |
| `op_margin` | operating_income / revenue |
| `fcf_margin` | free_cash_flow / revenue |
| `capex_to_depreciation` | capex / depreciation |
| `rd_intensity` | rd_expense / revenue |
| `net_debt_to_ebitda` | net_debt / ebitda |

### Detector — Phase 1

| Detector | 조건 | Severity |
|---|---|---|
| `AccrualsQualityDetector` | `ni_ocf_gap > 0.10` 연속 2분기 | linear clamp (0.10→0.0, 0.30→1.0) |
| `RevenueFcfGapDetector` | `revenue_yoy − fcf_yoy > 0.20` 연속 2분기 | linear clamp (0.20→0.0, 0.60→1.0) |

### DB 스키마

```sql
-- schema-divergence.sql
financial_statements          -- 원시 재무제표 (collector 적재)
financial_derived_metrics     -- 파생 지표 (UNIQUE: stock_cd, fiscal_year, fiscal_quarter)
divergence_detection_result   -- 탐지 결과
```

### REST API

```
GET /api/v1/divergences/stocks/{stockCd}         # 종목별 탐지 결과
GET /api/v1/divergences/batch?batchRunDt=YYYY-MM-DD  # 배치일별 결과 (severity DESC)
```

---

## Phase 2 — 5개 추가 Detector + 섹터 퍼센타일 Severity

### Detector — Phase 2

| Detector | 조건 | 주요 Evidence |
|---|---|---|
| `ChannelStuffingDetector` | revenue QoQ > 15% AND DSO 증가 > 15% 연속 2분기 | dso_current, dso_increase_pct |
| `InventoryBuildupDetector` | DIO 3분기 연속 증가 AND revenue YoY ±5% 이내 | dio_growth_3q, revenue_yoy |
| `MarginMixDetector` | gross_margin ↑ AND op_margin ↓ 동시 2분기 | max_gross_op_divergence |
| `CapExRegimeShiftDetector` | capex/dep > 3.0 연속 2분기 또는 단일분기 50%+ 급변 | signal(SUSTAINED_HIGH/SINGLE_QUARTER_SPIKE) |
| `PeerOutlierDetector` | 섹터 피어 op_margin Z-score \|z\| > 2.0 | z_score, sector_mean_op_margin, peer_count |

### 섹터 퍼센타일 Severity

```
severity = |percentile(value, sector_distribution) - 0.5| × 2
```

- 섹터 피어 수 < 10 이거나 `stock_sector` 데이터 없으면 → Phase 1 linear clamp로 fallback
- `SectorPercentileSeverityCalculator`가 `DivergenceDetectionServiceImpl`에서 자동 적용

### 추가 스키마

```sql
-- schema-divergence-phase2.sql
stock_sector  -- stock_cd, sector_code, sector_name (UNIQUE: stock_cd)
```

---

## Phase 3 — OpenAI LLM 해석 + Redis 캐싱

### 흐름

```
GET /stocks/{stockCd}/interpretations
          │
          ▼
  Redis cache 조회
  ├─ HIT  → cached=true 로 반환
  └─ MISS → OpenAI API 호출
              │  model: gpt-4o
              │  response_format: {type: json_object}
              ▼
           결과 파싱
              │
              ▼
      Redis 저장 (TTL 24h)
      cache key: divergence:interpretation:{stockCd}:{type}:{year}:{quarter}
              │
              ▼
          cached=false 로 반환
```

### LLM 응답 구조

```json
{
  "summary": "이상 신호 요약 (2-3 문장)",
  "risk_level": "HIGH | MEDIUM | LOW",
  "key_drivers": ["주요 원인 1", "주요 원인 2"],
  "watch_points": ["모니터링 포인트 1", "모니터링 포인트 2"]
}
```

### 토큰 사용량 로깅

```
[OpenAI] stockCd=AAPL type=ACCRUALS_QUALITY prompt_tokens=312 completion_tokens=187
```

### 배치 Step 3

- 당일 탐지 결과 전체에 대해 순차적으로 LLM 해석 실행
- 건별 try/catch → 하나 실패해도 나머지 계속 진행
- 완료 후 `success={n} fail={n}` 로그 출력

### 추가 API

```
GET /api/v1/divergences/stocks/{stockCd}/interpretations
```

### 설정 (application.yml)

```yaml
openai:
  api-key: ${OPENAI_API_KEY:}
  model: gpt-4o
  base-url: https://api.openai.com/v1
```

---

## 전체 배치 파이프라인 로그 흐름

```
[DivergenceBatch] pipeline started
[DivergenceBatch] step1 derived metrics done
[DivergenceBatch] step2 detection done
[OpenAI] stockCd=AAPL type=ACCRUALS_QUALITY prompt_tokens=312 completion_tokens=187
[OpenAI] stockCd=MSFT type=REVENUE_FCF_GAP prompt_tokens=298 completion_tokens=201
...
[DivergenceBatch] interpretation complete success=42 fail=0
[DivergenceBatch] step3 interpretation done
```

---

## 운영 체크리스트

- [ ] `OPENAI_API_KEY` 환경변수 설정
- [ ] `schema-divergence.sql` 실행
- [ ] `schema-divergence-phase2.sql` 실행
- [ ] `stock_sector` 테이블 종목-섹터 매핑 데이터 적재
- [ ] `financial_statements` 테이블 재무제표 데이터 수집 (collector 연동)
- [ ] Redis 연결 확인 (`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`)
