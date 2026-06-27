package com.investment.analyzer.market_analyzer.api.controller;

import com.investment.analyzer.market_analyzer.application.service.dividend.DividendHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "포트폴리오 배당", description = "포트폴리오 보유 종목 배당 이력 조회 API")
@RestController
@RequestMapping("/api/v1/dividends")
@RequiredArgsConstructor
public class PortfolioDividendController {

    private final DividendHistoryService dividendHistoryService;

    @Operation(summary = "포트폴리오 배당 이력 조회",
               description = "portfolioId에 속한 보유 종목의 최근 배당 이력을 종목코드별로 반환합니다")
    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<?> findRecentByPortfolioId(
            @PathVariable Long portfolioId,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseUtil.success(dividendHistoryService.findRecentByPortfolioId(portfolioId, limit));
    }
}
