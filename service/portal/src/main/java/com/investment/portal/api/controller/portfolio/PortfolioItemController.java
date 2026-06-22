package com.investment.portal.api.controller.portfolio;

import com.investment.portal.application.dto.portfolio.item.*;
import com.investment.portal.application.service.portfolio.PortfolioItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "포트폴리오 종목", description = "포트폴리오 종목 CRUD API")
@RestController
@RequestMapping("/api/v1/portfolio-items")
@RequiredArgsConstructor
public class PortfolioItemController {

    private final PortfolioItemService portfolioItemService;

    @Operation(summary = "종목 단건 조회", description = "항목ID로 포트폴리오 종목을 조회합니다")
    @GetMapping("/{itemId}")
    public ResponseEntity<?> getItem(
            @Parameter(description = "항목ID", example = "1")
            @PathVariable Long itemId) {

        PortfolioItemResponse response = portfolioItemService.getItem(itemId);
        if (response == null) {
            return ResponseUtil.notFound("해당 항목을 찾을 수 없습니다: " + itemId);
        }
        return ResponseUtil.success(response);
    }

    @Operation(summary = "포트폴리오별 종목 목록 조회", description = "포트폴리오ID로 종목 목록을 조회합니다")
    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<?> getItemsByPortfolio(
            @Parameter(description = "포트폴리오ID", example = "10")
            @PathVariable Long portfolioId) {

        List<PortfolioItemResponse> responses = portfolioItemService.getItemsByPortfolioId(portfolioId);
        return ResponseUtil.success(responses, "조회 성공 (" + responses.size() + "건)");
    }

    @Operation(summary = "종목 검색", description = "포트폴리오ID, 종목코드로 검색합니다")
    @GetMapping
    public ResponseEntity<?> searchItems(PortfolioItemSearchRequest request) {
        List<PortfolioItemResponse> responses = portfolioItemService.searchItems(request);
        return ResponseUtil.success(responses, "조회 성공 (" + responses.size() + "건)");
    }

    @Operation(summary = "종목 등록", description = "포트폴리오에 종목을 추가합니다")
    @PostMapping
    public ResponseEntity<?> addItem(@Valid @RequestBody PortfolioItemAddRequest request) {
        PortfolioItemResponse response = portfolioItemService.addItem(request);
        return ResponseUtil.created(response, "종목 등록 성공");
    }

    @Operation(summary = "종목 수정", description = "포트폴리오 종목 정보를 수정합니다")
    @PutMapping
    public ResponseEntity<?> modifyItem(@Valid @RequestBody PortfolioItemModRequest request) {
        PortfolioItemResponse response = portfolioItemService.modifyItem(request);
        return ResponseUtil.success(response, "종목 수정 성공");
    }

    @Operation(summary = "종목 삭제", description = "포트폴리오에서 종목을 삭제합니다 (논리 삭제)")
    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> removeItem(
            @Parameter(description = "항목ID", example = "1")
            @PathVariable Long itemId) {

        portfolioItemService.removeItem(itemId);
        return ResponseUtil.noContent();
    }
}
