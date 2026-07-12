package com.investment.stockadvisor.application.dto.survey;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "시장 리스크 설문 비교 응답 DTO", description = "나 vs 전체 투자자 설문 비교 결과")
public record MarketRiskComparisonResponse(

        @Schema(description = "질문 순서", example = "1")
        Integer sortOrder,

        @Schema(description = "질문 내용", example = "기술주 변동성이 큰 장세에서 리스크를 감수하고 투자할 의향이 있나요?")
        String questionText,

        @Schema(description = "내 선택 텍스트", example = "제한적 투자 (중간 리스크)")
        String myOptionText,

        @Schema(description = "다수 선택 텍스트", example = "제한적 투자 (중간 리스크)")
        String majorityOptionText,

        @Schema(description = "일치 여부 (Y/N)", example = "Y")
        String matchYn,

        @Schema(description = "내 선택을 한 전체 비율 (%)", example = "100")
        BigDecimal myChoicePct
) {}
