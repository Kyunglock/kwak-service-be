-- Phase 2 DDL: sector membership table for peer outlier detection and percentile severity
CREATE TABLE IF NOT EXISTS stock_sector (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    stock_cd    VARCHAR(20)  NOT NULL,
    sector_code VARCHAR(50)  NOT NULL,
    sector_name VARCHAR(100) NOT NULL,
    reg_dt      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upd_dt      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_sector_stock_cd  (stock_cd),
    INDEX       idx_stock_sector_code    (sector_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
