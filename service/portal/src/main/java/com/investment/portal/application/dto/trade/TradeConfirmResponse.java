package com.investment.portal.application.dto.trade;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "초안 확정 결과")
public record TradeConfirmResponse(

        @Schema(description = "저장 성공 건수", example = "2")
        int savedCount,

        @Schema(description = "저장 실패 건수", example = "1")
        int failedCount,

        @Schema(description = "건별 결과")
        List<Result> results
) {
    @Schema(description = "건별 저장 결과")
    public record Result(
            @Schema(description = "초안 줄 번호") int lineNo,
            @Schema(description = "종목코드") String stockCd,
            @Schema(description = "저장 성공 여부") boolean saved,
            @Schema(description = "거래ID (성공 시)") Long transId,
            @Schema(description = "실패 사유 (실패 시)") String message) {}
}
