package com.investment.portal.application.service.user;

import com.investment.portal.application.dto.user.NicknameCheckResponse;

public interface NicknameService {

    NicknameCheckResponse check(String nickname);

    /**
     * 닉네임 설정. 성공 시 저장된 닉네임 반환.
     * @throws IllegalArgumentException 형식 위반
     * @throws DuplicateNicknameException 이미 점유된 닉네임(사전조회 또는 유니크 제약 경합)
     */
    String setNickname(String userId, String nickname);
}
