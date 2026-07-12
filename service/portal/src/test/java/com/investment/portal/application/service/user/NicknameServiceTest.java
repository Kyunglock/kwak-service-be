package com.investment.portal.application.service.user;

import com.investment.portal.application.dto.user.NicknameCheckResponse;
import com.investment.portal.domain.entity.user.User;
import com.investment.portal.domain.repository.user.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NicknameServiceTest {

    @Mock UserMapper userMapper;
    @InjectMocks NicknameServiceImpl service;

    // ---- check: 형식 ----

    @Test
    void check_한글영문숫자_2자와_12자는_통과한다() {
        when(userMapper.findByNickname(anyString())).thenReturn(Optional.empty());

        assertThat(service.check("ab").available()).isTrue();
        assertThat(service.check("가나다라마바사아자차카타").available()).isTrue(); // 12자
    }

    @Test
    void check_1자_13자_특수문자_언더스코어_공백은_거부한다() {
        for (String bad : new String[]{"a", "가나다라마바사아자차카타파", "nick_name", "nick name", "닉네임!"}) {
            NicknameCheckResponse res = service.check(bad);
            assertThat(res.available()).as(bad).isFalse();
            assertThat(res.reason()).isEqualTo("2~12자 한글/영문/숫자만 사용할 수 있습니다.");
        }
        verifyNoInteractions(userMapper); // 형식 위반은 DB 조회 없이 거부
    }

    // ---- check: 중복 ----

    @Test
    void check_이미_점유된_닉네임은_거부한다() {
        when(userMapper.findByNickname("점유닉")).thenReturn(Optional.of(User.builder().userId("u2").build()));

        NicknameCheckResponse res = service.check("점유닉");

        assertThat(res.available()).isFalse();
        assertThat(res.reason()).isEqualTo("이미 사용 중인 닉네임입니다.");
    }

    // ---- setNickname ----

    @Test
    void set_정상이면_updateNickname을_호출하고_닉네임을_반환한다() {
        when(userMapper.findByNickname("새닉네임")).thenReturn(Optional.empty());

        String saved = service.setNickname("u1", "새닉네임");

        assertThat(saved).isEqualTo("새닉네임");
        verify(userMapper).updateNickname("u1", "새닉네임");
    }

    @Test
    void set_형식위반이면_IllegalArgumentException() {
        assertThatThrownBy(() -> service.setNickname("u1", "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("2~12자 한글/영문/숫자만 사용할 수 있습니다.");
        verifyNoInteractions(userMapper);
    }

    @Test
    void set_사전조회_중복이면_DuplicateNicknameException() {
        when(userMapper.findByNickname("점유닉")).thenReturn(Optional.of(User.builder().userId("u2").build()));

        assertThatThrownBy(() -> service.setNickname("u1", "점유닉"))
                .isInstanceOf(DuplicateNicknameException.class);
        verify(userMapper, never()).updateNickname(anyString(), anyString());
    }

    @Test
    void set_동시선점으로_DuplicateKeyException이_나면_DuplicateNicknameException으로_변환한다() {
        when(userMapper.findByNickname("경합닉")).thenReturn(Optional.empty());
        when(userMapper.updateNickname("u1", "경합닉")).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> service.setNickname("u1", "경합닉"))
                .isInstanceOf(DuplicateNicknameException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");
    }
}
