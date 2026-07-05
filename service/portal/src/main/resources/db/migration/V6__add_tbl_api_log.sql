-- API 요청 로그 테이블 — 공격 시도 탐지/운영 감사용 (응답 본문은 저장하지 않음)
CREATE TABLE IF NOT EXISTS tbl_api_log (
    log_id       BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      VARCHAR(64)  NULL,              -- 미인증 요청(공격 시도 다수)은 NULL
    ip           VARCHAR(45)  NULL,
    method       VARCHAR(10)  NOT NULL,
    url          VARCHAR(500) NOT NULL,          -- URI + 쿼리스트링
    request_body TEXT         NULL,              -- 1000자 절단, /api/v1/auth/** 는 마스킹
    status       SMALLINT     NOT NULL,          -- 응답 상태코드 (401/403 폭주 = 공격 신호)
    user_agent   VARCHAR(255) NULL,
    reg_dt       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_apilog_ip (ip, reg_dt),
    KEY idx_apilog_status (status, reg_dt),
    KEY idx_apilog_regdt (reg_dt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
