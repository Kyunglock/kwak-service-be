package com.investment.portal.application.service.login.guest;

import com.investment.portal.application.service.login.LoginResponse;
import com.investment.portal.domain.entity.user.User;
import com.investment.portal.domain.repository.user.UserMapper;
import kwak.common.config.security.JwtTokenProvider;
import kwak.common.infrastructure.token.RedisTokenStore;
import kwak.common.infrastructure.token.UserSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestAuthServiceImpl implements GuestAuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenStore redisTokenStore;

    @Override
    @Transactional
    public LoginResponse guestLogin() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String userId = UUID.randomUUID().toString();
        String email = "guest_" + uuid + "@guest.local";
        String nickname = "손님_" + uuid.substring(0, 8);

        log.info("손님 로그인 - 생성된 userId: {}, nickname: {}", userId, nickname);

        User guestUser = User.builder()
                .userId(userId)
                .userNm(nickname)
                .email(email)
                .nickname(nickname)
                .useYn("Y")
                .regDt(LocalDateTime.now())
                .updDt(LocalDateTime.now())
                .lastLoginDt(LocalDateTime.now())
                .build();

        userMapper.insert(guestUser);

        String sessionId = jwtTokenProvider.generateSessionId();
        String jwtToken = jwtTokenProvider.createToken(sessionId);
        String refreshTokenId = jwtTokenProvider.generateRefreshTokenId();

        UserSession session = UserSession.builder()
                .userId(userId)
                .nickname(nickname)
                .email(email)
                .build();
        redisTokenStore.saveSession(sessionId, session, jwtTokenProvider.getRefreshValidityInMilliseconds());
        redisTokenStore.saveRefreshToken(refreshTokenId, sessionId, jwtTokenProvider.getRefreshValidityInMilliseconds());

        return new LoginResponse(jwtToken, refreshTokenId, userId, email, nickname, true);
    }
}
