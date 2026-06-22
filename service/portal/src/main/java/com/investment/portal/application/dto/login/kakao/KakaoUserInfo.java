package com.investment.portal.application.dto.login.kakao;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 카카오 사용자 정보 응답
 */
@Schema(description = "카카오 사용자 정보 응답")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoUserInfo(
        
        @Schema(description = "회원번호", example = "123456789")
        Long id,
        
        @Schema(description = "서비스에 연결 완료된 시각 (UTC)", example = "2024-01-01T00:00:00Z")
        String connectedAt,
        
        @Schema(description = "사용자 프로퍼티")
        Properties properties,
        
        @Schema(description = "카카오계정 정보")
        KakaoAccount kakaoAccount
) {
    
    @Schema(description = "사용자 프로퍼티")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Properties(
            @Schema(description = "닉네임", example = "홍길동")
            String nickname,
            
            @Schema(description = "프로필 이미지 URL", example = "http://k.kakaocdn.net/...jpg")
            String profileImage,
            
            @Schema(description = "썸네일 이미지 URL", example = "http://k.kakaocdn.net/...jpg")
            String thumbnailImage
    ) {}
    
    @Schema(description = "카카오계정 정보")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record KakaoAccount(
            @Schema(description = "프로필 닉네임 동의 필요 여부", example = "false")
            Boolean profileNicknameNeedsAgreement,
            
            @Schema(description = "프로필 이미지 동의 필요 여부", example = "false")
            Boolean profileImageNeedsAgreement,
            
            @Schema(description = "프로필 정보")
            Profile profile,
            
            @Schema(description = "이메일 보유 여부", example = "true")
            Boolean hasEmail,
            
            @Schema(description = "이메일 동의 필요 여부", example = "false")
            Boolean emailNeedsAgreement,
            
            @Schema(description = "이메일 유효 여부", example = "true")
            Boolean isEmailValid,
            
            @Schema(description = "이메일 인증 여부", example = "true")
            Boolean isEmailVerified,
            
            @Schema(description = "카카오계정 이메일", example = "user@example.com")
            String email
    ) {}
    
    @Schema(description = "프로필 정보")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Profile(
            @Schema(description = "닉네임", example = "홍길동")
            String nickname,
            
            @Schema(description = "썸네일 이미지 URL", example = "http://k.kakaocdn.net/...jpg")
            String thumbnailImageUrl,
            
            @Schema(description = "프로필 이미지 URL", example = "http://k.kakaocdn.net/...jpg")
            String profileImageUrl,
            
            @Schema(description = "프로필 이미지 기본 이미지 여부", example = "false")
            Boolean isDefaultImage
    ) {}
}