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
public class User {

    private String userId;            // 사용자ID (UUID 또는 자동생성)
    private String userNm;            // 사용자명
    private String nickname;          // 닉네임
    private String email;             // 이메일
    private String password;          // 비밀번호 (일반 로그인용, BCrypt 해시)
    private String profileImgUrl;     // 프로필 이미지 URL
    private String useYn;             // 사용여부
    private String role;              // 역할 (USER/ADMIN)
    private LocalDateTime regDt;      // 등록일시
    private LocalDateTime updDt;      // 수정일시
    private LocalDateTime lastLoginDt; // 마지막 로그인일시
}