-- 관리자 역할 컬럼. ADMIN_USER_IDS env 하드코딩 대체.
-- 수동 실행 (V3~V6과 동일하게 Flyway 미사용)
ALTER TABLE tbl_user
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '역할(USER/ADMIN)' AFTER use_yn;

UPDATE tbl_user
SET role = 'ADMIN'
WHERE user_id = '60994513-8d21-4076-8d10-ef919f00e81a';
