package com.investment.portal.application.service.qa;

import com.investment.portal.application.dto.qa.MarketQuestionRequest;
import com.investment.portal.application.dto.qa.MarketQuestionResponse;

/**
 * 자유 질문(고정 5개 유형)에 DB 주가·배당 이력과 뉴스를 근거로 자연어로 답한다.
 *
 * <p>매매기록 추출(TradeCaptureService)과 마찬가지로 로컬 LLM은 "의도 추출"과
 * "서술"에만 쓴다 — 실제 수치 계산은 전부 결정적인 코드/쿼리가 한다.
 */
public interface MarketQuestionService {
    MarketQuestionResponse ask(MarketQuestionRequest request);
}
