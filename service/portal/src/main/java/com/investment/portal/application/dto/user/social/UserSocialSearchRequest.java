package com.investment.portal.application.dto.user.social;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 소셜 로그인 연동 검색 요청 DTO")
public record UserSocialSearchRequest(
    
    @Schema(description = "소셜연동ID", example = "1")
    Long socialId,
    
    @Schema(description = "사용자ID", example = "user-uuid-1234-5678")
    String userId,
    
    @Schema(description = "소셜 제공자", example = "KAKAO", allowableValues = {"KAKAO", "NAVER", "GOOGLE"})
    String provider
) {
}
