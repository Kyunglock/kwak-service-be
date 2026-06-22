package com.investment.portal.application.service.login;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "로그인 응답")
public record LoginResponse(

        @Schema(description = "JWT 액세스 토큰 (1시간 유효)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "리프레시 토큰 (7일 유효)", example = "550e8400-e29b-41d4-a716-446655440000")
        String refreshToken,

        @Schema(description = "사용자 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String userId,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "닉네임", example = "투자왕")
        String nickname,

        @Schema(description = "신규 사용자 여부", example = "false")
        boolean isNewUser
) {
}
