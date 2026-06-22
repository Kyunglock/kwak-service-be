package com.investment.analyzer.market_analyzer.api.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.investment.analyzer.market_analyzer.application.dto.market.MarketStatisticsSearchRequest;
import com.investment.analyzer.market_analyzer.application.service.market.MarketStatisticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

import kwak.common.application.dto.RokResponse;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/markets")
@RequiredArgsConstructor
public class MarketStatisticsController {

    final private MarketStatisticsService marketStatisticsService;

    @Operation(summary = "시장 데이터 조회", description = "시장 데이터를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "시장 데이터 조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RokResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("")
    public ResponseEntity<?> findAllMarket(@RequestBody @Valid MarketStatisticsSearchRequest searchRequest, Pageable pageable) {
        return ResponseUtil.success(marketStatisticsService.findAllMarket(searchRequest, pageable));
    }
}
