package com.investment.portal.application.dto.user.social;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "사용자 소셜 로그인 연동 수정 요청 DTO")
public record UserSocialModRequest(
    
    @Schema(description = "소셜연동ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "소셜연동ID는 필수입니다")
    Long socialId,
    
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,
    
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String refreshToken,
    
    @Schema(description = "토큰 만료일시", example = "2026-02-13T14:30:00")
    LocalDateTime tokenExpiredDt
) {
}
