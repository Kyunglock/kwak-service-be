package com.investment.portal.api.controller.dividend;

import com.investment.portal.domain.entity.dividend.DividendHistory;
import com.investment.portal.domain.repository.dividend.DividendHistoryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "배당 이력", description = "포트폴리오 배당 이력 조회 API")
@RestController
@RequestMapping("/api/v1/dividends")
@RequiredArgsConstructor
public class DividendController {

    private final DividendHistoryMapper dividendHistoryMapper;

    @Operation(summary = "포트폴리오 배당 이력 조회",
               description = "portfolioId에 속한 보유 종목의 최근 배당 이력을 종목코드별로 반환합니다")
    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<?> findRecentByPortfolioId(
            @PathVariable Long portfolioId,
            @RequestParam(defaultValue = "4") int limit) {

        Map<String, List<DividendHistory>> result = dividendHistoryMapper
                .findRecentBatchByPortfolioId(portfolioId, limit)
                .stream()
                .collect(Collectors.groupingBy(DividendHistory::getStockCd));

        return ResponseUtil.success(result);
    }
}
