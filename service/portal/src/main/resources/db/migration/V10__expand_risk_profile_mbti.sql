-- V10: RISK_PROFILE 설문을 MBTI 통합 설문으로 확장 (문항 21~44)
-- 성격 4축(EI/SN/TF/JP) 각 4문항 + 투자 4축 각 2문항. 컷 62.5 기준, score 높음 = 앞 글자(E/S/T/J, G/R/L/D)

UPDATE tbl_survey SET survey_name = '나의 MBTI & 투자 성향 분석',
       description = '성격 MBTI와 투자 MBTI를 한 번에 알아보는 통합 검사입니다. (44문항, 약 8분)'
 WHERE survey_type_code = 'RISK_PROFILE' AND use_yn = 'Y';

SET @sid = (SELECT survey_id FROM tbl_survey WHERE survey_type_code = 'RISK_PROFILE' AND use_yn = 'Y' LIMIT 1);

-- ───────── EI (외향/내향) 21~24 ─────────
INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 21, '주말에 에너지를 얻는 방법은?', 'SINGLE_CHOICE', 'EI', 21);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'혼자만의 시간이 꼭 필요하다','1',1,25),(@q,'주로 혼자가 편하다','2',2,50),
(@q,'종종 사람들을 만나야 활력이 돈다','3',3,75),(@q,'사람들과 있을 때 에너지가 차오른다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 22, '새로운 모임에 갔을 때 나는?', 'SINGLE_CHOICE', 'EI', 22);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'구석에서 아는 사람을 기다린다','1',1,25),(@q,'말을 걸어오면 대화한다','2',2,50),
(@q,'몇몇에게 먼저 말을 건넨다','3',3,75),(@q,'처음 보는 사람들과도 금방 어울린다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 23, '고민이 생기면 나는?', 'SINGLE_CHOICE', 'EI', 23);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'혼자 정리한 후에야 이야기한다','1',1,25),(@q,'가까운 한 명에게만 털어놓는다','2',2,50),
(@q,'이야기하면서 생각이 정리된다','3',3,75),(@q,'여러 사람과 대화하며 답을 찾는다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 24, '전화가 오면 나는?', 'SINGLE_CHOICE', 'EI', 24);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'가능하면 문자로 대신한다','1',1,25),(@q,'필요한 통화만 짧게 한다','2',2,50),
(@q,'통화가 편하고 자연스럽다','3',3,75),(@q,'통화로 수다 떠는 게 즐겁다','4',4,100);

-- ───────── SN (감각/직관) 25~28 ─────────
INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 25, '새로운 일을 배울 때 나는?', 'SINGLE_CHOICE', 'SN', 25);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'큰 그림과 가능성부터 상상한다','1',1,25),(@q,'개념을 먼저 잡고 세부는 나중에 본다','2',2,50),
(@q,'예시를 보며 따라 한다','3',3,75),(@q,'구체적인 순서대로 차근차근 익힌다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 26, '대화할 때 나는?', 'SINGLE_CHOICE', 'SN', 26);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'아이디어와 미래 이야기가 즐겁다','1',1,25),(@q,'의미와 해석을 곁들여 말한다','2',2,50),
(@q,'실제 있었던 일 중심으로 말한다','3',3,75),(@q,'사실과 경험을 정확하게 전달한다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 27, '무언가를 볼 때 먼저 눈에 들어오는 것은?', 'SINGLE_CHOICE', 'SN', 27);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'이면에 숨은 의미와 연결고리','1',1,25),(@q,'전체적인 인상과 분위기','2',2,50),
(@q,'두드러진 특징 몇 가지','3',3,75),(@q,'구체적인 생김새와 세부 사항','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 28, '일할 때 더 편한 방식은?', 'SINGLE_CHOICE', 'SN', 28);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'새로운 방식을 실험한다','1',1,25),(@q,'개선 아이디어를 자주 떠올린다','2',2,50),
(@q,'검증된 방법을 조금씩 응용한다','3',3,75),(@q,'검증된 방법을 그대로 따른다','4',4,100);

-- ───────── TF (사고/감정) 29~32 ─────────
INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 29, '친구가 고민을 털어놓으면 나는?', 'SINGLE_CHOICE', 'TF', 29);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'우선 마음에 공감해 준다','1',1,25),(@q,'감정을 먼저 살핀 뒤 조언한다','2',2,50),
(@q,'상황을 정리해 준다','3',3,75),(@q,'해결책부터 제시한다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 30, '결정을 내릴 때 더 중요한 것은?', 'SINGLE_CHOICE', 'TF', 30);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'관련된 사람들의 마음','1',1,25),(@q,'관계에 미칠 영향','2',2,50),
(@q,'원칙과 형평성','3',3,75),(@q,'객관적 사실과 논리','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 31, '팀에서 갈등이 생기면 나는?', 'SINGLE_CHOICE', 'TF', 31);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'분위기부터 풀려고 한다','1',1,25),(@q,'서로의 감정을 중재한다','2',2,50),
(@q,'갈등의 원인을 분석한다','3',3,75),(@q,'옳고 그름을 기준으로 판단한다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 32, '피드백을 줄 때 나는?', 'SINGLE_CHOICE', 'TF', 32);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'아쉬운 점은 부드럽게 돌려 말한다','1',1,25),(@q,'좋은 점을 먼저 말한다','2',2,50),
(@q,'사실 위주로 담백하게 말한다','3',3,75),(@q,'개선점을 직설적으로 말한다','4',4,100);

-- ───────── JP (판단/인식) 33~36 ─────────
INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 33, '마감이 있는 일을 할 때 나는?', 'SINGLE_CHOICE', 'JP', 33);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'마감 직전에 몰아서 한다','1',1,25),(@q,'흐름에 맡기다 막판에 스퍼트한다','2',2,50),
(@q,'대략의 일정을 잡고 진행한다','3',3,75),(@q,'계획을 세워 미리 끝낸다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 34, '내 책상과 방 상태는?', 'SINGLE_CHOICE', 'JP', 34);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'어수선해도 어디 있는지는 안다','1',1,25),(@q,'필요할 때만 정리한다','2',2,50),
(@q,'대체로 정돈되어 있다','3',3,75),(@q,'항상 제자리에 정리되어 있다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 35, '갑작스러운 계획 변경이 생기면?', 'SINGLE_CHOICE', 'JP', 35);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'오히려 재밌다','1',1,25),(@q,'크게 개의치 않는다','2',2,50),
(@q,'조금 불편하다','3',3,75),(@q,'스트레스를 받는다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 36, '나의 하루 일과는?', 'SINGLE_CHOICE', 'JP', 36);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'그때그때 끌리는 대로 움직인다','1',1,25),(@q,'느슨한 우선순위만 둔다','2',2,50),
(@q,'할 일 목록을 만든다','3',3,75),(@q,'시간표대로 움직인다','4',4,100);

-- ───────── 투자 보강: 수익추구 37~38 ─────────
INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 37, '은행 예금 금리가 5%라면 나는?', 'SINGLE_CHOICE', '수익추구', 37);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'전액 예금한다','1',1,25),(@q,'대부분 예금하고 일부만 투자한다','2',2,50),
(@q,'그래도 주식 비중을 유지한다','3',3,75),(@q,'예금보다 높은 수익처를 찾아 나선다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 38, '주변에서 큰 수익을 냈다는 이야기를 들으면?', 'SINGLE_CHOICE', '수익추구', 38);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'나와는 상관없는 일이다','1',1,25),(@q,'부럽지만 내 방식을 지킨다','2',2,50),
(@q,'어떤 종목인지 찾아본다','3',3,75),(@q,'나도 기회를 잡으러 분석에 들어간다','4',4,100);

-- ───────── 투자 보강: 리스크허용 39~40 ─────────
INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 39, '원금의 30%를 잃을 수도 있는 고수익 상품이 있다면?', 'SINGLE_CHOICE', '리스크허용', 39);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'쳐다보지 않는다','1',1,25),(@q,'아주 소액만 시험한다','2',2,50),
(@q,'분석해 보고 일부 투자한다','3',3,75),(@q,'확신이 서면 크게 베팅한다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 40, '밤사이 보유 종목에 악재 뉴스가 나오면?', 'SINGLE_CHOICE', '리스크허용', 40);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'개장 전 매도 예약부터 건다','1',1,25),(@q,'일단 절반을 줄인다','2',2,50),
(@q,'사실을 확인한 뒤 판단한다','3',3,75),(@q,'과민반응으로 보고 매수 기회를 살핀다','4',4,100);

-- ───────── 투자 보강: 장기투자 41~42 ─────────
INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 41, '3년째 주가가 제자리인 우량주가 있다면?', 'SINGLE_CHOICE', '장기투자', 41);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'진작에 팔았을 것이다','1',1,25),(@q,'슬슬 갈아탈 준비를 한다','2',2,50),
(@q,'배당을 받으며 더 기다린다','3',3,75),(@q,'오히려 추가 매수를 고려한다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 42, '계좌 수익률은 얼마나 자주 확인하나요?', 'SINGLE_CHOICE', '장기투자', 42);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'하루에도 몇 번씩','1',1,25),(@q,'매일 한 번','2',2,50),
(@q,'일주일에 한 번','3',3,75),(@q,'한 달에 한 번 이하','4',4,100);

-- ───────── 투자 보강: 분산투자 43~44 ─────────
INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 43, '강한 확신이 드는 종목이 생기면?', 'SINGLE_CHOICE', '분산투자', 43);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'한 종목에 집중 투자한다','1',1,25),(@q,'자산의 절반까지 태운다','2',2,50),
(@q,'정해둔 비중까지만 산다','3',3,75),(@q,'확신이 있어도 분산 원칙을 지킨다','4',4,100);

INSERT INTO tbl_survey_question (survey_id, question_number, question_text, question_type_code, description, sort_order)
VALUES (@sid, 44, '내 포트폴리오의 섹터 구성은?', 'SINGLE_CHOICE', '분산투자', 44);
SET @q = LAST_INSERT_ID();
INSERT INTO tbl_survey_option (question_id, option_text, option_value, sort_order, score) VALUES
(@q,'한 섹터에 집중되어 있다','1',1,25),(@q,'두세 섹터 정도','2',2,50),
(@q,'여러 섹터에 걸쳐 있다','3',3,75),(@q,'섹터·지역·자산군까지 고르게 분산','4',4,100);
