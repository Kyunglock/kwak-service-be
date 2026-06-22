-- 배당 이력 테이블
CREATE TABLE tbl_dividend_history (
    dividend_id  BIGINT         NOT NULL AUTO_INCREMENT   COMMENT '배당ID',
    stock_cd     VARCHAR(20)    NOT NULL                  COMMENT '종목코드',
    ex_date      DATE           NOT NULL                  COMMENT '배당락일',
    dividend     DECIMAL(10, 4) NOT NULL                  COMMENT '배당금',
    reg_dt       DATETIME       DEFAULT NOW()             COMMENT '등록일시',
    PRIMARY KEY (dividend_id),
    UNIQUE KEY uq_dividend (stock_cd, ex_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='배당 이력';
