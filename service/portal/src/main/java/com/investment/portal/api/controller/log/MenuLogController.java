package com.investment.portal.api.controller.log;

import com.investment.portal.application.dto.log.MenuLogRequest;
import com.investment.portal.application.service.log.MenuLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "메뉴 로그", description = "메뉴 이동 로그 적재 API")
@RestController
@RequestMapping("/api/v1/menu-logs")
@RequiredArgsConstructor
public class MenuLogController {

    private final MenuLogService menuLogService;

    @Operation(summary = "메뉴 이동 기록", description = "탭 전환 이벤트를 적재합니다 (FE fire-and-forget)")
    @PostMapping
    public ResponseEntity<?> record(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody MenuLogRequest request) {
        menuLogService.record(userId, request);
        return ResponseUtil.success(null, "기록 완료");
    }
}
