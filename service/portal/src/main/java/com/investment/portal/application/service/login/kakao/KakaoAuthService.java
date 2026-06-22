package com.investment.portal.application.service.login.kakao;

import com.investment.portal.application.service.login.LoginResponse;

public interface KakaoAuthService {
    
    /**
     * 카카오 OAuth 인증 URL 생성
     * @return 카카오 로그인 페이지 URL
     */
    String getKakaoAuthUrl();
    
    /**
     * 카카오 인가 코드로 로그인
     * @param code 카카오로부터 받은 인가 코드
     * @return 로그인 응답 (JWT 토큰 포함)
     */
    LoginResponse loginWithKakao(String code);
    
    /**
     * 카카오 연결 해제
     * @param userId 사용자 ID
     */
    void unlinkKakao(String userId);
}