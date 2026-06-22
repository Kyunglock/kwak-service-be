-- RAG 인사이트 결과 테이블 (타입별 행 분리)
-- result_type_cd 기준으로 사용자당 타입 1건씩 upsert
--
-- result_type_cd 목록:
--   KEY_FINDINGS             주요 발견사항
--   INVESTMENT_STYLE         나의 투자성향
--   RISK_ASSESSMENT          리스크 평가
--   PORTFOLIO_ALIGNMENT      포트폴리오 정합성
--   INVESTMENT_RECOMMENDATION 투자 추천
CREATE TABLE tbl_insight_result (
    result_id       BIGINT          AUTO_INCREMENT  PRIMARY KEY                         COMMENT '인사이트 결과 ID',
    user_id         VARCHAR(100)    NOT NULL                                            COMMENT '사용자 ID',
    result_type_cd  VARCHAR(50)     NOT NULL                                            COMMENT '인사이트 유형 코드',
    title           VARCHAR(200)    NOT NULL                                            COMMENT '섹션 제목',
    content         TEXT            NOT NULL                                            COMMENT '인사이트 내용',
    use_yn          CHAR(1)         NOT NULL        DEFAULT 'Y'                         COMMENT '사용여부',
    reg_dt          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP           COMMENT '등록일시',
    upd_dt          DATETIME        NULL            ON UPDATE CURRENT_TIMESTAMP         COMMENT '수정일시',

    UNIQUE KEY uk_insight_user_type (user_id, result_type_cd),
    INDEX      idx_insight_user_id  (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 인사이트 결과 (타입별 행 분리)';
