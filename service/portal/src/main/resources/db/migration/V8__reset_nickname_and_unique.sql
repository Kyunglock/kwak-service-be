-- 닉네임 전면 리셋(게스트 제외) + 유니크 제약. 수동 실행 (V3~V7과 동일하게 Flyway 미사용)
-- 기존 닉네임은 전부 자동 부여였으므로 유저가 직접 다시 정한다. NULL은 유니크 인덱스에 걸리지 않는다.
UPDATE tbl_user
SET nickname = NULL
WHERE email IS NULL
   OR email NOT LIKE 'guest\_%@guest.local';

CREATE UNIQUE INDEX uk_tbl_user_nickname ON tbl_user (nickname);
