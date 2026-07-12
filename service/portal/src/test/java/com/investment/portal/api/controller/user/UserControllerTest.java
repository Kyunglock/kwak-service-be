package com.investment.portal.api.controller.user;

import com.investment.portal.application.service.user.DuplicateNicknameException;
import com.investment.portal.application.service.user.NicknameService;
import com.investment.portal.domain.entity.user.User;
import com.investment.portal.domain.repository.user.UserMapper;
import kwak.common.application.dto.RokResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserMapper userMapper;
    @Mock NicknameService nicknameService;
    @InjectMocks UserController controller;

    private Authentication auth(String role) {
        return new UsernamePasswordAuthenticationToken(
                "u1", null, List.of(new SimpleGrantedAuthority(role)));
    }

    private Object dataOf(ResponseEntity<?> response) {
        return ((RokResponse<?>) response.getBody()).getData();
    }

    @Test
    void me_닉네임과_관리자여부를_반환한다() {
        when(userMapper.findByUserId("u1")).thenReturn(User.builder().userId("u1").nickname(null).build());

        ResponseEntity<?> response = controller.me("u1", auth("ROLE_ADMIN"));

        com.investment.portal.application.dto.user.MeResponse me =
                (com.investment.portal.application.dto.user.MeResponse) dataOf(response);
        assertThat(me.userId()).isEqualTo("u1");
        assertThat(me.nickname()).isNull();
        assertThat(me.isAdmin()).isTrue();
    }

    @Test
    void setNickname_중복이면_409를_반환한다() {
        when(nicknameService.setNickname("u1", "점유닉"))
                .thenThrow(new DuplicateNicknameException("이미 사용 중인 닉네임입니다."));

        ResponseEntity<?> response = controller.setNickname("u1", Map.of("nickname", "점유닉"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void setNickname_형식위반이면_400을_반환한다() {
        when(nicknameService.setNickname("u1", "x"))
                .thenThrow(new IllegalArgumentException("2~12자 한글/영문/숫자만 사용할 수 있습니다."));

        ResponseEntity<?> response = controller.setNickname("u1", Map.of("nickname", "x"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void setNickname_성공하면_닉네임을_반환한다() {
        when(nicknameService.setNickname("u1", "새닉네임")).thenReturn("새닉네임");

        ResponseEntity<?> response = controller.setNickname("u1", Map.of("nickname", "새닉네임"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, String> responseData = (Map<String, String>) dataOf(response);
        assertThat(responseData).containsEntry("nickname", "새닉네임");
    }
}
