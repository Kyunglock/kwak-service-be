package com.investment.stockadvisor.api.controller;

import com.investment.stockadvisor.application.service.survey.UserSurveyService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 설문 결과", description = "사용자 투자 성향 및 시장 심리 설문 결과 조회 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserSurveyController {

    private final UserSurveyService userSurveyService;

    @Operation(
            summary = "사용자 설문 결과 조회",
            description = "사용자의 투자 성향(RISK_PROFILE) 및 시장 심리(MARKET_SENTIMENT) 설문 응답 결과를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RokResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/risk-profile")
    public ResponseEntity<?> findUserRiskProfileResults(@AuthenticationPrincipal String userId) {
        return ResponseUtil.success(userSurveyService.findUserRiskProfileResults(userId));
    }

    @Operation(
            summary = "사용자 선호 섹터 조회",
            description = "전체 사용자 포트폴리오 종목 수 기준으로 섹터 선호 비중(%)을 집계하여 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RokResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/preferred-sectors")
    public ResponseEntity<?> findUserPreferredSectors() {
        return ResponseUtil.success(userSurveyService.findUserPreferredSectors());
    }

    @Operation(
            summary = "나 vs 전체 투자자 시장 리스크 설문 비교 조회",
            description = "MARKET_RISK_SURVEY 기준으로 내 선택 / 다수 선택 / 일치 여부 / 내 선택 비율을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RokResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/market-risk-comparison")
    public ResponseEntity<?> findMarketRiskComparison(@AuthenticationPrincipal String userId) {
        return ResponseUtil.success(userSurveyService.findMarketRiskComparison(userId));
    }
}
