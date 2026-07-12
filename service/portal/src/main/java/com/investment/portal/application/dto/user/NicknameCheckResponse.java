package com.investment.portal.application.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "닉네임 사용 가능 여부")
public record NicknameCheckResponse(boolean available, String reason) {

    public static NicknameCheckResponse ok() {
        return new NicknameCheckResponse(true, null);
    }

    public static NicknameCheckResponse rejected(String reason) {
        return new NicknameCheckResponse(false, reason);
    }
}
