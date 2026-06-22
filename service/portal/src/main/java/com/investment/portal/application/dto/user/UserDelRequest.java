package com.investment.portal.application.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "사용자 삭제 요청 DTO")
public record UserDelRequest(
    
    @Schema(description = "사용자ID", example = "user-uuid-1234-5678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "사용자ID는 필수입니다")
    String userId
) {
}
