package com.investment.analyzer.market_analyzer.application.service.dividend;

import com.investment.analyzer.market_analyzer.application.dto.dividend.DividendHistoryResponse;

import java.util.List;
import java.util.Map;

public interface DividendHistoryService {
    List<DividendHistoryResponse> findByStockCd(String stockCd);
    List<DividendHistoryResponse> findRecentByStockCd(String stockCd, int limit);
    Map<String, List<DividendHistoryResponse>> findRecentBatch(List<String> stockCds, int limit);
}
