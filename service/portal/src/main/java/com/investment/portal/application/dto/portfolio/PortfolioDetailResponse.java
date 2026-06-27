package com.investment.portal.application.dto.portfolio;

import com.investment.portal.application.dto.history.transaction.TransactionHistoryResponse;
import com.investment.portal.application.dto.portfolio.item.PortfolioPositionDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "포트폴리오 상세 응답 DTO")
public record PortfolioDetailResponse(

    @Schema(description = "포트폴리오ID", example = "2")
    Long portfolioId,

    @Schema(description = "보유 포지션 (종목명·섹터 포함)")
    List<PortfolioPositionDto> positions,

    @Schema(description = "거래 이력")
    List<TransactionHistoryResponse> transactions
) {
}
