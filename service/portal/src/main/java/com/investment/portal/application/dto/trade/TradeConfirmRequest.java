package com.investment.portal.application.dto.trade;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "초안 확정 요청. 사용자가 화면에서 수정한 최종값을 담는다.")
public record TradeConfirmRequest(

        @Schema(description = "초안ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "초안ID는 필수입니다")
        String draftId,

        @Schema(description = "저장할 항목들", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "저장할 항목이 없습니다")
        @Valid
        List<Item> items
) {
    @Schema(description = "확정할 매매 한 건")
    public record Item(

            @Schema(description = "초안 줄 번호", example = "1")
            int lineNo,

            @Schema(description = "종목코드", example = "AAPL", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "종목코드는 필수입니다")
            String stockCd,

            @Schema(description = "거래유형", example = "BUY", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "거래유형은 필수입니다")
            String transType,

            @Schema(description = "거래일자", example = "2026-09-18", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "거래일자는 필수입니다")
            LocalDate transDt,

            @Schema(description = "수량", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "수량은 필수입니다")
            BigDecimal qty,

            @Schema(description = "단가", example = "230.15", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "단가는 필수입니다")
            BigDecimal price,

            @Schema(description = "통화", example = "USD")
            String currency,

            @Schema(description = "메모")
            String memo
    ) {}
}
