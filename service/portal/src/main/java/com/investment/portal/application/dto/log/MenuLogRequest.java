package com.investment.portal.application.dto.log;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 메뉴 이동 로그 적재 요청 */
public record MenuLogRequest(
        @NotBlank @Size(max = 40) String menuCd,
        @Size(max = 40) String prevMenuCd,
        @NotBlank @Size(max = 64) String sessionId
) {}
