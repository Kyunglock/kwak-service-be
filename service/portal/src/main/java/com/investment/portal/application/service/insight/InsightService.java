package com.investment.portal.application.service.insight;

import com.investment.portal.application.dto.insight.InsightResultResponse;

import java.util.List;

public interface InsightService {

    List<InsightResultResponse> getAllResults(String userId);

    InsightResultResponse getResultByType(String userId, String resultTypeCd);

    List<InsightResultResponse> buildAndSaveContext(String userId);
}
