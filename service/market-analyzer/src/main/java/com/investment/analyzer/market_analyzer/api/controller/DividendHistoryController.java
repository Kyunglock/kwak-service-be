package com.investment.analyzer.market_analyzer.api.controller;

import com.investment.analyzer.market_analyzer.application.service.dividend.DividendHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/markets/dividends")
@RequiredArgsConstructor
public class DividendHistoryController {

    private final DividendHistoryService dividendHistoryService;

    @Operation(summary = "배당 이력 전체 조회")
    @GetMapping("/{stockCd}")
    public ResponseEntity<?> findByStockCd(@PathVariable String stockCd) {
        return ResponseUtil.success(dividendHistoryService.findByStockCd(stockCd));
    }

    @Operation(summary = "최근 배당 이력 조회")
    @GetMapping("/{stockCd}/recent")
    public ResponseEntity<?> findRecentByStockCd(
            @PathVariable String stockCd,
            @RequestParam(defaultValue = "12") int limit) {
        return ResponseUtil.success(dividendHistoryService.findRecentByStockCd(stockCd, limit));
    }

    @Operation(summary = "여러 종목 최근 배당 이력 일괄 조회")
    @GetMapping("/recent/batch")
    public ResponseEntity<?> findRecentBatch(
            @RequestParam List<String> stockCds,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseUtil.success(dividendHistoryService.findRecentBatch(stockCds, limit));
    }
}
