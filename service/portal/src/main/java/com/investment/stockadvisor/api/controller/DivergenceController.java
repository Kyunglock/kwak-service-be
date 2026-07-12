package com.investment.stockadvisor.api.controller;

import com.investment.stockadvisor.application.service.divergence.DivergenceDetectionService;
import com.investment.stockadvisor.application.service.divergence.DivergenceInterpretationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Divergence", description = "재무 이상 신호 탐지 API")
@RestController
@RequestMapping("/api/v1/divergences")
@RequiredArgsConstructor
public class DivergenceController {

    private final DivergenceDetectionService detectionService;
    private final DivergenceInterpretationService interpretationService;

    @Operation(summary = "종목별 Divergence 조회",
               description = "특정 종목의 전체 Divergence 탐지 결과를 조회합니다")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/stocks/{stockCd}")
    public ResponseEntity<?> findByStockCd(@PathVariable String stockCd) {
        return ResponseUtil.success(detectionService.findByStockCd(stockCd));
    }

    @Operation(summary = "배치 실행일별 Divergence 조회",
               description = "특정 배치 실행일에 탐지된 전체 종목 결과를 severity 내림차순으로 조회합니다")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/batch")
    public ResponseEntity<?> findByBatchRunDt(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate batchRunDt) {
        return ResponseUtil.success(detectionService.findByBatchRunDt(batchRunDt));
    }

    @Operation(summary = "종목별 Divergence LLM 해석 조회",
               description = "OpenAI 해석 결과를 조회합니다. Redis 캐시 히트 시 cached=true")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/stocks/{stockCd}/interpretations")
    public ResponseEntity<?> interpretByStockCd(@PathVariable String stockCd) {
        return ResponseUtil.success(interpretationService.interpretByStockCd(stockCd));
    }
}
