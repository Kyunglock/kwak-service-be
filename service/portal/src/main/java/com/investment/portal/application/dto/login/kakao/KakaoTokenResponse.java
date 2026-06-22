package com.investment.portal.application.dto.login.kakao;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 카카오 OAuth 토큰 응답
 */
@Schema(description = "카카오 OAuth 토큰 응답")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoTokenResponse(
        
        @Schema(description = "액세스 토큰", example = "xxxxxxxxxxxxxxxxxxxxxxxxx")
        String accessToken,
        
        @Schema(description = "토큰 타입", example = "bearer")
        String tokenType,
        
        @Schema(description = "리프레시 토큰", example = "xxxxxxxxxxxxxxxxxxxxxxxxx")
        String refreshToken,
        
        @Schema(description = "액세스 토큰 만료 시간(초)", example = "21599")
        Integer expiresIn,
        
        @Schema(description = "리프레시 토큰 만료 시간(초)", example = "5183999")
        Integer refreshTokenExpiresIn,
        
        @Schema(description = "인증된 사용자의 정보 조회 권한 범위", example = "profile_nickname profile_image")
        String scope
) {
}