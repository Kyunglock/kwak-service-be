package com.investment.portal.application.service.login.standard;

import com.investment.portal.application.dto.login.standard.StandardLoginRequest;
import com.investment.portal.application.service.login.LoginResponse;
import com.investment.portal.domain.entity.user.User;
import com.investment.portal.domain.repository.user.UserMapper;
import kwak.common.config.security.JwtTokenProvider;
import kwak.common.exception.AuthenticationException;
import kwak.common.infrastructure.token.RedisTokenStore;
import kwak.common.infrastructure.token.UserSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandardAuthServiceImpl implements StandardAuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenStore redisTokenStore;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public LoginResponse login(StandardLoginRequest request) {
        // 이메일로 사용자 조회 (비밀번호 포함)
        User user = userMapper.findByEmailWithPassword(request.email())
                .orElseThrow(() -> new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 없는 계정 (소셜 전용)
        if (user.getPassword() == null) {
            throw new AuthenticationException("소셜 로그인으로 가입된 계정입니다.");
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 마지막 로그인 업데이트
        userMapper.updateLastLoginDt(user.getUserId());

        // JWT 액세스 토큰 발급 (1시간 유효)
        String sessionId = jwtTokenProvider.generateSessionId();
        String jwtToken = jwtTokenProvider.createToken(sessionId);

        // 리프레시 토큰 ID 생성 (7일 유효)
        String refreshTokenId = jwtTokenProvider.generateRefreshTokenId();

        // 세션 저장 (리프레시 토큰 유효 기간에 맞춰 7일)
        UserSession session = UserSession.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImgUrl(user.getProfileImgUrl())
                .build();
        redisTokenStore.saveSession(sessionId, session, jwtTokenProvider.getRefreshValidityInMilliseconds());

        // 리프레시 토큰 저장 (refreshTokenId → sessionId)
        redisTokenStore.saveRefreshToken(refreshTokenId, sessionId, jwtTokenProvider.getRefreshValidityInMilliseconds());

        return new LoginResponse(jwtToken, refreshTokenId, user.getUserId(), user.getEmail(), user.getNickname(), false);
    }
}
