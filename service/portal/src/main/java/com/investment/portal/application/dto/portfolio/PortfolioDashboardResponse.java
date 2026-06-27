package com.investment.portal.application.dto.portfolio;

import com.investment.portal.application.dto.history.transaction.TransactionHistoryResponse;
import com.investment.portal.application.dto.portfolio.item.PortfolioPositionDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "종목탭 대시보드 응답 DTO")
public record PortfolioDashboardResponse(

    @Schema(description = "내 포트폴리오 목록")
    List<PortfolioResponse> portfolios,

    @Schema(description = "기본 활성 포트폴리오ID", example = "1")
    Long activePortfolioId,

    @Schema(description = "활성 포트폴리오의 보유 포지션 (종목명·섹터 포함)")
    List<PortfolioPositionDto> positions,

    @Schema(description = "활성 포트폴리오의 거래 이력")
    List<TransactionHistoryResponse> transactions
) {
}
