package com.investment.portal.application.dto.trade;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "자연어 매매 기록 입력 요청")
public record TradeCaptureTextRequest(

        @Schema(description = "포트폴리오ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "포트폴리오ID는 필수입니다")
        Long portfolioId,

        @Schema(description = "사용자가 입력한 문장", example = "어제 애플 10주를 230달러에 샀어",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "입력 내용은 필수입니다")
        @Size(max = 2000, message = "입력은 2000자를 넘을 수 없습니다")
        String text
) {}
