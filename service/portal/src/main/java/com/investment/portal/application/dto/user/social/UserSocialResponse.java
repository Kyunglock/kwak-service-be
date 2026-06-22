package com.investment.portal.application.dto.user.social;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자 소셜 로그인 연동 응답 DTO")
public record UserSocialResponse(
    
    @Schema(description = "소셜연동ID", example = "1")
    Long socialId,
    
    @Schema(description = "사용자ID", example = "user-uuid-1234-5678")
    String userId,
    
    @Schema(description = "소셜 제공자", example = "KAKAO", allowableValues = {"KAKAO", "NAVER", "GOOGLE"})
    String provider,
    
    @Schema(description = "소셜 제공자의 사용자 ID", example = "kakao-123456789")
    String providerUserId,
    
    @Schema(description = "연동일시", example = "2026-01-13T14:30:00")
    LocalDateTime connectedDt,
    
    @Schema(description = "마지막 로그인일시", example = "2026-01-13T16:20:00")
    LocalDateTime lastLoginDt,
    
    @Schema(description = "토큰 만료일시", example = "2026-02-13T14:30:00")
    LocalDateTime tokenExpiredDt,
    
    @Schema(description = "사용여부", example = "Y")
    String useYn
) {
}
