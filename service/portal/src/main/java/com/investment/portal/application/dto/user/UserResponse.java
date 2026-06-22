package com.investment.portal.application.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자 정보 응답 DTO")
public record UserResponse(
    
    @Schema(description = "사용자ID", example = "user-uuid-1234-5678")
    String userId,
    
    @Schema(description = "사용자명", example = "홍길동")
    String userNm,
    
    @Schema(description = "닉네임", example = "투자왕")
    String nickname,
    
    @Schema(description = "이메일", example = "user@example.com")
    String email,
    
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile/123.jpg")
    String profileImgUrl,
    
    @Schema(description = "사용여부", example = "Y")
    String useYn,
    
    @Schema(description = "등록일시", example = "2026-01-13T14:30:00")
    LocalDateTime regDt,
    
    @Schema(description = "수정일시", example = "2026-01-13T15:45:00")
    LocalDateTime updDt,
    
    @Schema(description = "마지막 로그인일시", example = "2026-01-13T16:20:00")
    LocalDateTime lastLoginDt
) {
}
