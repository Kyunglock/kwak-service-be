package com.investment.portal.application.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "사용자 수정 요청 DTO")
public record UserModRequest(
    
    @Schema(description = "사용자ID", example = "user-uuid-1234-5678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "사용자ID는 필수입니다")
    String userId,
    
    @Schema(description = "사용자명", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "사용자명은 필수입니다")
    String userNm,
    
    @Schema(description = "닉네임", example = "투자왕")
    String nickname,
    
    @Schema(description = "이메일", example = "user@example.com")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    String email,
    
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile/123.jpg")
    String profileImgUrl
) {
}
