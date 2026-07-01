package com.investment.portal.application.service.insight;

import com.investment.portal.application.dto.insight.InsightResultResponse;

import java.util.List;

public interface InsightService {

    List<InsightResultResponse> getAllResults(String userId);

    InsightResultResponse getResultByType(String userId, String resultTypeCd);

    /** 비동기 빌드 트리거. 락 획득 시 "PROCESSING", 이미 진행 중이면 "ALREADY_PROCESSING". */
    String requestBuild(String userId);

    /** 동기 빌드 실행 (Kafka 컨슈머가 호출). */
    void executeBuild(String userId);

    /** 투자 MBTI(STOCK_MBTI)만 즉시 동기 생성 (설문 점수 기반, LLM 불필요). */
    InsightResultResponse generateStockMbti(String userId);
}
