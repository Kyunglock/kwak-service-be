package com.investment.portal.application.dto.user.social;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "사용자 소셜 로그인 연동 추가 요청 DTO")
public record UserSocialAddRequest(
    
    @Schema(description = "사용자ID", example = "user-uuid-1234-5678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "사용자ID는 필수입니다")
    String userId,
    
    @Schema(description = "소셜 제공자", example = "KAKAO", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"KAKAO", "NAVER", "GOOGLE"})
    @NotBlank(message = "소셜 제공자는 필수입니다")
    String provider,
    
    @Schema(description = "소셜 제공자의 사용자 ID", example = "kakao-123456789", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "소셜 제공자의 사용자 ID는 필수입니다")
    String providerUserId,
    
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,
    
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String refreshToken
) {
}
