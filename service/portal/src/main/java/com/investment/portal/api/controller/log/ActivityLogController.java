package com.investment.portal.api.controller.log;

import com.investment.portal.application.service.log.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "활동 로그", description = "감사/활동 로그 조회 API")
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ActivityLogService activityLogService;

    @Operation(summary = "내 활동 내역", description = "로그인 사용자 본인의 활동 로그를 조회합니다")
    @GetMapping("/me")
    public ResponseEntity<?> myLogs(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseUtil.success(activityLogService.getMyLogs(userId, page, size));
    }

    @Operation(summary = "관리자 여부 확인", description = "현재 사용자가 관리자(ROLE_ADMIN)인지 반환합니다")
    @GetMapping("/admin/access")
    public ResponseEntity<?> adminAccess(Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
        return ResponseUtil.success(Map.of("isAdmin", isAdmin));
    }

    @Operation(summary = "전체 활동 로그(관리자)", description = "관리자만 전체 사용자 활동을 조회합니다")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> allLogs(
            @RequestParam(required = false) String targetUserId,
            @RequestParam(required = false) String actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseUtil.success(activityLogService.search(targetUserId, actionType, page, size));
    }
}
