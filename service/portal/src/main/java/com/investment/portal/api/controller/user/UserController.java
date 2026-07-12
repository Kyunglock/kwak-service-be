package com.investment.portal.api.controller.user;

import com.investment.portal.application.dto.user.MeResponse;
import com.investment.portal.application.service.user.DuplicateNicknameException;
import com.investment.portal.application.service.user.NicknameService;
import com.investment.portal.domain.entity.user.User;
import com.investment.portal.domain.repository.user.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "사용자", description = "로그인 사용자 정보/닉네임 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserMapper userMapper;
    private final NicknameService nicknameService;

    @Operation(summary = "내 정보", description = "userId, nickname(미설정 시 null), 관리자 여부를 반환합니다")
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal String userId, Authentication authentication) {
        User user = userMapper.findByUserId(userId);
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
        return ResponseUtil.success(new MeResponse(userId, user != null ? user.getNickname() : null, isAdmin));
    }

    @Operation(summary = "닉네임 중복 확인", description = "형식 위반도 available=false로 반환합니다")
    @GetMapping("/nickname/check")
    public ResponseEntity<?> checkNickname(@RequestParam String nickname) {
        return ResponseUtil.success(nicknameService.check(nickname));
    }

    @Operation(summary = "닉네임 설정", description = "형식 위반 400, 중복 409")
    @PutMapping("/me/nickname")
    public ResponseEntity<?> setNickname(@AuthenticationPrincipal String userId,
                                         @RequestBody Map<String, String> body) {
        try {
            String saved = nicknameService.setNickname(userId, body.get("nickname"));
            return ResponseUtil.success(Map.of("nickname", saved));
        } catch (IllegalArgumentException e) {
            return ResponseUtil.error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (DuplicateNicknameException e) {
            return ResponseUtil.error(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
