package com.investment.portal.application.dto.trade;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "매매 기록 초안 응답. 이 시점에는 아직 아무것도 저장되지 않았다.")
public record TradeDraftResponse(

        @Schema(description = "초안ID. 확정 요청에 그대로 넘긴다", example = "3f9a1c2e-...")
        String draftId,

        @Schema(description = "기록될 포트폴리오ID", example = "10")
        Long portfolioId,

        @Schema(description = "추출된 항목들")
        List<TradeDraftItem> items,

        @Schema(description = "바로 저장 가능한 건수", example = "2")
        int readyCount,

        @Schema(description = "사용자 확인이 필요한 건수", example = "1")
        int needsReviewCount,

        @Schema(description = "사용자에게 보여줄 안내 문구")
        String notice
) {}
