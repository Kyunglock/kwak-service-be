package com.investment.portal.application.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 검색 요청 DTO")
public record UserSearchRequest(
    
    @Schema(description = "사용자ID", example = "user-uuid-1234-5678")
    String userId,
    
    @Schema(description = "이메일", example = "user@example.com")
    String email,
    
    @Schema(description = "닉네임", example = "투자왕")
    String nickname
) {
}