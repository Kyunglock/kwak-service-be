package com.investment.portal.application.dto.portfolio;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "포트폴리오 추가 요청 DTO")
public record PortfolioAddRequest(

    @Schema(description = "포트폴리오명", example = "미국 주식 장기투자", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "포트폴리오명은 필수입니다")
    String portfolioNm,

    @Schema(description = "포트폴리오 설명", example = "기술주 중심의 장기 투자 포트폴리오")
    String portfolioDesc,

    @Schema(description = "기준통화", example = "USD")
    String baseCurrency
) {
}
