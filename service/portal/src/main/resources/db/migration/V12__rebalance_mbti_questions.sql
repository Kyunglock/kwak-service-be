-- V12: 문항 균형 재조정 — 성격 20(축당 5) : 투자 20(축당 5) = 총 40문항
-- (사용자 피드백: 일반 MBTI가 주인데 문항이 투자에 치중 — 기존 성격16:투자28)
-- ① V10에서 추가한 투자 보강 8문항(현재 번호 37~44) 제거 (응답·옵션 포함 — 테스트 응답만 존재)
-- ② 성격 4문항(축당 1) 신규 추가 → 번호 17~20
-- ③ 투자 문항 17~36 → 21~40 재배치

SET @sid = (SELECT survey_id FROM tbl_survey WHERE survey_type_code = 'RISK_PROFILE' AND use_yn = 'Y' LIMIT 1);

-- ① 투자 보강 8문항 제거 (답변 → 옵션 → 문항 순)
DELETE d FROM tbl_survey_answer d
  JOIN tbl_survey_question q ON d.question_id = q.question_id
 WHERE q.survey_id = @sid AND q.question_number BETWEEN 37 AND 44;

DELETE o FROM tbl_survey_option o
  JOIN tbl_survey_question q ON o.question_id = q.question_id
 WHERE q.survey_id = @sid AND q.question_number BETWEEN 37 AND 44;

DELETE FROM tbl_survey_question
 WHERE survey_id = @sid AND question_number BETWEEN 37 AND 44;

-- ③ 투자 17~36 → 21~40 (충돌 방지 오프셋 후 이동)
UPDATE tbl_survey_question SET question_number = question_number + 100
 WHERE survey_id = @sid AND question_number BETWEEN 17 AND 36;
UPDATE tbl_survey_question SET question_number = question_number - 96
 WHERE survey_id = @sid AND question_number BETWEEN 117 AND 136;

-- ② 성격 신규 4문항 (17~20, 축당 1 — score 높음 = 앞 글자)
INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 17, '오랜만에 생긴 휴가, 더 끌리는 쪽은?', 'SINGLE_CHOICE', 'EI', 17);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'집에서 혼자 재충전한다','1',1,25),(@q,'가까운 사람 한두 명과 조용히 보낸다','2',2,50),
(@q,'친구들과 근교 나들이를 간다','3',3,75),(@q,'여럿이 떠들썩한 여행을 떠난다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 18, '책을 고를 때 더 손이 가는 쪽은?', 'SINGLE_CHOICE', 'SN', 18);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'상상력을 자극하는 소설·아이디어','1',1,25),(@q,'생각할 거리를 주는 교양·에세이','2',2,50),
(@q,'경험담이 담긴 실용서','3',3,75),(@q,'바로 써먹는 매뉴얼·실전 가이드','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 19, '영화를 보고 나서 나는?', 'SINGLE_CHOICE', 'TF', 19);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'인물의 감정선이 여운으로 남는다','1',1,25),(@q,'메시지가 마음에 와닿았는지 돌아본다','2',2,50),
(@q,'전개가 설득력 있었는지 평가한다','3',3,75),(@q,'설정의 논리적 허점을 따져본다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 20, '나의 여행 스타일은?', 'SINGLE_CHOICE', 'JP', 20);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'발길 닿는 대로 즉흥적으로','1',1,25),(@q,'큰 동선만 정해 둔다','2',2,50),
(@q,'주요 일정은 미리 예약한다','3',3,75),(@q,'시간 단위 계획표를 만든다','4',4,100);

-- 표시 순서 동기화
UPDATE tbl_survey_question SET sort_order = question_number WHERE survey_id = @sid;

-- 설문 설명 갱신 (44 → 40문항)
UPDATE tbl_survey SET description = '성격 MBTI와 투자 MBTI를 한 번에 알아보는 통합 검사입니다. (40문항, 약 7분)'
 WHERE survey_id = @sid;
