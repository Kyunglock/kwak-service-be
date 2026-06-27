package com.investment.portal.api.controller.portfolio;

import com.investment.portal.application.dto.portfolio.*;
import com.investment.portal.application.service.portfolio.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "포트폴리오", description = "포트폴리오 CRUD API")
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(summary = "내 포트폴리오 목록 조회", description = "로그인한 사용자의 포트폴리오 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<?> getMyPortfolios(@AuthenticationPrincipal String userId) {
        List<PortfolioResponse> responses = portfolioService.getMyPortfolios(userId);
        return ResponseUtil.success(responses, "조회 성공 (" + responses.size() + "건)");
    }

    @Operation(summary = "종목탭 대시보드", description = "탭 진입 시 포트폴리오 목록·포지션·거래 이력을 한 번에 반환합니다")
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal String userId) {
        return ResponseUtil.success(portfolioService.getDashboard(userId));
    }

    @Operation(summary = "포트폴리오 상세", description = "포트폴리오 전환 시 포지션·거래 이력을 반환합니다")
    @GetMapping("/{portfolioId}/detail")
    public ResponseEntity<?> getDetail(
            @Parameter(description = "포트폴리오ID", example = "1")
            @PathVariable Long portfolioId) {
        return ResponseUtil.success(portfolioService.getDetail(portfolioId));
    }

    @Operation(summary = "포트폴리오 단건 조회", description = "포트폴리오ID로 조회합니다")
    @GetMapping("/{portfolioId}")
    public ResponseEntity<?> getPortfolio(
            @Parameter(description = "포트폴리오ID", example = "1")
            @PathVariable Long portfolioId) {

        PortfolioResponse response = portfolioService.getPortfolio(portfolioId);
        if (response == null) {
            return ResponseUtil.notFound("해당 포트폴리오를 찾을 수 없습니다: " + portfolioId);
        }
        return ResponseUtil.success(response);
    }

    @Operation(summary = "포트폴리오 등록", description = "새 포트폴리오를 등록합니다 (userId는 JWT에서 자동 추출)")
    @PostMapping
    public ResponseEntity<?> addPortfolio(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PortfolioAddRequest request) {

        PortfolioResponse response = portfolioService.addPortfolio(userId, request);
        return ResponseUtil.created(response, "포트폴리오 등록 성공");
    }

    @Operation(summary = "포트폴리오 수정", description = "포트폴리오 정보를 수정합니다")
    @PutMapping
    public ResponseEntity<?> modifyPortfolio(@Valid @RequestBody PortfolioModRequest request) {
        PortfolioResponse response = portfolioService.modifyPortfolio(request);
        return ResponseUtil.success(response, "포트폴리오 수정 성공");
    }

    @Operation(summary = "포트폴리오 삭제", description = "포트폴리오를 삭제합니다 (논리 삭제)")
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<?> removePortfolio(
            @Parameter(description = "포트폴리오ID", example = "1")
            @PathVariable Long portfolioId) {

        portfolioService.removePortfolio(portfolioId);
        return ResponseUtil.noContent();
    }
}
