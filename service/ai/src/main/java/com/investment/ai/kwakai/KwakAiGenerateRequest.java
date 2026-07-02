package com.investment.ai.kwakai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "KwakAI 텍스트 생성 요청")
public class KwakAiGenerateRequest {

    @Schema(description = "사용할 모델명 (비워두면 기본 모델 사용)")
    private String model;

    @NotBlank
    @Schema(description = "프롬프트", example = "하늘은 왜 파란색인가요?")
    private String prompt;
}
