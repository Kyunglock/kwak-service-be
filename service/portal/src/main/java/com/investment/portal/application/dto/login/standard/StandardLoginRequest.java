package com.investment.portal.application.dto.login.standard;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "일반 로그인 요청")
public record StandardLoginRequest(

        @Schema(description = "이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "비밀번호", example = "Password1!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
