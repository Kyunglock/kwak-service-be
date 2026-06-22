package com.investment.portal.domain.entity.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSocial {
    
    private Long socialId;            // 소셜연동ID
    private String userId;            // 사용자ID
    private String provider;          // 소셜 제공자 (KAKAO, NAVER, GOOGLE)
    private String providerUserId;    // 소셜 제공자의 사용자 ID
    private LocalDateTime connectedDt; // 연동일시
    private LocalDateTime lastLoginDt; // 마지막 로그인일시
    private LocalDateTime tokenExpiredDt; // 토큰 만료일시
    private String useYn;             // 사용여부
}