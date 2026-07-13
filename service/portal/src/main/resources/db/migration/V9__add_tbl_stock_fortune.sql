-- 종목운세 테이블. 수동 실행 (V3~V8과 동일하게 Flyway 미사용)
-- (ticker, fortune_date)당 1건 전역 공유 — 일별 갱신. 지난 날짜 행은 삭제하지 않는다(조회는 항상 오늘 날짜).
CREATE TABLE tbl_stock_fortune (
    fortune_id   BIGINT       AUTO_INCREMENT PRIMARY KEY              COMMENT '운세 ID',
    ticker       VARCHAR(20)  NOT NULL                                COMMENT '종목 티커 (정규화·정식형: US는 그대로, KR은 005930.KS 형식)',
    fortune_date DATE         NOT NULL                                COMMENT '운세 기준일 (KST)',
    content      TEXT         NOT NULL                                COMMENT '운세 내용',
    use_yn       CHAR(1)      NOT NULL DEFAULT 'Y'                    COMMENT '사용여부',
    reg_dt       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '등록일시',
    upd_dt       DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP        COMMENT '수정일시',
    UNIQUE KEY uk_fortune_ticker_date (ticker, fortune_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='종목운세 (종목·일자당 1건, 전역 공유)';
