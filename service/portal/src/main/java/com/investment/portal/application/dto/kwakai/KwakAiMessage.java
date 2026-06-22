package com.investment.portal.application.dto.kwakai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KwakAI 메시지")
public class KwakAiMessage {

    @NotBlank
    @Schema(description = "역할 (system/user/assistant)", example = "user")
    private String role;

    @NotBlank
    @Schema(description = "메시지 내용", example = "안녕하세요!")
    private String content;
}
