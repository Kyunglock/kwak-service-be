package com.investment.portal.api.controller.history;

import com.investment.portal.application.dto.history.transaction.*;
import com.investment.portal.application.event.ActivityEvent;
import com.investment.portal.application.service.history.TransactionHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "거래 이력", description = "거래 이력 CRUD API")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionHistoryController {

    private final TransactionHistoryService transactionHistoryService;
    private final ApplicationEventPublisher eventPublisher;

    @Operation(summary = "거래 단건 조회", description = "거래ID로 조회합니다")
    @GetMapping("/{transId}")
    public ResponseEntity<?> getTransaction(
            @Parameter(description = "거래ID", example = "1")
            @PathVariable Long transId) {

        TransactionHistoryResponse response = transactionHistoryService.getTransaction(transId);
        if (response == null) {
            return ResponseUtil.notFound("해당 거래를 찾을 수 없습니다: " + transId);
        }
        return ResponseUtil.success(response);
    }

    @Operation(summary = "포트폴리오별 거래 이력 조회", description = "포트폴리오ID로 거래 이력을 조회합니다")
    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<?> getTransactionsByPortfolio(
            @Parameter(description = "포트폴리오ID", example = "10")
            @PathVariable Long portfolioId) {

        List<TransactionHistoryResponse> responses = transactionHistoryService.getTransactionsByPortfolioId(portfolioId);
        return ResponseUtil.success(responses, "조회 성공 (" + responses.size() + "건)");
    }

    @Operation(summary = "포트폴리오+종목별 거래 이력 조회", description = "포트폴리오ID와 종목코드로 거래 이력을 조회합니다")
    @GetMapping("/portfolio/{portfolioId}/stock/{stockCd}")
    public ResponseEntity<?> getTransactionsByPortfolioAndStock(
            @Parameter(description = "포트폴리오ID", example = "10")
            @PathVariable Long portfolioId,
            @Parameter(description = "종목코드", example = "AAPL")
            @PathVariable String stockCd) {

        List<TransactionHistoryResponse> responses =
                transactionHistoryService.getTransactionsByPortfolioIdAndStockCd(portfolioId, stockCd);
        return ResponseUtil.success(responses, "조회 성공 (" + responses.size() + "건)");
    }

    @Operation(summary = "거래 등록", description = "거래 이력을 등록합니다 (amount = qty * price 자동 계산)")
    @PostMapping
    public ResponseEntity<?> addTransaction(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody TransactionHistoryAddRequest request) {
        TransactionHistoryResponse response = transactionHistoryService.addTransaction(request);

        String action = "SELL".equals(request.transType()) ? "TRADE_SELL" : "TRADE_BUY";
        eventPublisher.publishEvent(ActivityEvent.of(
                userId, action, "STOCK", request.stockCd(),
                request.stockCd() + " " + request.qty() + "주"));

        return ResponseUtil.created(response, "거래 등록 성공");
    }

    @Operation(summary = "거래 수정", description = "거래 이력을 수정합니다")
    @PutMapping
    public ResponseEntity<?> modifyTransaction(@Valid @RequestBody TransactionHistoryModRequest request) {
        TransactionHistoryResponse response = transactionHistoryService.modifyTransaction(request);
        return ResponseUtil.success(response, "거래 수정 성공");
    }

    @Operation(summary = "거래 삭제", description = "거래 이력을 삭제합니다 (물리 삭제)")
    @DeleteMapping("/{transId}")
    public ResponseEntity<?> removeTransaction(
            @Parameter(description = "거래ID", example = "1")
            @PathVariable Long transId) {

        transactionHistoryService.removeTransaction(transId);
        return ResponseUtil.noContent();
    }
}
