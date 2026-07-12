package com.investment.stockadvisor.api.controller;

import com.investment.stockadvisor.application.dto.guru.GuruPortfolioSearchRequest;
import com.investment.stockadvisor.application.service.guru.GuruPortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.application.dto.RokResponse;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "구루 포트폴리오", description = "저명 투자자(구루) 포트폴리오 보유 현황 API")
@RestController
@RequestMapping("/api/v1/guru/portfolios")
@RequiredArgsConstructor
public class GuruPortfolioController {

    private final GuruPortfolioService guruPortfolioService;

    @Operation(summary = "구루 포트폴리오 목록 조회", description = "저명 투자자의 포트폴리오 보유 현황 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RokResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("")
    public ResponseEntity<?> findGuruPortfolioAll(GuruPortfolioSearchRequest searchRequest) {
        return ResponseUtil.success(guruPortfolioService.findGuruPortfolioAll(searchRequest));
    }

    @Operation(summary = "구루 ticker당 최근 매매 조회", description = "구루의 포트폴리오 ticker별 가장 최근 매매 활동을 조회합니다. guruCd 미입력 시 전체 구루 대상으로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RokResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/latest-activities")
    public ResponseEntity<?> findLatestActivityPerTicker(@RequestParam(required = false) String guruCd) {
        return ResponseUtil.success(guruPortfolioService.findLatestActivityPerTicker(guruCd));
    }

    @Operation(summary = "구루 포트폴리오 단건 조회", description = "ID로 구루 포트폴리오 항목을 단건 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RokResponse.class))),
        @ApiResponse(responseCode = "404", description = "데이터 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> findGuruPortfolioById(@PathVariable Long id) {
        return ResponseUtil.success(guruPortfolioService.findGuruPortfolioById(id));
    }
}
