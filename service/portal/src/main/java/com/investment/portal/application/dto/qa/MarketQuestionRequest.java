package com.investment.portal.application.dto.qa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "시황 질문 요청")
public record MarketQuestionRequest(

        @Schema(description = "사용자가 입력한 질문", example = "올해 애플 가장 많이 하락했던 날 무슨 일이 있었는지 알려줘",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "질문 내용은 필수입니다")
        @Size(max = 500, message = "질문은 500자를 넘을 수 없습니다")
        String text
) {}
