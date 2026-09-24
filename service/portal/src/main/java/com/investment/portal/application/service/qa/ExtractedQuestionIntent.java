package com.investment.portal.application.service.qa;

/**
 * LLM이 질문에서 뽑아낸 원시 의도. 매매기록 추출의 ExtractedTrade 와 같은 위치의
 * 개념 — 아직 검증되지 않았다. stockName 은 StockResolver 가, questionType 은
 * {@link QuestionType#fromRaw}가 각각 확정/검증한다.
 */
record ExtractedQuestionIntent(
        String questionType,
        String stockName,
        String periodHint
) {}
