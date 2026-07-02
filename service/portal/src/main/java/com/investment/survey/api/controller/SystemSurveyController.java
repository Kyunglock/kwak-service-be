package com.investment.survey.api.controller;

import com.investment.survey.application.dto.survey.SurveyDetailResponse;
import com.investment.survey.application.dto.survey.SystemSurveyCreateRequest;
import com.investment.survey.application.service.survey.SystemSurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwak.common.application.dto.RokResponse;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "시스템 API", description = "내부 시스템 전용 API (뉴스 크롤러 연동)")
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemSurveyController {

    private final SystemSurveyService systemSurveyService;

    @Value("${system.api-key}")
    private String systemApiKey;

    @Operation(
            summary = "뉴스 기반 설문 자동 생성",
            description = "뉴스 크롤러가 Claude AI 분석 결과로 설문을 자동 생성합니다. X-System-Key 헤더 필수."
    )
    @PostMapping("/surveys")
    public ResponseEntity<RokResponse<SurveyDetailResponse>> createNewsSurvey(
            @RequestHeader(value = "X-System-Key", required = false) String apiKey,
            @Valid @RequestBody SystemSurveyCreateRequest request) {

        if (apiKey == null || !systemApiKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 시스템 키입니다");
        }

        return ResponseUtil.created(
                systemSurveyService.createSurveyWithQuestions(request),
                "뉴스 설문 생성 완료"
        );
    }
}
