package com.investment.portal.application.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 사용자 정보")
public record MeResponse(String userId, String nickname, boolean isAdmin) {
}
