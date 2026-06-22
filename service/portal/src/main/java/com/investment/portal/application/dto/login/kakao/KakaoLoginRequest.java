package com.investment.portal.application.dto.login.kakao;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 로그인 요청")
public record KakaoLoginRequest(
        
        @Schema(description = "카카오 액세스 토큰", example = "xxxxxxxxxxxxxxxxxxxxxxxxx")
        @NotBlank(message = "액세스 토큰은 필수입니다")
        String accessToken
) {
}
