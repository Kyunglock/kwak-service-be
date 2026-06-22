package com.investment.survey.api.controller;

import com.investment.survey.application.dto.survey.SurveyStatsResponse;
import com.investment.survey.application.service.survey.SurveyStatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.util.ResponseUtil;
import kwak.common.application.dto.RokResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@Tag(name = "설문 통계", description = "설문 통계 조회 API")
@RestController
@RequestMapping("/api/v1/surveys-stats")
@RequiredArgsConstructor
public class SurveyStatsController {
    private final SurveyStatsService statsService;

    @Operation(summary = "설문 제출", description = "모든 문항에 대한 응답을 제출하고 결과를 받습니다")
    @GetMapping("")
    public ResponseEntity<RokResponse<List<SurveyStatsResponse>>> getStatsResponses(@AuthenticationPrincipal String userId) {

        return ResponseUtil.success(statsService.getSurveyStatsResponses(userId), "조회 성공");
    }
  
}
