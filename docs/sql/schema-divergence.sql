-- ============================================================
-- Divergence Detection Pipeline - DB Schema (MySQL)
-- 실행 순서: financial_statements → financial_derived_metrics
--           → divergence_detection_result
-- FK 미사용 (기존 프로젝트 컨벤션)
-- ============================================================

-- 1. 원시 재무제표 (collector가 외부 API에서 수집해 저장)
CREATE TABLE IF NOT EXISTS financial_statements (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    stock_cd             VARCHAR(20)     NOT NULL COMMENT '종목 코드 (예: AAPL)',
    fiscal_year          SMALLINT        NOT NULL COMMENT '회계 연도',
    fiscal_quarter       TINYINT         NOT NULL COMMENT '회계 분기 (1~4)',
    period_end_dt        DATE            NOT NULL COMMENT '분기 종료일',
    revenue              DECIMAL(20, 2)  NULL     COMMENT '매출액',
    gross_profit         DECIMAL(20, 2)  NULL     COMMENT '매출총이익',
    operating_income     DECIMAL(20, 2)  NULL     COMMENT '영업이익',
    net_income           DECIMAL(20, 2)  NULL     COMMENT '순이익',
    operating_cash_flow  DECIMAL(20, 2)  NULL     COMMENT '영업현금흐름',
    free_cash_flow       DECIMAL(20, 2)  NULL     COMMENT '잉여현금흐름',
    capex                DECIMAL(20, 2)  NULL     COMMENT '자본적 지출',
    depreciation         DECIMAL(20, 2)  NULL     COMMENT '감가상각',
    total_assets         DECIMAL(20, 2)  NULL     COMMENT '총자산',
    accounts_receivable  DECIMAL(20, 2)  NULL     COMMENT '매출채권',
    inventory            DECIMAL(20, 2)  NULL     COMMENT '재고자산',
    cogs                 DECIMAL(20, 2)  NULL     COMMENT '매출원가',
    rd_expense           DECIMAL(20, 2)  NULL     COMMENT 'R&D 비용',
    sga_expense          DECIMAL(20, 2)  NULL     COMMENT 'SG&A 비용',
    ebitda               DECIMAL(20, 2)  NULL     COMMENT 'EBITDA',
    net_debt             DECIMAL(20, 2)  NULL     COMMENT '순부채',
    reg_dt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upd_dt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_financial_statements (stock_cd, fiscal_year, fiscal_quarter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='원시 재무제표';

-- 2. 파생 지표 (분기별 시계열, DerivedMetricsService가 계산)
CREATE TABLE IF NOT EXISTS financial_derived_metrics (
    id                    BIGINT          NOT NULL AUTO_INCREMENT,
    stock_cd              VARCHAR(20)     NOT NULL COMMENT '종목 코드',
    fiscal_year           SMALLINT        NOT NULL COMMENT '회계 연도',
    fiscal_quarter        TINYINT         NOT NULL COMMENT '회계 분기',
    period_end_dt         DATE            NULL     COMMENT '분기 종료일',
    revenue_yoy           DECIMAL(10, 6)  NULL     COMMENT '매출 YoY 성장률',
    revenue_qoq           DECIMAL(10, 6)  NULL     COMMENT '매출 QoQ 성장률',
    fcf_yoy               DECIMAL(10, 6)  NULL     COMMENT 'FCF YoY 성장률',
    ocf_yoy               DECIMAL(10, 6)  NULL     COMMENT 'OCF YoY 성장률',
    ni_ocf_gap            DECIMAL(10, 6)  NULL     COMMENT '(NetIncome-OCF)/TotalAssets',
    dso                   DECIMAL(10, 4)  NULL     COMMENT 'Days Sales Outstanding (일)',
    dio                   DECIMAL(10, 4)  NULL     COMMENT 'Days Inventory Outstanding (일)',
    gross_margin          DECIMAL(10, 6)  NULL     COMMENT '매출총이익률',
    op_margin             DECIMAL(10, 6)  NULL     COMMENT '영업이익률',
    fcf_margin            DECIMAL(10, 6)  NULL     COMMENT 'FCF 마진',
    capex_to_depreciation DECIMAL(10, 4)  NULL     COMMENT 'CapEx / Depreciation',
    rd_intensity          DECIMAL(10, 6)  NULL     COMMENT 'R&D / Revenue',
    sga_intensity         DECIMAL(10, 6)  NULL     COMMENT 'SG&A / Revenue',
    net_debt_to_ebitda    DECIMAL(10, 4)  NULL     COMMENT 'NetDebt / EBITDA',
    reg_dt                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upd_dt                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_derived_metrics (stock_cd, fiscal_year, fiscal_quarter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='파생 재무 지표';

-- 3. Divergence 탐지 결과
CREATE TABLE IF NOT EXISTS divergence_detection_result (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    stock_cd         VARCHAR(20)     NOT NULL COMMENT '종목 코드',
    fiscal_year      SMALLINT        NOT NULL COMMENT '이상 신호 발생 회계 연도',
    fiscal_quarter   TINYINT         NOT NULL COMMENT '이상 신호 발생 분기',
    divergence_type  VARCHAR(50)     NOT NULL COMMENT '탐지 유형',
    severity         DECIMAL(5, 4)   NOT NULL COMMENT '심각도 0.0000~1.0000',
    evidence         JSON            NOT NULL COMMENT '탐지 근거 데이터',
    detected_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    batch_run_dt     DATE            NOT NULL COMMENT '배치 실행일',
    PRIMARY KEY (id),
    INDEX idx_divergence_stock (stock_cd, batch_run_dt),
    INDEX idx_divergence_batch (batch_run_dt, severity DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Divergence 탐지 결과';
