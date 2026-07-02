package com.investment.survey.api.controller;

import com.investment.survey.application.dto.question.QuestionAddRequest;
import com.investment.survey.application.dto.question.QuestionResponse;
import com.investment.survey.application.dto.survey.*;
import com.investment.survey.application.service.survey.SurveyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwak.common.util.ResponseUtil;
import kwak.common.application.dto.RokResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "설문 관리", description = "설문/문항/선택지 CRUD API")
@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @Operation(summary = "설문 목록 조회")
    @GetMapping
    public ResponseEntity<RokResponse<List<SurveyResponse>>> getSurveys(
            @Parameter(description = "설문 유형 코드 필터") @RequestParam(required = false) String surveyTypeCode) {

        List<SurveyResponse> surveys = surveyTypeCode != null
                ? surveyService.getSurveysByType(surveyTypeCode)
                : surveyService.getSurveys();

        return ResponseUtil.success(surveys, "조회 성공 (" + surveys.size() + "건)");
    }

    @Operation(summary = "설문 상세 조회", description = "문항 + 선택지 포함")
    @GetMapping("/{surveyId}")
    public ResponseEntity<RokResponse<SurveyDetailResponse>> getSurveyDetail(
            @PathVariable Long surveyId) {

        return ResponseUtil.success(surveyService.getSurveyDetail(surveyId), "조회 성공");
    }

    @Operation(summary = "설문 등록")
    @PostMapping
    public ResponseEntity<RokResponse<SurveyResponse>> addSurvey(
            @Valid @RequestBody SurveyAddRequest request) {

        return ResponseUtil.created(surveyService.addSurvey(request), "설문 등록 성공");
    }

    @Operation(summary = "설문 수정")
    @PutMapping("/{surveyId}")
    public ResponseEntity<RokResponse<SurveyResponse>> modifySurvey(
            @PathVariable Long surveyId,
            @Valid @RequestBody SurveyModRequest request) {

        return ResponseUtil.success(surveyService.modifySurvey(request), "설문 수정 성공");
    }

    @Operation(summary = "설문 삭제")
    @DeleteMapping("/{surveyId}")
    public ResponseEntity<?> removeSurvey(@PathVariable Long surveyId) {
        surveyService.removeSurvey(surveyId);
        return ResponseUtil.noContent();
    }

    // === 문항 관리 ===

    @Operation(summary = "문항 목록 조회", description = "선택지 포함")
    @GetMapping("/{surveyId}/questions")
    public ResponseEntity<RokResponse<List<QuestionResponse>>> getQuestions(
            @PathVariable Long surveyId) {

        return ResponseUtil.success(surveyService.getQuestions(surveyId), "조회 성공");
    }

    @Operation(summary = "문항 등록", description = "선택지를 포함하여 등록")
    @PostMapping("/{surveyId}/questions")
    public ResponseEntity<RokResponse<QuestionResponse>> addQuestion(
            @PathVariable Long surveyId,
            @Valid @RequestBody QuestionAddRequest request) {

        return ResponseUtil.created(surveyService.addQuestion(surveyId, request), "문항 등록 성공");
    }

    @Operation(summary = "문항 삭제", description = "선택지도 함께 삭제")
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<?> removeQuestion(@PathVariable Long questionId) {
        surveyService.removeQuestion(questionId);
        return ResponseUtil.noContent();
    }

    @Operation(summary = "제출된 설문 상세 조회", description = "문항 + 선택지 + 제출 답변 포함")
    @GetMapping("/{surveyId}/response/{responseId}")
    public ResponseEntity<RokResponse<SurveyAnswerResponse>> getSurveyResponseDetail(
            @PathVariable Long surveyId,
            @PathVariable Long responseId,
            @AuthenticationPrincipal String userId) {

        return ResponseUtil.success(surveyService.getSurveyResponseDetail(surveyId, responseId, userId), "조회 성공");
    }

    @Operation(summary = "설문 옵션 응답 상세 통계 조회")
    @GetMapping("/{surveyId}/response/{responseId}/options-stats")
    public ResponseEntity<RokResponse<SurveyOptionStatResponse>> getOptionStats(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Parameter(description = "응답 ID") @PathVariable Long responseId,
            @AuthenticationPrincipal String userId) {

        return ResponseUtil.success(surveyService.getSurveyResponseStatsDetail(surveyId, responseId, userId), "조회 성공");
    }
}
