-- 일반 로그인을 위한 password 컬럼 추가
-- 소셜 로그인 전용 계정은 NULL 허용
ALTER TABLE tbl_user
    ADD COLUMN password VARCHAR(255) NULL COMMENT '비밀번호 (BCrypt 해시, 일반 로그인 전용)' AFTER email;
