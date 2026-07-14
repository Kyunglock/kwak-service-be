-- V11: 통합 MBTI 설문 문항 재배치 — 일반 MBTI가 주(主)이므로 성격 문항을 앞단으로
-- 성격(EI/SN/TF/JP) 21~36 → 1~16, 기존 투자 1~20 → 17~36, 신규 투자 37~44 → 유지
-- UNIQUE(survey_id, question_number) 때문에 +100 오프셋 후 재배치 (2단계)

SET @sid = (SELECT survey_id FROM tbl_survey WHERE survey_type_code = 'RISK_PROFILE' AND use_yn = 'Y' LIMIT 1);

-- 1단계: 전체 오프셋 (충돌 방지)
UPDATE tbl_survey_question SET question_number = question_number + 100 WHERE survey_id = @sid;

-- 2단계: 최종 번호
-- 성격 축 (오프셋 121~136) → 1~16 (축 내부 순서 유지: EI, SN, TF, JP)
UPDATE tbl_survey_question SET question_number = question_number - 120
 WHERE survey_id = @sid AND description IN ('EI', 'SN', 'TF', 'JP');

-- 기존 투자 문항 (오프셋 101~120) → 17~36
UPDATE tbl_survey_question SET question_number = question_number - 84
 WHERE survey_id = @sid AND question_number BETWEEN 101 AND 120;

-- 신규 투자 문항 (오프셋 137~144) → 37~44
UPDATE tbl_survey_question SET question_number = question_number - 100
 WHERE survey_id = @sid AND question_number BETWEEN 137 AND 144;

-- 표시 순서 동기화 (조회는 ORDER BY sort_order, question_number)
UPDATE tbl_survey_question SET sort_order = question_number WHERE survey_id = @sid;
