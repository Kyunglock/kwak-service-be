-- 메뉴 이동(탭 전환) 로그 테이블 — 사용 패턴 분석용
CREATE TABLE IF NOT EXISTS tbl_menu_log (
    log_id       BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      VARCHAR(64) NULL,              -- 공개 페이지 확장 대비 NULL 허용
    session_id   VARCHAR(64) NOT NULL,          -- FE sessionStorage UUID, 세션 내 이동 흐름 추적
    menu_cd      VARCHAR(40) NOT NULL,          -- 이동한 메뉴(탭): portfolio, survey, insights ...
    prev_menu_cd VARCHAR(40) NULL,              -- 직전 메뉴 (최초 진입은 NULL)
    reg_dt       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_menulog_user (user_id, reg_dt),
    KEY idx_menulog_menu (menu_cd, reg_dt),
    KEY idx_menulog_session (session_id, reg_dt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
