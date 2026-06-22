package com.investment.analyzer.market_analyzer.application.service.dividend;

import com.investment.analyzer.market_analyzer.application.dto.dividend.DividendHistoryResponse;
import com.investment.analyzer.market_analyzer.domain.repository.dividend.DividendHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DividendHistoryServiceImpl implements DividendHistoryService {

    private final DividendHistoryMapper dividendHistoryMapper;

    @Override
    public List<DividendHistoryResponse> findByStockCd(String stockCd) {
        return dividendHistoryMapper.findByStockCd(stockCd).stream()
                .map(DividendHistoryResponse::from)
                .toList();
    }

    @Override
    public List<DividendHistoryResponse> findRecentByStockCd(String stockCd, int limit) {
        return dividendHistoryMapper.findRecentByStockCd(stockCd, limit).stream()
                .map(DividendHistoryResponse::from)
                .toList();
    }

    @Override
    public Map<String, List<DividendHistoryResponse>> findRecentBatch(List<String> stockCds, int limit) {
        return dividendHistoryMapper.findRecentBatchByStockCds(stockCds, limit).stream()
                .map(DividendHistoryResponse::from)
                .collect(Collectors.groupingBy(DividendHistoryResponse::getStockCd));
    }
}
