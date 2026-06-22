package com.investment.portal.api.controller.insight;

import com.investment.portal.application.dto.insight.InsightResultResponse;
import com.investment.portal.application.service.insight.InsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "RAG 인사이트", description = "설문 결과 + 포트폴리오 데이터 기반 RAG 인사이트 결과 API")
@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
public class RagController {

    private final InsightService insightService;

    @Operation(
        summary = "인사이트 전체 결과 조회",
        description = "DB에 저장된 사용자의 모든 타입 인사이트 결과를 조회합니다."
    )
    @GetMapping("/results")
    public ResponseEntity<?> getAllInsightResults(@AuthenticationPrincipal String userId) {
        List<InsightResultResponse> results = insightService.getAllResults(userId);
        return ResponseUtil.success(results, "인사이트 결과 조회 성공 (" + results.size() + "건)");
    }

    @Operation(
        summary = "인사이트 타입별 결과 조회",
        description = "특정 타입(KEY_FINDINGS, INVESTMENT_STYLE, RISK_ASSESSMENT, PORTFOLIO_ALIGNMENT, INVESTMENT_RECOMMENDATION)의 인사이트 결과를 조회합니다."
    )
    @GetMapping("/results/{resultTypeCd}")
    public ResponseEntity<?> getInsightResultByType(
            @AuthenticationPrincipal String userId,
            @Parameter(description = "인사이트 유형 코드", example = "KEY_FINDINGS")
            @PathVariable String resultTypeCd) {

        InsightResultResponse result = insightService.getResultByType(userId, resultTypeCd);
        if (result == null) {
            return ResponseUtil.success(null, "해당 타입의 인사이트 결과가 없습니다.");
        }
        return ResponseUtil.success(result, "인사이트 결과 조회 성공");
    }

    @Operation(
        summary = "인사이트 결과 생성",
        description = "설문 결과와 포트폴리오 데이터를 기반으로 RAG 컨텍스트를 재구성하여 타입별 인사이트를 생성/갱신합니다."
    )
    @PostMapping("/build-context")
    public ResponseEntity<?> buildContext(@AuthenticationPrincipal String userId) {
        List<InsightResultResponse> results = insightService.buildAndSaveContext(userId);
        return ResponseUtil.success(results, "인사이트 결과 생성 성공 (" + results.size() + "건)");
    }
}
