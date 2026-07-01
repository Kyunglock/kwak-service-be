-- 감사/활동 로그 테이블
CREATE TABLE IF NOT EXISTS tbl_activity_log (
    log_id       BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      VARCHAR(64)   NULL,               -- 익명/시스템은 NULL
    action_type  VARCHAR(40)   NOT NULL,           -- LOGIN, LOGOUT, TRADE_BUY ...
    target_type  VARCHAR(40)   NULL,               -- PORTFOLIO, SURVEY, STOCK ...
    target_id    VARCHAR(64)   NULL,
    detail       VARCHAR(1000) NULL,               -- 부가 컨텍스트(요약 문자열)
    ip           VARCHAR(45)   NULL,
    user_agent   VARCHAR(255)  NULL,
    reg_dt       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_actlog_user (user_id, reg_dt),
    KEY idx_actlog_action (action_type, reg_dt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
