package com.investment.portal.application.service.login.kakao;

import com.investment.portal.application.dto.login.kakao.KakaoTokenResponse;
import com.investment.portal.application.dto.login.kakao.KakaoUserInfo;
import com.investment.portal.application.service.login.LoginResponse;
import kwak.common.config.security.JwtTokenProvider;
import com.investment.portal.domain.entity.user.User;
import com.investment.portal.domain.entity.user.UserSocial;
import com.investment.portal.domain.enums.SocialProvider;
import com.investment.portal.domain.repository.user.UserMapper;
import com.investment.portal.domain.repository.user.UserSocialMapper;
import kwak.common.infrastructure.token.RedisTokenStore;
import kwak.common.infrastructure.token.UserSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

import kwak.common.exception.AuthenticationException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthServiceImpl implements KakaoAuthService {

    private final UserMapper userMapper;
    private final UserSocialMapper userSocialMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenStore redisTokenStore;
    private final WebClient webClient;

    @Value("${kakao.registration.client-id}")
    private String kakaoClientId;

    @Value("${kakao.registration.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.registration.client-secret}")
    private String kakaoClientSecret;
    

    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    @Override
    public String getKakaoAuthUrl() {
        return UriComponentsBuilder.fromUriString(KAKAO_AUTH_URL)
                .queryParam("client_id", kakaoClientId)
                .queryParam("redirect_uri", kakaoRedirectUri)
                .queryParam("response_type", "code")
                .build()
                .toUriString();
    }

    @Override
    @Transactional
    public LoginResponse loginWithKakao(String code) {
        // 1. 인가 코드로 액세스 토큰 받기
        KakaoTokenResponse tokenResponse = getKakaoAccessToken(code);

        // 카카오 Access Token은 사용 후 버림 (DB 저장 안 함)
        String kakaoAccessToken = tokenResponse.accessToken();
        
        // 2. 카카오 사용자 정보 조회
        KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(kakaoAccessToken);
        
        // 3. 기존 사용자 확인 또는 신규 사용자 생성
        User user = findOrCreateUser(kakaoUserInfo);
        
        // 4. sessionId 생성 + JWT 액세스 토큰 생성 (1시간 유효)
        String sessionId = jwtTokenProvider.generateSessionId();
        String jwtToken = jwtTokenProvider.createToken(sessionId);

        // 5. 리프레시 토큰 ID 생성 (7일 유효)
        String refreshTokenId = jwtTokenProvider.generateRefreshTokenId();

        // 6. Redis에 유저 세션 저장 (리프레시 토큰 유효 기간에 맞춰 7일)
        UserSession session = UserSession.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImgUrl(user.getProfileImgUrl())
                .role(user.getRole())
                .build();
        redisTokenStore.saveSession(sessionId, session, jwtTokenProvider.getRefreshValidityInMilliseconds());

        // 7. Redis에 리프레시 토큰 저장 (refreshTokenId → sessionId)
        redisTokenStore.saveRefreshToken(refreshTokenId, sessionId, jwtTokenProvider.getRefreshValidityInMilliseconds());

        return new LoginResponse(
                jwtToken,
                refreshTokenId,
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                true
        );
    }

    @Override
    @Transactional
    public void unlinkKakao(String userId) {
        // 1. 사용자의 카카오 소셜 정보 조회
        UserSocial userSocial = userSocialMapper.findByUserIdAndProvider(userId, SocialProvider.KAKAO.name())
                .orElseThrow(() -> new AuthenticationException("연결된 카카오 계정이 없습니다."));

        // 2. 카카오 연결 해제 API 호출 (Admin Key 사용)
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("target_id_type", "user_id");
            params.add("target_id", userSocial.getProviderUserId());

            webClient.post()
                    .uri(KAKAO_UNLINK_URL)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("카카오 연결 해제 완료 - 사용자 ID: {}, Provider ID: {}", 
                    userId, userSocial.getProviderUserId());
        } catch (Exception e) {
            log.error("카카오 연결 해제 실패", e);
            throw new AuthenticationException("카카오 연결 해제에 실패했습니다.");
        }

        // 3. DB에서 소셜 정보 삭제 (논리 삭제)
        userSocialMapper.delete(userSocial.getSocialId());
    }

    private KakaoTokenResponse getKakaoAccessToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        try {
            return webClient.post()
                    .uri(KAKAO_TOKEN_URL)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(KakaoTokenResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("카카오 액세스 토큰 발급 실패", e);
            throw new AuthenticationException("카카오 로그인에 실패했습니다.");
        }
    }

    private KakaoUserInfo getKakaoUserInfo(String accessToken) {
        try {
            return webClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfo.class)
                    .block();
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new AuthenticationException("카카오 사용자 정보를 가져올 수 없습니다.");
        }
    }

    
    private User findOrCreateUser(KakaoUserInfo kakaoUserInfo) {
        String providerUserId = String.valueOf(kakaoUserInfo.id());
        
        // 1. 소셜 계정으로 기존 사용자 확인
        Optional<UserSocial> existingSocial = userSocialMapper.findByProviderAndProviderUserId(
                SocialProvider.KAKAO.name(), 
                providerUserId
        );

        if (existingSocial.isPresent()) {
            // 기존 사용자: 마지막 로그인만 업데이트
            UserSocial userSocial = existingSocial.get();
            
            // ✅ Access Token 없이 업데이트
            UserSocial updatedSocial = UserSocial.builder()
                    .socialId(userSocial.getSocialId())
                    .userId(userSocial.getUserId())
                    .provider(userSocial.getProvider())
                    .providerUserId(userSocial.getProviderUserId())
                    .connectedDt(userSocial.getConnectedDt())
                    .lastLoginDt(LocalDateTime.now())
                    .tokenExpiredDt(userSocial.getTokenExpiredDt())
                    .useYn(userSocial.getUseYn())
                    // ✅ accessToken 필드 제거
                    .build();
            
            userSocialMapper.update(updatedSocial);
            
            // 사용자 마지막 로그인 업데이트
            userMapper.updateLastLoginDt(userSocial.getUserId());
            
            return userMapper.findByUserId(userSocial.getUserId());
        }

        // 2. 신규 사용자 생성
        String userId = java.util.UUID.randomUUID().toString();
        
        String kakaoNickname = (kakaoUserInfo.properties() != null) ? kakaoUserInfo.properties().nickname() : "카카오사용자";
        String kakaoEmail = (kakaoUserInfo.kakaoAccount() != null) ? kakaoUserInfo.kakaoAccount().email() : null;
        String profileImgUrl = (kakaoUserInfo.kakaoAccount() != null && kakaoUserInfo.kakaoAccount().profile() != null)
                ? kakaoUserInfo.kakaoAccount().profile().profileImageUrl() : null;

        User newUser = User.builder()
                .userId(userId)
                .userNm(kakaoNickname)
                .email(kakaoEmail)
                .nickname(kakaoNickname)
                .profileImgUrl(profileImgUrl)
                .useYn("Y")
                .regDt(LocalDateTime.now())
                .updDt(LocalDateTime.now())
                .lastLoginDt(LocalDateTime.now())
                .build();
        
        userMapper.insert(newUser);
        
        // 3. 소셜 정보 저장 (Access Token 제외)
        UserSocial userSocial = UserSocial.builder()
                .userId(newUser.getUserId())
                .provider(SocialProvider.KAKAO.name())
                .providerUserId(providerUserId)
                .connectedDt(LocalDateTime.now())
                .lastLoginDt(LocalDateTime.now())
                .useYn("Y")
                .build();
        
        userSocialMapper.insert(userSocial);
        
        return newUser;
    }
}