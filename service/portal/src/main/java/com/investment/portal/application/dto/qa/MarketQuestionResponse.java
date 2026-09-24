package com.investment.portal.application.dto.qa;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시황 질문 응답. answer 는 자연어 서술(JSON 아님)이다.")
public record MarketQuestionResponse(

        @Schema(description = "판단된 질문 유형", example = "MAX_DROP_DAY")
        String questionType,

        @Schema(description = "해석된 종목명", example = "애플")
        String stockNm,

        @Schema(description = "자연어 답변")
        String answer
) {}
