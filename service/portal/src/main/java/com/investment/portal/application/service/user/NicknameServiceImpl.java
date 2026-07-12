package com.investment.portal.application.service.user;

import com.investment.portal.application.dto.user.NicknameCheckResponse;
import com.investment.portal.domain.repository.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NicknameServiceImpl implements NicknameService {

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]{2,12}$");
    private static final String INVALID_FORMAT = "2~12자 한글/영문/숫자만 사용할 수 있습니다.";
    private static final String DUPLICATED = "이미 사용 중인 닉네임입니다.";

    private final UserMapper userMapper;

    @Override
    public NicknameCheckResponse check(String nickname) {
        if (nickname == null || !NICKNAME_PATTERN.matcher(nickname).matches()) {
            return NicknameCheckResponse.rejected(INVALID_FORMAT);
        }
        // 탈퇴 유저 점유분도 유니크 인덱스에 걸리므로 use_yn 무관 조회
        if (userMapper.findByNickname(nickname).isPresent()) {
            return NicknameCheckResponse.rejected(DUPLICATED);
        }
        return NicknameCheckResponse.ok();
    }

    @Override
    public String setNickname(String userId, String nickname) {
        if (nickname == null || !NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new IllegalArgumentException(INVALID_FORMAT);
        }
        if (userMapper.findByNickname(nickname).isPresent()) {
            throw new DuplicateNicknameException(DUPLICATED);
        }
        try {
            userMapper.updateNickname(userId, nickname);
        } catch (DuplicateKeyException e) {
            // 사전조회~UPDATE 사이 타인이 선점(유니크 제약 경합)
            throw new DuplicateNicknameException(DUPLICATED);
        }
        return nickname;
    }
}
