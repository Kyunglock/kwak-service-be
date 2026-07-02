package com.investment.survey.api.controller;

import com.investment.survey.application.dto.response.SurveySubmitRequest;
import com.investment.survey.application.dto.response.SurveySubmitResponse;
import com.investment.survey.application.dto.response.SurveyWithMyResponse;
import com.investment.survey.application.dto.result.SurveyResultResponse;
import com.investment.survey.application.service.survey.SurveyResponseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwak.common.util.ResponseUtil;
import kwak.common.application.dto.PageResponse;
import kwak.common.application.dto.RokResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@Tag(name = "설문 응답", description = "설문 제출 및 결과 조회 API")
@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyResponseController {

    private final SurveyResponseService responseService;

    @Operation(summary = "설문 제출", description = "모든 문항에 대한 응답을 제출하고 결과를 받습니다")
    @PostMapping("/submit")
    public ResponseEntity<RokResponse<SurveySubmitResponse>> submitSurvey(
            @Valid @RequestBody SurveySubmitRequest request,
            @AuthenticationPrincipal String userId) {

        return ResponseUtil.created(responseService.submitSurvey(userId, request), "설문 제출 완료");
    }

    @Operation(summary = "내 설문 응답 이력 조회")
    @GetMapping("/responses")
    public ResponseEntity<RokResponse<List<SurveySubmitResponse>>> getMyResponses(@AuthenticationPrincipal String userId) {

        return ResponseUtil.success(responseService.getMyResponses(userId), "조회 성공");
    }

    @Operation(summary = "특정 설문 결과 조회", description = "가장 최신 활성 결과를 반환합니다")
    @GetMapping("/{surveyId}/results")
    public ResponseEntity<RokResponse<SurveyResultResponse>> getMyResult(
            @PathVariable Long surveyId,
            @AuthenticationPrincipal String userId) {

        SurveyResultResponse result = responseService.getMyResult(userId, surveyId);
        if (result == null) {
            return ResponseUtil.notFound("설문 결과가 없습니다");
        }
        return ResponseUtil.success(result, "조회 성공");
    }

    @Operation(summary = "내 전체 설문 결과 조회")
    @GetMapping("/results")
    public ResponseEntity<RokResponse<List<SurveyResultResponse>>> getMyResults(
            @AuthenticationPrincipal String userId) {

        return ResponseUtil.success(responseService.getMyResults(userId), "조회 성공");
    }

    @Operation(summary = "내 설문 응답 이력 조회")
    @GetMapping("/with-my-responses")
    public ResponseEntity<RokResponse<List<SurveyWithMyResponse>>> getSurveyWithMyResponses(@AuthenticationPrincipal String userId) {
        return ResponseUtil.success(responseService.getSurveyWithMyResponses(userId), "조회 성공");
    }

    @Operation(summary = "설문 목록 조회 (검색 + 페이징)")
    @GetMapping("/with-my-responses/paged")
    public ResponseEntity<RokResponse<PageResponse<SurveyWithMyResponse>>> getSurveyWithMyResponsesPaged(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "regDt,desc") String sort) {
        return ResponseUtil.success(
                responseService.getSurveyWithMyResponsesPaged(userId, keyword, page, size, sort), "조회 성공");
    }
}
